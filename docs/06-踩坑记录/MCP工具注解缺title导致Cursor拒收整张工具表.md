# MCP 工具注解缺 title 导致 Cursor 拒收整张工具表

- 日期：2026-10-15
- 涉及：后台（MCP 服务）

## 现象

Cursor 完成 OAuth 登录、显示已连接，但看不到任何 cat2bug 工具。Cursor 的 MCP 日志里 `listOfferingsForUI sub-call failed`，逐条报 `tools[n].annotations.title: Invalid input: expected string, received null`。

## 根因

quarkus-mcp-server 的 `@Tool.Annotations` 不写 `title` 时，`tools/list` 里会输出 `"title": null`，而不是省略这个字段。Cursor 按 MCP 规范做 schema 校验，只要有一个工具不合格，就把整张工具表丢掉。服务端自己的测试只看工具名，所以发现不了。

## 解决办法

每个 `@Tool(annotations = @Tool.Annotations(...))` 都显式写 `title`（取工具名转成英文短语，如 `search_defects` 写 `Search defects`）。`QuarkusMcpServerTest.toolsListedAndDefectFlow` 断言每个工具的 `annotations.title` 要么不出现、要么是字符串。

服务端修好后，Cursor 仍显示 0 个工具：它把这次拉取失败的结果缓存在 MCP 后台进程里，之后开关服务、重新登录、Reload Window 都只做 `initialize`，不再发 `tools/list`。把 `.cursor/mcp.json` 里的服务名改掉（例如 `cat2bug` 改成 `cat2bug-platform`）或彻底退出 Cursor 后，才会重新拉取。排查时可在开发环境临时打开 `quarkus.mcp.server.traffic-logging.enabled` 和 `quarkus.http.access-log.enabled`，看客户端到底发了哪些请求。

## 以后怎么避免

- 新增 MCP 工具时，只要写了 `@Tool.Annotations`，就必须带 `title`。
- 判断「MCP 工具可用」要看真实客户端（Cursor）的日志能不能列出工具，不能只看服务端测试。

相关：[[MCP接入与权限]]
