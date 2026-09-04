# FRP 内网穿透隧道（支付宝异步通知专用）

> 2026-08-16 搭建。用途：将内网 mall-order 的 `/payed/notify` 暴露到公网，
> 替代已失效的花生壳隧道（`your-tunnel.example.com`），接收支付宝沙箱异步通知。

## 1. 架构

```
支付宝服务器
   │  HTTP POST /payed/notify
   ▼
http://CHANGE_ME_PUBLIC_IP/  (公网 80)
   │  frps 服务端（阿里云服务器 lavm-2bnjf2yec3，Ubuntu 24.04）
   │  控制通道 443/TLS（token 认证）
   ▼
frpc 客户端（内网 Windows 机器 localhost，**主动外连**）
   │  TCP 转发
   ▼
127.0.0.1:10015  (mall-order)
```

关键点：frpc 从内网**主动外连** frps，因此内网机器不需要任何入站端口/端口映射。

## 2. 服务端（frps）——已部署完毕，一般无需再动

### 2.1 安装

```bash
# 下载最新版（以 v0.71.0 为例）
curl -sL -o /tmp/frp.tar.gz \
  "https://github.com/fatedier/frp/releases/download/v0.71.0/frp_0.71.0_linux_amd64.tar.gz"
tar xzf /tmp/frp.tar.gz -C /tmp
mkdir -p /opt/frp
cp /tmp/frp_*/frps /opt/frp/
```

### 2.2 配置 `/opt/frp/frps.toml`

```toml
bindPort = 443

transport.tls.force = true

auth.method = "token"
auth.token = "2e6872dd2a7e5745f6ba09409cd5285f"

log.to = "/var/log/frp/frps.log"
log.level = "info"
log.maxDays = 7
```

端口说明：

| 端口 | 用途 | 安全组状态 |
|---|---|---|
| 443 | frpc 控制通道（TLS + token） | 已放行 |
| 80 | 公网 HTTP 转发（通知入口） | 已放行 |

### 2.3 systemd 开机自启

```bash
cat > /etc/systemd/system/frps.service <<'EOF'
[Unit]
Description=FRP Server
After=network.target

[Service]
Type=simple
ExecStart=/opt/frp/frps -c /opt/frp/frps.toml
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now frps
systemctl status frps
```

常用运维命令：

```bash
systemctl restart frps          # 重启
systemctl stop frps             # 停止
tail -f /var/log/frp/frps.log   # 看日志
```

## 3. 客户端（frpc）——部署在运行 mall-order 的内网机器

### 3.1 下载（Windows）

- 直链：<https://github.com/fatedier/frp/releases/download/v0.71.0/frp_0.71.0_windows_amd64.zip>
- GitHub 慢时加加速前缀：`https://ghfast.top/https://github.com/fatedier/frp/releases/download/v0.71.0/frp_0.71.0_windows_amd64.zip`

解压后目录里就是 `frpc.exe`。

### 3.2 配置 `frpc.toml`（与 frpc.exe 同目录）

```toml
serverAddr = "CHANGE_ME_PUBLIC_IP"
serverPort = 443
transport.tls.enable = true

auth.method = "token"
auth.token = "2e6872dd2a7e5745f6ba09409cd5285f"

[[proxies]]
name = "mall-order-notify"
type = "tcp"
localIP = "127.0.0.1"
localPort = 10015
remotePort = 80
```

> 若 mall-order 与 frpc 不在同一台机器，把 `localIP` 改成 mall-order 所在机器的内网 IP。

### 3.3 运行

```bash
frpc.exe -c frpc.toml
```

看到 `start proxy success` 即成功。**开机自启**（二选一）：

- 任务计划程序：开机触发，程序指向 frpc.exe，参数 `-c frpc.toml`，起始目录为 frpc 所在目录；
- 或 nssm：`nssm install frpc "C:\path\frpc.exe" "-c C:\path\frpc.toml"`。

### 3.4 验证

```bash
# 任意公网设备上执行，返回非"连接拒绝"即隧道通（如 404/Whitelabel 也算通）
curl -X POST http://CHANGE_ME_PUBLIC_IP/payed/notify -d "test=1"
```

隧道通后，支付宝沙箱走一单支付，检查：

```sql
SELECT * FROM oms_payment_info ORDER BY id DESC LIMIT 5;  -- 应有新记录
SELECT order_sn, status FROM oms_order WHERE order_sn = '你的订单号';  -- status 应为 1(已付款)
```

## 4. 关联配置

- 支付宝通知地址：`mall-order/src/main/resources/application.properties` 与 `application-prod.properties`
  ```properties
  alipay.notify_url=http://CHANGE_ME_PUBLIC_IP/payed/notify
  ```
  配置已提交（commit `cc6608c`），重新部署 mall-order 后生效。
- 本地备份：`/root/.openclaw/workspace/frp-windows/frpc.toml`（frpc 配置副本）。

## 5. 故障排查

| 现象 | 排查 |
|---|---|
| frpc 连不上，日志 `login to server failed` | 443 是否放行；token 是否一致；frps 是否在运行 |
| 公网访问 80 超时 | 安全组 80 是否放行；frpc 是否在运行（frps 仅在 frpc 注册代理后才监听 80） |
| 通知能到但支付表无数据 | 看 mall-order 日志 `/payed/notify` 是否 400（检查 Date 绑定修复是否已部署，commit `4ec07a8`） |
| frps 起不来 | `journalctl -u frps -n 50` 看报错；443 是否被占用 |

## 6. 注意事项

- **frpc 必须常驻**：Windows 机器关机/断网时通知收不到，支付宝会重试数次后放弃。
- **token 安全**：本文件含 token，若仓库是公开的请立即更换：改 `/opt/frp/frps.toml` 中 `auth.token`，重启 frps，并同步修改 frpc.toml。
- **生产环境**：支付宝正式环境要求 notify_url 为**域名**（IP 不行）。上生产时把域名解析到 `CHANGE_ME_PUBLIC_IP` 即可，frp 架构无需改动。
- frps 只做 TCP 透传，不落任何业务数据。
