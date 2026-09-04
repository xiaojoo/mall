#!/usr/bin/env bash
# ============================================================================
# install-infra.sh —— 微服务运行依赖环境一键安装（Docker）
#
# 覆盖：Elasticsearch(7.17 + IK分词器)、Kibana、RabbitMQ、Zipkin、MySQL、Nacos、Sentinel
# 并自动执行 ES 商品索引重建脚本（mall-search/src/main/resources/create-product-index.sh）
#
# 说明：
#  - Redis / MinIO / Nginx 已单独脚本（install-infra-basic.sh），本脚本不重复。
#  - 主机地址默认自动探测本机 IP（SERVER_IP），仅探测失败才回退 localhost；可用环境变量覆盖。
#  - 容器间/业务服务访问基础设施请用本机 IP（容器内 localhost 指容器自身）。
#  - 容器均加了 --restart unless-stopped，宿主机重启后自动拉起。
#  - 需要宿主机能访问外网（拉镜像 + 下载 IK 插件）。
#
# 用法：
#   bash install-infra.sh              # 默认自动探测本机 IP（失败才用 localhost）
#   ES_HOST=192.0.2.1 bash install-infra.sh   # 指定 ES 所在地址
# ============================================================================

set -e

# ---------------- 可配置项（默认值，可用环境变量覆盖） ----------------
# 自动探测本服务器地址（优先默认路由源 IP；取不到再用 hostname -I 首个非回环，最后兜底 localhost）
detect_ip() {
  local ip=""
  if command -v ip >/dev/null 2>&1; then
    ip=$(ip route get 1 2>/dev/null | awk '{for(i=1;i<=NF;i++) if($i=="src"){print $(i+1); exit}}')
  fi
  if [ -z "$ip" ] && command -v hostname >/dev/null 2>&1; then
    ip=$(hostname -I 2>/dev/null | awk '{for(i=1;i<=NF;i++) if($i!="127.0.0.1"){print $i; break}}')
  fi
  [ -z "$ip" ] && ip="127.0.0.1"
  echo "$ip"
}
SERVER_IP="${SERVER_IP:-$(detect_ip)}"   # 本机对外地址；容器/业务服务间用它，勿用 localhost

# 数据/配置统一存放根目录（默认 /data/mall；可用 DATA_ROOT=xxx 覆盖；请在仓库根目录运行本脚本）
DATA_ROOT="${DATA_ROOT:-/data/mall}"

ES_VERSION="${ES_VERSION:-7.17.15}"
ES_HOST="${ES_HOST:-$SERVER_IP}"          # 服务间访问 ES 的地址（业务服务配置里 spring.elasticsearch.uris）
ES_DATA_DIR="${ES_DATA_DIR:-$DATA_ROOT/es}"

KIBANA_HOST="${KIBANA_HOST:-$SERVER_IP}"  # Kibana 连接 ES 用
KIBANA_PORT="${KIBANA_PORT:-5601}"

RABBIT_PORT="${RABBIT_PORT:-5672}"
RABBIT_MGMT_PORT="${RABBIT_MGMT_PORT:-15672}"

ZIPKIN_PORT="${ZIPKIN_PORT:-9411}"

MYSQL_IMAGE="${MYSQL_IMAGE:-mysql:8.0}"  # 项目面向 MySQL 8；本项目代码注释给的是 5.7，可按需改 mysql:5.7
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-admin123}"   # 需与各服务 spring.datasource.password 一致
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATA_DIR="${MYSQL_DATA_DIR:-$DATA_ROOT/mysql/data}"   # 数据目录（与 conf 分开，避免 MySQL 判"目录非空"）
MYSQL_CONF_DIR="${MYSQL_CONF_DIR:-$DATA_ROOT/mysql/conf}"   # 配置文件目录

NACOS_VERSION="${NACOS_VERSION:-v3.1.1}"
NACOS_PORT="${NACOS_PORT:-8848}"
NACOS_GRPC_PORT="${NACOS_GRPC_PORT:-9848}"
# Nacos 3.x 默认开启鉴权，必须设置 Base64 的 token（解密后 >=32 字节）；生产请改成自己的强随机值
NACOS_AUTH_TOKEN="${NACOS_AUTH_TOKEN:-$(printf '%s' 'mall-demo-nacos-auth-token-change-me-32chars!' | base64)}"
NACOS_AUTH_IDENTITY_KEY="${NACOS_AUTH_IDENTITY_KEY:-serverIdentity}"
NACOS_AUTH_IDENTITY_VALUE="${NACOS_AUTH_IDENTITY_VALUE:-$NACOS_AUTH_TOKEN}"

SENTINEL_VERSION="${SENTINEL_VERSION:-1.8.9}"
SENTINEL_PORT="${SENTINEL_PORT:-8333}"                    # 需与 spring.cloud.sentinel.transport.dashboard 一致(项目默认 localhost:8333)

# ---------------- 工具函数 ----------------
docker_run() { echo "==> docker run $*"; docker run "$@"; }
wait_for() { # wait_for <port> <name>
  local port="$1" name="$2" i=0
  echo "==> 等待 $name 就绪( $port )..."
  until (echo > /dev/tcp/127.0.0.1/"$port") >/dev/null 2>&1; do
    i=$((i+1)); [ "$i" -gt 60 ] && { echo "  等待超时：$name"; return 1; }
    sleep 2
  done
  echo "  $name 就绪。"
}

echo "============================================================"
echo " 安装微服务基础设施 (ES/Kibana/RabbitMQ/Zipkin/MySQL/Nacos/Sentinel)"
echo "============================================================"

# ---------------------------------------------------------------------
# 1. Elasticsearch 7.17（单节点）+ IK 分析插件
# ---------------------------------------------------------------------
echo "-------- [1/7] Elasticsearch ${ES_VERSION} + IK --------"
mkdir -p "$ES_DATA_DIR"
docker rm -f elasticsearch >/dev/null 2>&1 || true
docker_run -d --name elasticsearch \
  -p 9200:9200 -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "ES_JAVA_OPTS=-Xms1g -Xmx1g" \
  -e "cluster.name=elasticsearch" \
  -v "$ES_DATA_DIR":/usr/share/elasticsearch/data \
  --restart unless-stopped \
  "elasticsearch:${ES_VERSION}"

# 安装 IK 分析插件：优先用本地 zip 离线装（防 GitHub 连不上），找不到再尝试下载
echo "==> 安装 IK 分词器插件..."
IK_ZIP=""
for d in "$PWD" "$DATA_ROOT" "/data" "/tmp"; do
  found=$(ls "$d"/elasticsearch-analysis-ik-*.zip 2>/dev/null | head -1)
  if [ -n "$found" ]; then IK_ZIP="$found"; break; fi
done
if [ -n "$IK_ZIP" ]; then
  echo "  使用本地 IK 包: $IK_ZIP"
  docker cp "$IK_ZIP" elasticsearch:/tmp/ik.zip
  docker exec elasticsearch bin/elasticsearch-plugin install --batch file:///tmp/ik.zip \
    || echo "  ⚠️ 本地 IK 安装失败，请检查 zip 是否完整。"
  docker restart elasticsearch || true
else
  echo "  未找到本地 IK zip（如 /data/elasticsearch-analysis-ik-${ES_VERSION}.zip），尝试从 GitHub 下载..."
  docker exec elasticsearch bin/elasticsearch-plugin install \
    "https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v${ES_VERSION}/elasticsearch-analysis-ik-${ES_VERSION}.zip" \
    || echo "  ⚠️ IK 下载失败（无外网），请把 IK zip 放到 /data 下再重跑本脚本。"
  docker restart elasticsearch || true
fi
wait_for 9200 "Elasticsearch"

# ---------------------------------------------------------------------
# 2. Kibana
# ---------------------------------------------------------------------
echo "-------- [2/7] Kibana --------"
docker rm -f kibana >/dev/null 2>&1 || true
docker_run -d --name kibana \
  -e "ELASTICSEARCH_HOSTS=http://${KIBANA_HOST}:9200" \
  -p "${KIBANA_PORT}:5601" \
  --restart unless-stopped \
  "kibana:${ES_VERSION}"
wait_for "$KIBANA_PORT" "Kibana"

# ---------------------------------------------------------------------
# 3. RabbitMQ (management)
# ---------------------------------------------------------------------
echo "-------- [3/7] RabbitMQ --------"
docker rm -f rabbitmq >/dev/null 2>&1 || true
docker_run -d --name rabbitmq \
  -p 5671:5671 -p 5672:5672 -p 4369:4369 -p 25672:25672 \
  -p 15671:15671 -p 15672:15672 \
  --restart unless-stopped \
  rabbitmq:management
wait_for "$RABBIT_PORT" "RabbitMQ"

# ---------------------------------------------------------------------
# 4. Zipkin
# ---------------------------------------------------------------------
echo "-------- [4/7] Zipkin --------"
docker rm -f zipkin >/dev/null 2>&1 || true
docker_run -d --name zipkin \
  -e "JAVA_OPTS=--enable-native-access=ALL-UNNAMED" \
  -p "${ZIPKIN_PORT}:9411" \
  --restart unless-stopped \
  openzipkin/zipkin
wait_for "$ZIPKIN_PORT" "Zipkin"

# ---------------------------------------------------------------------
# 5. MySQL（带 my.cnf 配置）
# ---------------------------------------------------------------------
echo "-------- [5/7] MySQL (${MYSQL_IMAGE}) --------"
mkdir -p "$MYSQL_DATA_DIR"
mkdir -p "$MYSQL_CONF_DIR"
# 生成 my.cnf
cat > "$MYSQL_CONF_DIR/my.cnf" <<'EOF'
[mysqld]
default-storage-engine=INNODB
character-set-server=utf8mb4
pid-file        = /var/run/mysqld/mysqld.pid
socket          = /var/run/mysqld/mysqld.sock
datadir         = /var/lib/mysql
sql_mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION
max_connections=200
server-id=3306
log-bin=/var/lib/mysql/mysql-bin
max_binlog_size=100M
binlog_expire_logs_seconds=604800
[mysql]
default-character-set=utf8mb4
[client]
default-character-set=utf8mb4
EOF
docker rm -f mysql >/dev/null 2>&1 || true
docker_run -d --name mysql \
  -p "${MYSQL_PORT}:3306" \
  -v "$MYSQL_DATA_DIR":/var/lib/mysql \
  -v "$MYSQL_CONF_DIR/my.cnf":/etc/mysql/conf.d/my.cnf \
  -e "MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}" \
  -e "TZ=Asia/Shanghai" \
  --restart unless-stopped \
  "$MYSQL_IMAGE" \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
wait_for "$MYSQL_PORT" "MySQL"

# ---------------------------------------------------------------------
# 6. Nacos（注册中心 + 配置中心，单机 standalone）
# ---------------------------------------------------------------------
echo "-------- [6/7] Nacos ${NACOS_VERSION} --------"
docker rm -f nacos >/dev/null 2>&1 || true
docker_run -d --name nacos \
  -e "MODE=standalone" \
  -e "NACOS_AUTH_ENABLE=true" \
  -e "NACOS_AUTH_TOKEN=${NACOS_AUTH_TOKEN}" \
  -e "NACOS_AUTH_IDENTITY_KEY=${NACOS_AUTH_IDENTITY_KEY}" \
  -e "NACOS_AUTH_IDENTITY_VALUE=${NACOS_AUTH_IDENTITY_VALUE}" \
  -p "${NACOS_PORT}:8848" -p "${NACOS_GRPC_PORT}:9848" -p 9849:9849 -p 8080:8080 \
  --restart unless-stopped \
  "nacos/nacos-server:${NACOS_VERSION}"
wait_for "$NACOS_PORT" "Nacos"

# ---------------------------------------------------------------------
# 7. Sentinel 控制台
# ---------------------------------------------------------------------
echo "-------- [7/7] Sentinel 控制台 --------"
docker rm -f sentinel-dashboard >/dev/null 2>&1 || true
docker_run -d --name sentinel-dashboard \
  -p "${SENTINEL_PORT}:8858" \
  --restart unless-stopped \
  "bladex/sentinel-dashboard:${SENTINEL_VERSION}"
wait_for "$SENTINEL_PORT" "Sentinel"

# ---------------------------------------------------------------------
# 8. 将业务配置里的占位符 ${SERVER_IP:localhost} 批量替换为实际探测 IP
#    （服务/Docker 容器访问基础设施需用宿主 IP；容器内 localhost 指容器自身）
# ---------------------------------------------------------------------
echo "-------- 批量替换业务配置地址 -> ${SERVER_IP} --------"
find . -type d \( -name target -o -name .git \) -prune -o \
  \( -name '*.yml' -o -name '*.yaml' -o -name '*.properties' \) -print 2>/dev/null \
  | while IFS= read -r f; do
      case "$f" in
        ./install-infra.sh|./install-infra-basic.sh) continue;;
      esac
      if grep -q '\${SERVER_IP:localhost}' "$f" 2>/dev/null; then
        sed -i "s|\${SERVER_IP:localhost}|${SERVER_IP}|g; s|localhost|${SERVER_IP}|g" "$f"
        echo "  已更新: $f"
      fi
    done

# ---------------------------------------------------------------------
# 9. 执行 ES 商品索引重建脚本（必须，价格排序依赖 skuPrice 为 long 类型）
# ---------------------------------------------------------------------
echo "-------- 执行 ES 索引重建脚本 --------"
if [ -f "mall-search/src/main/resources/create-product-index.sh" ]; then
  bash "mall-search/src/main/resources/create-product-index.sh" "${ES_HOST}:9200" \
    || echo "  ⚠️ 索引脚本执行失败，请确认 ES 已就绪且 IK 插件已装。"
else
  echo "  ⚠️ 未找到 create-product-index.sh，跳过。"
fi

echo
echo "============================================================"
echo " 基础设施安装完成 ✔   本机服务地址(业务服务/容器间请用它): ${SERVER_IP}"
echo " 后续请手动完成："
echo "  1) 导入数据库: mysql -h${SERVER_IP} -uroot -p${MYSQL_ROOT_PASSWORD} -> source db/*.sql"
echo "  2) 载入 Nacos 配置: nacos-config/*.yaml 导入对应 data-id（Nacos 地址用 ${SERVER_IP}:${NACOS_PORT}）"
echo "  3) 业务配置地址已由脚本自动替换为 ${SERVER_IP}；若未替换，请把配置文件里的 localhost 改成 ${SERVER_IP}"
echo "  4) 将各服务 spring.datasource.password / spring.data.redis.password 设为与脚本一致(MYSQL=${MYSQL_ROOT_PASSWORD})"
echo "============================================================"
