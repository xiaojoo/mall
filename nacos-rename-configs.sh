#!/usr/bin/env bash
# ============================================================================
# nacos-rename-configs.sh —— Nacos 3.x OpenAPI 批量给配置 dataId 补 .yaml 后缀
#
# 背景：本仓库各模块 spring.config.import 已改为 "nacos:mall-xxx.yaml"，
#       而 Nacos 里现存 dataId 是不带后缀的 mall-xxx（格式 YAML），导致
#       应用启动报 Config data resource ... does not exist。
#       本脚本把旧 dataId mall-xxx 复制为新 dataId mall-xxx.yaml 后删除旧条目
#       （内容原样保留，仅改名，不影响线上数据）。
#
# 原理（Nacos 3.x 官方运维 API，端口 8848）：
#   GET/POST/DELETE /nacos/v3/admin/cs/config     —— 查/发/删配置
#   GET /nacos/v3/admin/cs/config/list            —— 配置列表
#   POST /nacos/v1/auth/login                     —— 登录取 accessToken
#   之后所有请求携带 ?accessToken=xxx
#
# 依赖：curl + jq（Debian/Ubuntu: apt install -y jq；CentOS: yum install -y jq）
#
# 用法：
#   bash nacos-rename-configs.sh            # 只读：登录 + 列出将被改名的配置（dry-run）
#   APPLY=1 bash nacos-rename-configs.sh    # 真正执行改名（复制新 dataId + 删除旧）
#
# 可配置项（环境变量覆盖）：
#   NACOS_HOST       Nacos 地址，默认自动探测本机 IP（失败回退 127.0.0.1）
#   NACOS_API_PORT   Nacos API 端口，默认 8848（控制台 UI 8080 是另一个端口）
#   NACOS_USERNAME   默认 nacos
#   NACOS_PASSWORD   默认 nacos
#   NACOS_GROUP      默认 DEFAULT_GROUP
#   NACOS_NAMESPACE  默认 public
#   ONLY_PREFIX      只处理该前缀的 dataId，默认 mall-；置空则处理全部无后缀配置
# ============================================================================

set -euo pipefail

# ---------------- 本机 IP 探测（同 install-infra.sh） ----------------
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

# ---------------- 可配置项 ----------------
NACOS_HOST="${NACOS_HOST:-$(detect_ip)}"
NACOS_API_PORT="${NACOS_API_PORT:-8848}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-public}"
ONLY_PREFIX="${ONLY_PREFIX:-mall-}"
APPLY="${APPLY:-}"

BASE="http://${NACOS_HOST}:${NACOS_API_PORT}/nacos"

command -v curl >/dev/null 2>&1 || { echo "缺少 curl"; exit 1; }
command -v jq   >/dev/null 2>&1 || { echo "缺少 jq，请先安装（Debian/Ubuntu: apt install -y jq）"; exit 1; }

echo "==> Nacos: ${BASE}  group=${NACOS_GROUP}  namespace=${NACOS_NAMESPACE}  prefix=${ONLY_PREFIX:-全部}"

# ---------------- 登录拿 accessToken ----------------
login() {
  local resp
  resp=$(curl -s -X POST "${BASE}/v1/auth/login" \
    --data-urlencode "username=${NACOS_USERNAME}" \
    --data-urlencode "password=${NACOS_PASSWORD}") || return 1
  echo "$resp" | jq -r '.accessToken // empty' 2>/dev/null
}
TOKEN="$(login)"
if [ -z "$TOKEN" ]; then
  # 部分 3.x 版本可能只支持 v2 登录，兜底一次
  TOKEN=$(curl -s -X POST "${BASE}/v2/auth/login" \
    --data-urlencode "username=${NACOS_USERNAME}" \
    --data-urlencode "password=${NACOS_PASSWORD}" | jq -r '.accessToken // empty' 2>/dev/null || true)
fi
if [ -z "$TOKEN" ]; then
  echo "!! 登录失败：请检查 NACOS_HOST/NACOS_API_PORT/NACOS_USERNAME/NACOS_PASSWORD"
  exit 1
fi
echo "==> 登录成功 (accessToken 已获取)"

AUTH="accessToken=${TOKEN}"

# ---------------- 收集待处理 dataId ----------------
# 规则：目标分组 + 前缀匹配(可空=全部) + 不含 "."（即没有后缀的配置才需要改名）
JQ_FILTER='.data.pageItems[] | select(.groupName == $g) | select(.dataId | contains(".") | not) | .dataId'
if [ -n "$ONLY_PREFIX" ]; then
  JQ_FILTER='.data.pageItems[] | select(.groupName == $g) | select(.dataId | startswith($p)) | select(.dataId | contains(".") | not) | .dataId'
fi

mapfile -t DATAIDS < <(
  curl -s -G "${BASE}/v3/admin/cs/config/list" \
    --data-urlencode "pageNo=1" \
    --data-urlencode "pageSize=1000" \
    --data-urlencode "namespaceId=${NACOS_NAMESPACE}" \
    --data-urlencode "${AUTH}" \
  | jq -r --arg g "${NACOS_GROUP}" --arg p "${ONLY_PREFIX}" "${JQ_FILTER}"
)

if [ "${#DATAIDS[@]}" -eq 0 ]; then
  echo "==> 没有需要改名的配置（可能已全部带 .yaml 后缀，或命名空间/分组不对）"
  exit 0
fi

echo "==> 待改名 ${#DATAIDS[@]} 条："
for d in "${DATAIDS[@]}"; do
  echo "    ${d}  ->  ${d}.yaml"
done

[ -z "$APPLY" ] && { echo; echo "==> dry-run 结束。确认无误后执行： APPLY=1 bash $0"; exit 0; }

echo
echo "==> 开始执行改名（复制内容到新 dataId 后删除旧 dataId）..."

ok=0; skip=0; fail=0
TMPDIR_CUSTOM="$(mktemp -d)"
trap 'rm -rf "${TMPDIR_CUSTOM}"' EXIT

for d in "${DATAIDS[@]}"; do
  new="${d}.yaml"
  # 1) 旧配置是否存在
  old_json=$(curl -s -G "${BASE}/v3/admin/cs/config" \
    --data-urlencode "dataId=${d}" \
    --data-urlencode "groupName=${NACOS_GROUP}" \
    --data-urlencode "namespaceId=${NACOS_NAMESPACE}" \
    --data-urlencode "${AUTH}")
  if ! echo "$old_json" | jq -e '.code == 0 and .data != null' >/dev/null 2>&1; then
    echo "    [跳过] 旧配置不存在或已删除: ${d}"; skip=$((skip+1)); continue
  fi

  # 2) 目标是否已存在（避免覆盖线上内容）
  new_json=$(curl -s -G "${BASE}/v3/admin/cs/config" \
    --data-urlencode "dataId=${new}" \
    --data-urlencode "groupName=${NACOS_GROUP}" \
    --data-urlencode "namespaceId=${NACOS_NAMESPACE}" \
    --data-urlencode "${AUTH}")
  if echo "$new_json" | jq -e '.code == 0 and .data != null' >/dev/null 2>&1; then
    echo "    [跳过] 目标已存在（未改动，请人工核对内容是否一致）: ${new}"; skip=$((skip+1)); continue
  fi

  # 3) 取出内容与类型
  content_file="${TMPDIR_CUSTOM}/${d}.yaml"
  echo "$old_json" | jq -r '.data.content // empty' > "$content_file"
  ctype=$(echo "$old_json" | jq -r '.data.type // "yaml"')
  if [ ! -s "$content_file" ]; then
    echo "    [失败] 旧配置内容为空: ${d}"; fail=$((fail+1)); continue
  fi

  # 4) 发布新 dataId
  pub=$(curl -s -X POST "${BASE}/v3/admin/cs/config?${AUTH}" \
    --data-urlencode "dataId=${new}" \
    --data-urlencode "groupName=${NACOS_GROUP}" \
    --data-urlencode "namespaceId=${NACOS_NAMESPACE}" \
    --data-urlencode "type=${ctype}" \
    --data-urlencode "content@${content_file}")
  if ! echo "$pub" | jq -e '.code == 0 and .data == true' >/dev/null 2>&1; then
    echo "    [失败] 发布 ${new} 失败: $(echo "$pub" | jq -r '.message // .' | head -c 200)"; fail=$((fail+1)); continue
  fi

  # 5) 删除旧 dataId
  del=$(curl -s -X DELETE "${BASE}/v3/admin/cs/config?${AUTH}" \
    --data-urlencode "dataId=${d}" \
    --data-urlencode "groupName=${NACOS_GROUP}" \
    --data-urlencode "namespaceId=${NACOS_NAMESPACE}")
  if ! echo "$del" | jq -e '.code == 0 and .data == true' >/dev/null 2>&1; then
    echo "    [警告] 新配置已发布 ${new}，但删除旧 ${d} 失败：$(echo "$del" | jq -r '.message // .' | head -c 200)（请手动在控制台删除旧条目）"
    fail=$((fail+1)); continue
  fi

  echo "    [OK] ${d} -> ${new}"
  ok=$((ok+1))
done

echo
echo "==> 完成：成功 ${ok}，跳过 ${skip}，失败 ${fail}"
[ "$fail" -gt 0 ] && exit 1
echo "==> 提示：改名后请重新部署对应服务（spring.config.import 现在请求 mall-xxx.yaml）。"
