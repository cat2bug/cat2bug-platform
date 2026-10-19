# 反代 HTTPS 未配 public-url 导致 MCP 显示 http 且 Logout 405

- 日期：2026-10-18
- 涉及：部署 / MCP / OAuth

## 现象

- 浏览器以 HTTPS（如 `https://cloud.example.com`）打开系统，项目设置「MCP 接入」示例地址却是 `http://…/mcp`。
- Cursor 把 MCP URL 改成远程后点 Logout，报 `HTTP 405`，之后无法重新登录。

## 根因

1. 未配置 `cat2bug.public-url`（`CAT2BUG_PUBLIC_URL`）时，OAuth issuer / MCP 地址按 `X-Forwarded-Proto` / `Host` 推导。
2. 外层 nginx 终止 TLS 再转发到容器 HTTP 时，若未传 `X-Forwarded-Proto: https`（或容器内 `/prod-api/` 未转发该头），后端推导成 `http://`。
3. 元数据里的 `revocation_endpoint` 因此是 `http://…/oauth/revoke`；客户端对 http 口 POST revoke 时，部分部署的 http vhost 直接返回 nginx `405 Not Allowed`（未正确反代），或 http→https 重定向把 POST 变成 GET，Quarkus 的 revoke 只接受 POST 也会 405。

## 解决办法

1. **优先**：容器环境变量 `CAT2BUG_PUBLIC_URL=https://cloud.example.com`（无尾斜杠），重启后端。
2. 外层与容器内 nginx：所有反代 location（含 `/prod-api/`、`/ws/`、OAuth/MCP）设置并透传 `X-Forwarded-Proto` / `X-Forwarded-Host`（见 `deploy/docker/nginx.conf`）。
3. Cursor：`mcp.json` 使用 `https://…/mcp`；元数据修复后若 Logout 仍卡死，删除该 MCP 条目再重新添加并登录。

## 以后怎么避免

- HTTPS 域名部署时默认写上 `CAT2BUG_PUBLIC_URL`，不要只依赖请求头推导。
- 改完后用 `curl https://<host>/.well-known/oauth-authorization-server` 确认 `issuer` 以 `https://` 开头。
