## Context

首次安装向导（`cat2bug-platform-ui/src/views/setup/index.vue`）在 `data().form` 中写死 H2 / 127.0.0.1 等默认值，仅调用 `GET /setup/status` 检测安装与重启状态。升级向导已实现 `GET /upgrade/preflight`，通过 `UpgradePreflightService` + `InstallConfigExporter.exportFromEnvironment` 从 Spring Environment 或磁盘 install 文件导出配置并 `patchFormFromPreflight`。

Docker Compose（`deploy/docker/docker-compose.yml`）在 `web-after` 服务注入 `SPRING_DATASOURCE_DRUID_MASTER_*`、`SPRING_REDIS_HOST` 等变量，但安装向导不读取，导致容器化首次安装需重复填写。用户约定：环境变量不全时沿用现有向导默认；既无 env 也无默认则留空；UI 不标注来源。

## Goals / Non-Goals

**Goals:**

- 提供 `GET /setup/preflight`，返回 Environment 可映射的表单字段。
- 向导 `created` 时拉取并逐字段合并（env → 现有默认 → 空）。
- 复用 `InstallConfigExporter` 与升级向导的 JDBC/Redis 解析，避免双份逻辑。
- 在缺少 `spring.database-type` / `cat2bug.cache.type` 时做最小推断（MySQL URL、redis host）。
- 补充单元测试与 Compose 文档/示例变量。

**Non-Goals:**

- 不通过环境变量预填管理员账号、注册开关、验证码等安全项（除非后续单独扩展）。
- 不在 UI 显示「来自环境变量」提示或一键恢复按钮。
- 不自动执行数据库/Redis/Ollama 连接测试（仍由用户点击测试）。
- 不改变 `CAT2BUG_INSTALL_SKIP` 跳过向导的语义。
- 不修改已安装后再次访问 setup 的行为（setup API 仍应拒绝）。

## Decisions

### 1. 新增独立端点 `GET /setup/preflight`

**选择：** 独立端点，而非扩展 `GET /setup/status`。

**理由：** 与 upgrade 对称；status 保持轻量轮询；preflight  payload 较大且仅在进入向导时请求一次。

**替代：** 合并进 status — 增加每次轮询负担，已否决。

### 2. 新建 `SetupPreflightService`，抽取共享表单字段解析

**选择：** 在 `cat2bug-platform-admin` 新增 `SetupPreflightService`；将 `UpgradePreflightService` 中的 `putFormFields`、`putMysqlFormFields`、`putRedisFormFields`、`parseMysqlJdbcUrl` 抽到 `SetupFormDefaultsSupport`（放在 admin 或 common 包）。

**理由：** setup 无磁盘 install 时仅读 Environment；upgrade 优先磁盘再 Environment。共享解析保证 host/port/db 一致。

**替代：** setup 直接调用 `UpgradePreflightService.buildPreflight()` — 会带上 Flyway diff、backup 等无关字段，语义混乱，已否决。

### 3. Setup preflight 仅读 Environment，不读未完成 install 文件

**选择：** `SetupPreflightService.buildPreflight()` 仅调用 `InstallConfigExporter.exportFromEnvironment(environment)`。

**理由：** 未安装阶段通常无 completed install；避免半写入文件干扰。

### 4. 类型推断规则（最小集）

| 信号 | 推断结果 |
|------|----------|
| `spring.database-type` 已设置 | 使用该值 |
| 否则 `spring.datasource.druid.master.url` 含 `jdbc:mysql` | `databaseType = mysql` |
| 否则 | 不返回 / 前端保留 `h2` |
| `cat2bug.cache.type` 已设置 | 使用该值 |
| 否则 `spring.redis.host` 非空 | `cacheType = redis` |
| 否则 | 前端保留 `local` |

不在 setup 阶段根据 env 自动切换 H2 文件路径等引导期行为。

### 5. 前端合并策略

**选择：** 新增 `getSetupPreflight()`；`patchFormFromPreflight(preflight)` 与 upgrade 类似，但对每个字符串字段使用：

```javascript
form.field = (preflight.field != null && preflight.field !== '')
  ? preflight.field
  : form.field  // 保留 data() 默认
```

密码等默认即为空的字段：preflight 无值时保持 `''`。布尔值：`aiEnabled` 仅当 preflight 显式 `true`/`false` 时覆盖（或 preflight 省略则保留默认）。

**不**在模板中加提示文案。

### 6. 安全与过滤器

**选择：** `GET /setup/preflight` 在未安装时与 `/setup/status` 同等放行（`SetupFilter`、`SecurityConfig` 已有 `/setup/**` 模式需确认包含新路径）。

**权衡：** 响应可能含数据库/Redis 密码，与 upgrade preflight 及 submit 测试接口一致，属引导期可接受风险。

### 7. Docker Compose 示例更新（文档层）

在 `deploy/docker/docker-compose.yml` 的 `web-after.environment` 增加推荐变量（非 breaking）：

```yaml
SPRING_DATABASE_TYPE: mysql
CAT2BUG_CACHE_TYPE: redis
SPRING_REDIS_PORT: 6379
```

可选注释 `CAT2BUG_AI_HOST`。不强制用户设置，推断仍可工作。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 推断误判（仅有 redis host 但用户想用 local 缓存） | 用户可在向导中改回 local；Compose 文档说明显式设置 `CAT2BUG_CACHE_TYPE` |
| 与 upgrade 解析逻辑漂移 | 抽取 `SetupFormDefaultsSupport` 单测 + 与 `UpgradePreflightServiceTest` 共用 JDBC 样例 |
| 引导期 Environment 含 Spring Boot 默认 redis localhost | `InstallConfigExporter` 仅导出 enumerable 中显式出现的 key；若 application.yml 默认导致误填，preflight 应只导出 property source 中**用户/env 层**设置的值 — 实现时以 `exportFromEnvironment` 遍历 enumerable keys 为准，classpath 默认值若出现在 Environment 中需验证；必要时 preflight 忽略与 classpath 模板相同的占位默认值 |
| Native / 路径类 env 与容器挂载不一致 | 路径字段仍按合并规则；Compose 文档列出 `CAT2BUG_PROFILE` 等可选变量 |

## Migration Plan

1. 部署新版本后，未完成安装的实例打开 `/setup` 即自动预填，无需数据迁移。
2. 无 Flyway / 数据库 schema 变更。
3. 回滚：移除 preflight 调用后向导恢复纯前端默认，不影响已写入的 install 文件。

## Open Questions

- （已关闭）UI 是否标注 env 来源 — **否**。
- （已关闭）env 不全时的策略 — **用现有向导默认**。
- 若实测发现 `exportFromEnvironment` 把 `application.yml` 的 `spring.redis.host: localhost` 当作预填，是否增加「仅当值来自系统环境变量 PropertySource」过滤 — 实现阶段用集成测试验证，必要时在 design 迭代中收紧。
