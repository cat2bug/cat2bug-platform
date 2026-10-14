# Vite 代理用 localhost 解析到 IPv6 导致 500

记录 macOS 上前端开发代理连不上 Quarkus dev 的原因与修法。

- 日期：2026-10-14
- 涉及：前端 / Quarkus 后端

## 现象

`npm run dev`（端口 2222）打开页面后，所有走代理的接口（`/dev-api/**`、`/git/**`）返回 500 或连接失败；直接访问 `http://127.0.0.1:2020` 正常。

## 根因

`cat2bug-platform-ui/vite.config.js` 的 `proxy.target` 写的是 `http://localhost:2020`。Node 在 macOS 上会把 `localhost` 优先解析为 IPv6 `::1`，而 Quarkus dev 只监听 IPv4，代理连接被拒，Vite 把错误回成 500。

## 解决办法

代理目标全部改为 `http://127.0.0.1:2020`（提交 `59f6a23e`，涉及 API 代理、`/git` Smart HTTP 代理等）。

## 以后怎么避免

- 本地代理、脚本、冒烟里的后端地址统一写 `127.0.0.1`，不写 `localhost`。
- 遇到「浏览器直连正常、经代理 500」时，先怀疑 IPv4/IPv6 解析差异。
