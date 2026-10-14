## Why

要把 cat2bug 做成「代码质量管理 + AI」平台，第一步是让 Claude、Cursor、Codex、OpenCode 等 AI 客户端通过远程 MCP 直连平台（见变更 `mcp-server`）。Quarkus 官方的 MCP 扩展 `quarkus-mcp-server` 最早版本要求 Quarkus 3.17，正式版要求 3.20，当前后端停在 3.15.7，无法引入。3.15 的 LTS 维护期也即将结束，升级迟早要做，先单独做、单独验收，把风险和后续功能隔开。

## What Changes

- Quarkus 平台从 3.15.7 升级到 3.27 LTS（以开工时最新的 3.27.x 补丁版为准）。
- 第三方扩展同步升级到与 3.27 兼容的版本：`quarkus-mybatis` 2.3.0 → 2.4.x；其余依赖（JGit、FastExcel、fastjson2、JJWT、Jedis 等）只在编译或运行不兼容时调整。
- 处理升级带来的 API / 配置变化（废弃或改名的配置项、RESTEasy Reactive / Vert.x / Flyway / Agroal / Mailer / WebSockets 行为差异），保持对外接口、数据库结构、安装向导和交付形态不变。
- 预先确认 `quarkus-mcp-server-http`（1.9.x，对应 3.27.2）能在本项目编译、启动并通过 Native 构建，为 `mcp-server` 变更扫清依赖障碍；本变更不暴露任何 MCP 端点。
- 同步更新文档中的 Quarkus 版本号。

## Capabilities

### New Capabilities

- `quarkus-lts-baseline`：后端运行在 Quarkus LTS 版本上，并保证升级前后接口、数据、交付形态和 Native 体积口径不回退。

### Modified Capabilities

（无。现有规范没有写死 Quarkus 版本，升级不改变已有需求。）

## Impact

- 构建：根 `pom.xml` 的 `quarkus.platform.version`、`cat2bug-platform-admin/pom.xml` 的 `quarkus.mybatis.version`。
- 代码：四个模块中因 Quarkus API 变化需要调整的地方（预计集中在 admin 的 REST 资源、过滤器、WebSocket、调度和 Native 配置）。
- 配置：`application*.properties` 中改名或废弃的键；安装向导写出的 `config/install/quarkus-runtime.properties` 必须继续可读，否则老实例升级后无法启动。
- 交付：JVM 单 JAR、Docker Compose、Native（arm64 / amd64）、rpm 全部重新构建并冒烟；Native 体积允许有小幅波动，但必须记录。
- 风险：Native 构建所用的 Mandrel 镜像版本需随 Quarkus 升级。
