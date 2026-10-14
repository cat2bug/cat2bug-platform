## Context

- 后端是唯一的 Quarkus 线（3.15.7），四个模块 `cat2bug-platform-{admin,system,framework,common}`，见 `docs/02-系统架构/整体架构.md`。
- 已用扩展：quarkus-rest / rest-jackson、arc、agroal、jdbc-h2 / jdbc-mysql、flyway / flyway-mysql、scheduler、websockets、mailer、smallrye-health，以及 Quarkiverse 的 quarkus-mybatis 2.3.0。
- 查 Maven Central 得到的兼容关系：
  - `quarkus-mcp-server`：1.0.0.Alpha1 要求 3.17.4，1.0.0 要求 3.20.0，1.8.x 要求 3.27.1，1.9.1 要求 3.27.2，2.0.x 要求 3.33。
  - `quarkus-mybatis`：2.3.0 按 3.20.0 构建（当前在 3.15.7 上运行正常），2.4.0 按 3.22，2.4.2 按 3.29.2。
- 交付有 JVM 单 JAR、Docker、Native + UPX、rpm 四种形态；Native 按 ADR-004 的两道门验收，体积口径为 UPX 后约 38.8 MiB。
- 老实例的安装配置 `config/install/quarkus-runtime.properties`（命名数据源 `runtime-h2` / `runtime-mysql`）由安装向导写出，升级后必须原样可用。

## Goals / Non-Goals

**Goals:**

- 升级到 3.27 LTS，全部现有测试、两种 Native 冒烟、浏览器主流程回归通过。
- 验证 `quarkus-mcp-server-http` 1.9.x 可以引入（编译、启动、Native 构建），但不在本变更暴露 MCP 端点。
- 老实例直接替换二进制即可升级：安装配置、数据库、Git 仓库、上传文件无需人工迁移。

**Non-Goals:**

- 不升级到 3.33 LTS / MCP 2.0：2.0 刚发布，3.33 跨度更大，留给以后。
- 不顺带做依赖瘦身、重构或新功能。
- 不改 JDK 版本（保持 17）。

## Decisions

### D1：目标版本选 3.27 LTS

- 选 3.27：同时满足 MCP 扩展 1.9.x（要求 3.27.2）与 quarkus-mybatis 2.4.x 的兼容区间，且是 LTS。
- 否决 3.20 LTS：能用 MCP 1.0～1.6，但这些版本对 Streamable HTTP、授权元数据的支持较早期，后续还要再升一次。
- 否决 3.33 LTS：MCP 2.0 刚发布，quarkus-mybatis 最新 2.4.2 只到 3.29，存在扩展跟不上的风险。

### D2：分步升级，每步全量测试

先把平台版本和 mybatis 一起提到 3.27.x（mybatis 选 2.4.x 中与 3.27 匹配的最高版本），按 Quarkus 官方迁移指南（3.16～3.27 各版本）逐条核对配置改名，再修编译错误，然后跑全量测试。不采用「先升到 3.20 再升 3.27」的两跳方案：中间版本没有交付价值，只会多一轮回归。

### D3：MCP 扩展只做引入验证

在一个临时分支或 Maven profile 中加入 `quarkus-mcp-server-http`，写一个最小的测试工具，确认 JVM 启动和 Native 构建都通过后移除，结论写进任务记录。正式引入放在 `mcp-server` 变更，避免本变更出现半成品端点。

### D4：配置兼容

升级后若有配置键改名，优先在 `application.properties` 中改为新键；对安装向导写出的运行期配置，如果新版本不再识别旧键，需要在启动期做兼容映射，并补一个「用 3.15 时代写出的配置文件启动」的测试。

## Risks / Trade-offs

- [quarkus-mybatis 2.4.x 在 3.27 下有行为差异] → 全量 Mapper 测试 + Native 冒烟；不行就退回 2.4.0 并记录原因。
- [Native 构建失败或体积明显变大] → 记录 UPX 前后体积；超过 ADR-004 口径 10% 时停下评估，不为体积裁功能。
- [Mandrel 构建镜像需要升级] → 同步更新 `deploy/build-native.sh` 中的镜像版本，并在 arm64 上先验证。
- [RESTEasy Reactive 默认行为变化（如 JSON 序列化、空响应、异常映射）] → 依赖现有集成测试和浏览器回归发现；`GlobalExceptionMapper` 与 `AjaxResult` 格式必须保持不变。
- [老实例升级后无法读取安装配置] → D4 中的兼容测试兜底。

回滚：保留升级前的 tag，升级后的版本若发现严重问题，可以直接换回旧二进制；本变更不改数据库结构，回滚无需处理数据。

## 相关长期文档

- `docs/02-系统架构/整体架构.md`（版本号需更新）
- `docs/02-系统架构/部署.md`
- `docs/00-项目总览.md`（技术栈版本号需更新）
- `docs/03-技术方案/Native镜像与操作日志对等.md`
- `docs/07-技术决策/ADR-004-Native交付分两道门并接受约38MiB.md`
- `docs/07-技术决策/ADR-010-移除Spring后端并统一模块命名.md`

拟新增 ADR：无。目标版本的取舍写在本设计中；如果升级过程中被迫改变 Native 体积口径或交付形态，再补 ADR。
