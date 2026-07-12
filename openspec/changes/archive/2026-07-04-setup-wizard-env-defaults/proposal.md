## Why

Docker Compose 与容器化部署常通过环境变量预置 MySQL、Redis、Ollama 等连接信息，但首次安装向导（`/setup`）前端仍使用写死的本地默认值（如 `127.0.0.1`、H2、本地缓存），运维需在向导中重复填写与 Compose 一致的配置。升级向导已通过 preflight 从 Environment 预填表单，安装向导缺少对等能力，导致「环境已配好、向导仍要手改」的体验断层。

## What Changes

- 新增 `GET /setup/preflight`（或等价接口），从 Spring `Environment` 导出安装相关配置并映射为向导表单字段。
- 安装向导页面在初始化时拉取 preflight，按字段合并：环境变量有值则预填；无 env 则保留现有向导默认值；既无 env 也无默认则留空。
- 复用或抽取与 `UpgradePreflightService`、`InstallConfigExporter` 一致的 JDBC URL 解析与 Redis/AI 字段映射逻辑。
- 在 `database-type` / `cache.type` 未显式设置时，根据 JDBC URL（`jdbc:mysql`）与 `spring.redis.host` 等做类型推断，推断失败则沿用现有默认（H2 / local）。
- 不在 UI 上标注「来自环境变量」；预填行为对用户透明。
- 可选：更新 `deploy/docker/docker-compose.yml` 示例，补充 `SPRING_DATABASE_TYPE`、`CAT2BUG_CACHE_TYPE` 等推荐变量，使 Compose 场景开箱即用。

## Capabilities

### New Capabilities

- `setup-wizard-env-defaults`: 首次安装向导从运行环境读取基础设施默认值并预填表单，支持与 Docker/Compose 环境变量对齐。

### Modified Capabilities

（无：仓库尚无 `openspec/specs/` 基线能力文档。）

## Impact

- **后端**：`SetupController`、新建 `SetupPreflightService`（或共享表单默认值服务）、`SetupFilter` / `SecurityConfig` 白名单（若新端点需放行）。
- **前端**：`cat2bug-platform-ui/src/views/setup/index.vue`、`src/api/setup.js`。
- **公共模块**：可能扩展 `InstallConfigExporter` 或抽取 `SetupFormDefaultsSupport` 供 setup/upgrade 共用。
- **部署文档**：`deploy/docker/README.md`、`docker-compose.yml` 示例环境变量。
- **测试**：`SetupPreflightService` 单元测试（Environment 导出、字段合并、类型推断）；可选前端或 API 集成测试。
