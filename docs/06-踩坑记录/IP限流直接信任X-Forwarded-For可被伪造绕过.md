# IP 限流直接信任 X-Forwarded-For 可被伪造绕过

记录按 IP 限流时「客户端 IP 取哪个」的规则，避免限流形同虚设。

- 日期：2026-10-15
- 涉及：后端 / 部署

## 现象

OAuth 动态注册、换令牌接口按 IP 限流。若取 IP 的逻辑是「有 `X-Forwarded-For` 就取第一段」，攻击者每次请求带不同的 `X-Forwarded-For: 1.2.3.x` 即可无限注册 / 暴力尝试。

## 根因

`X-Forwarded-For` 是请求方可任意填写的头；只有经过**自己的**反向代理时，代理追加的最后一段才可信。第一段恰恰是最容易伪造的。原有 `ClientInfo.getIp` 为日志展示而写，取第一段，不适合安全用途。

## 解决办法

新增 `IpRateLimiter.clientKey(request)`：

- 直连地址是回环 / 内网 / 链路本地（说明前面是本机或内网的反向代理）时，取 `X-Forwarded-For` 的**最后一段**，其次 `X-Real-IP`；
- 公网直连一律用 TCP 连接地址，忽略这两个头；
- 判断地址类型前先用正则确认是 IP 字面量，再 `InetAddress.getByName`，避免触发 DNS 解析。

## 以后怎么避免

- 用于限流、封禁、审计判定的 IP 一律用 `IpRateLimiter.clientKey`，不要用 `ClientInfo.getIp`。
- 反向代理配置 `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for`（追加而非覆盖），见 [[部署]]。
- 若代理链有多层，要按实际层数取，不能简单取第一段。
