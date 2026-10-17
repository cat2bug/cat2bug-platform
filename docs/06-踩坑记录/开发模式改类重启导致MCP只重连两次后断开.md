# 开发模式改类重启导致 MCP 只重连两次后断开

- 日期：2026-10-17
- 涉及：后台（Quarkus 开发模式 / MCP）

## 现象

Cursor 里的 Cat2Bug MCP 在同一晚反复变成 Not connected。日志里先是 `POST /mcp` 收到 `<!doctype html>`，接着 `Failed to open SSE stream: Internal Server Error`，然后 `Maximum reconnection attempts (2) exceeded`。此后工具调用一直是 Not connected，直到在 MCP 设置里重新登录。同一窗口里直接访问 `127.0.0.1:2020` 会出现 `ECONNREFUSED`，过一会儿端口又在听。

## 根因

`quarkus:dev` 默认 `quarkus.live-reload.instrumentation=false`。改 Java 源码会整进程重启。2026-10-17 02:35–02:40 改了 webhook 相关类和 `NativeRuntimeInitFeature`，02:41 拉起了新的 `quarkus:dev`（新的 Java PID）。

重启过程中旧进程已经不接连接，新进程还没起来时返回连接拒绝；过渡期 `/mcp` 还会吐出 Quarkus 的 HTML 页，而不是 JSON-RPC。Cursor 的 Streamable HTTP 客户端只重连 SSE 两次，失败后停在断开，不会在服务恢复后自己再连。

这和「回环占满工作线程、端口仍监听但全部超时」不是同一次故障。见 [[MCP回环占满工作线程导致Quarkus假死]]。

## 解决办法

开发配置打开字节码增强：`%dev.quarkus.live-reload.instrumentation=true`。能热替换的改动不再重启，已建立的 MCP 会话可以留住。改方法签名、增删类、改构建期配置仍会重启，重启后需要在 Cursor 里重连 MCP。

访问令牌保持 1 小时，刷新令牌 30 天。连接被 Cursor 放弃后，若访问令牌已过期，再连时会要求重新登录。HTTP 空闲超时从默认 30 分钟改为 3 小时。

## 以后怎么避免

- 有 MCP 客户端连着开发进程时，不要同时改会触发整进程重启的 Java。
- 看到 `/mcp` 返回 HTML 或 `ECONNREFUSED`，先看是不是 `quarkus:dev` 正在重启，不要当成令牌失效去解密钥链。
- 服务恢复后若工具仍是 Not connected，重新登录 MCP；客户端不会在两次 SSE 失败后自动恢复。
