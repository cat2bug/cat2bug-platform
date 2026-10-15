# MCP 回环占满工作线程导致 Quarkus 假死

- 日期：2026-10-15
- 涉及：后台（MCP / Quarkus 开发模式）

## 现象

`quarkus:dev` 进程仍监听 `2020`，但任意 HTTP（含 `/setup/status`、`/upgrade/status`）超时无响应，CPU≈0。前端路由守卫一直等安装/升级状态，页面只剩主题色选择器。重启后端后恢复。连着 Cursor 等 MCP 客户端、并行打多个工具时更容易出现。

## 根因

精做工具与通用通道经 `McpGateway` **同步** `HttpClient` 打回 `127.0.0.1:{port}`（见 [[ADR-015-MCP用精做工具加通用通道-经回环转发复用网页接口]]）：

1. MCP 工具占用 Quarkus **工作线程 A**，阻塞等待回环响应；
2. 嵌套的网页 REST 还要再占 **工作线程 B** 才能进过滤器链；
3. 并行工具数 ≥ 线程池大小时，A 都在等 B，B 无人可派 → **自死锁**。端口由 event loop 接受连接，所以仍显示 Listening。

次要放大：登录页会强制刷新 `/upgrade/status`，内部跑 Flyway `info()`，也会占工作线程；前端守卫对这两个接口无超时，后端一挂就白屏。

卡死时用 `jstack <pid>`：大量 `executor-thread` 停在 `McpGateway.call` / `HttpClient` / `CompletableFuture.get`，即可确认。

## 解决办法

在 `McpGateway` 用信号量限制在途回环数，默认 `max(4, quarkus.thread-pool.max-threads/3)`，为嵌套 REST 与网页请求预留约 2/3 工作线程；可用 `cat2bug.mcp.loopback.max-in-flight` 覆盖。取不到许可时工具返回「系统繁忙」而不是拖死整进程。回环发送改为 `sendAsync` + 限时 `get`（调用方线程仍会等待，故信号量不可省）。

## 以后怎么避免

- 新增会同步回环的 MCP 路径时，不要绕过 `McpGateway` 的限流。
- 长期若要把工具改成返回 `Uni` / `CompletionStage` 并在等待 IO 时释放工作线程，可再收紧或去掉信号量，但须保证工具方法内不再做阻塞 JDBC / `Thread.sleep`。
- 开发机假死先 `jstack` 再杀进程；前端安装/升级探测宜带超时降级（另项优化）。

相关：[[MCP接入与权限]]、[[ADR-015-MCP用精做工具加通用通道-经回环转发复用网页接口]]
