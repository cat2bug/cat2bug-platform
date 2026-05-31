## Context

Cat2Bug 当前状态（0.6.2 + legacy-upgrade-wizard 已落地）：

- **Setup**：MySQL 测试连接时库已存在 → 失败；H2 固定 `./data/cat2bug`；提交时 H2 已有 Flyway 记录则跳过 migrate
- **Legacy 检测**：无 install 文件 + `user_id=1` 或 Flyway 历史 → `markPending` → `/upgrade`
- **Upgrade**：`install.completed=true` + Flyway pending → `upgradeRequired`；`CAT2BUG_UPGRADE_SKIP` 主要覆盖无 install 的 legacy 静默写盘

**产品决策（已确认）**：

| 项 | 决策 |
|----|------|
| 存在性判定 | 仅库/文件是否存在；不查 `sys_defect` / `sys_case` |
| H2 存在 | 仅认 `./data/{name}.mv.db` |
| MySQL 存在 | `information_schema.SCHEMATA` |
| 旧库安装路径 | 留在 `/setup`，不 Flyway |
| 旧库 admin | 覆盖用户名/密码 |
| install 后升级 | pending → `/upgrade` 或 skip 静默 migrate |
| 静默 migrate 失败 | 进程 exit ≠ 0 |

## Goals / Non-Goals

**Goals:**

- H2 / MySQL 对称的「数据库名 + new/existing 模式」
- 首次 install（含附着旧库）统一走 setup
- Schema 版本追赶仅在 install 完成后通过 upgrade（或 skip 静默）
- Docker 自动化部署可跳过 UI 锁，但 migrate 失败必须 fail-fast

**Non-Goals:**

- 跨库类型迁移（H2 → MySQL 数据搬迁）
- 多实例滚动升级
- 安装阶段对 existing 库做 schema 校验/部分 migrate
- 业务表级别的 legacy 检测

## Decisions

### 1. `databaseMode` 由测试 API 判定并提交时信任

**决策**：`POST /setup/test/database` 响应增加 `databaseMode: new | existing`；submit 请求携带同字段，服务端校验与当前检测结果一致（或 submit 时重新探测一次）。

**理由**：前端步骤清晰；避免 submit 时库状态突变（极低概率）导致误 migrate。

**备选**：仅服务端 submit 时探测 —— 前端无法展示「将附着旧库」提示，不采用。

### 2. H2 库名 → 文件路径

**决策**：库名 `{name}` 默认 `cat2bug_platform`；存在性 = `Files.exists(./data/{name}.mv.db)`；JDBC URL = `jdbc:h2:file:./data/{name};MODE=MySQL;DATABASE_TO_LOWER=TRUE;`

**理由**：与现有默认路径一致；`.mv.db` 为 H2 2.x 单文件形态。

### 3. existing 模式 submit 跳过 Flyway

**决策**：

```
submit + databaseMode=existing:
  MySQL: 不 CREATE DATABASE，不 migrate
  H2:    不 migrate
  共用:  write install yml, createOrUpdateAdmin (覆盖), sys_config, completed=true
```

**理由**：旧库 schema 版本可能低于 JAR；由 install 后 upgrade 统一追赶。

**风险**：existing 库为空壳（有 .mv.db 但无表）→ 登录后业务报错 → 文档说明应使用 new 模式或删文件重装。

### 4. 无 install 时不再 markPending 进 upgrade

**决策**：`InstallConfigMigrationRunner` 在无 install 文件时 **不再** 调用 `markPending()`；legacy 用户通过 setup existing 写 install。

**改造 `UpgradeService.isUpgradeRequired()`**（无 install 分支）：

- 移除 `hasLegacyInstallation()` / `isSchemaPresent()` 作为 upgrade 触发条件
- 仅保留：`isUpgradeActive()`（进行中的 upgrade 状态机）

**install 完成后**：

- `hasPendingSchemaUpgrade()` → `upgradeRequired=true`

**理由**：setup / upgrade 职责分离（见 proposal 流程图）。

### 5. Skip 路径：已安装 + pending → 静默 migrate

**决策**：新增 `PostInstallFlywayRunner`（`ApplicationRunner`, `@Order` 在业务初始化之后）：

```
IF install.completed AND upgrade.skip AND hasPendingMigrations:
  SetupMigrationService.migrate(...)
  on success: 可选更新 completedVersion，正常启动
  on failure: log error + System.exit(1)
```

**与 `InstallConfigMigrationRunner` skip 分支关系**：

- 无 install + skip：保留现有静默写 completed install（Docker 预置）
- 有 install + skip + pending：本 Runner 静默 migrate

**理由**：Docker 换 JAR 后无需人工点 upgrade UI；失败 exit 便于编排重启/告警。

### 6. 路由优先级（permission.js）

```
1. upgrade.skip + installed + pending → 不锁 UI（Runner 已处理或启动中）
2. !installed → /setup（不再 upgradeRequired 无 install 分支）
3. installed + upgradeRequired + !skip → /upgrade
4. 正常业务
```

### 7. Admin 覆盖

**决策**：existing 模式仍调用 `createOrUpdateAdmin`，更新 `user_id=1` 的用户名与密码哈希。

**理由**：产品确认；附着旧库时以向导配置为准。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| existing 空库（有文件无表） | UI 提示 existing = 附着已有数据；测试通过仅表示文件在 |
| install 后不 migrate 导致首次登录缺列 | install 后立即检测 pending → upgrade 或 skip 静默 |
| submit 伪造 databaseMode=existing 跳过 migrate | submit 服务端复验存在性 |
| 静默 migrate 失败 exit 导致 K8s 重启循环 | 文档：skip 场景需备份；失败查日志修 Flyway |
| 与已部署 legacy-upgrade 行为 BREAKING | 发布说明 + TESTING.md 覆盖迁移路径 |

## Migration Plan

1. **发布说明**：老用户无 install 文件首次启动 → 进 setup（选已有库名）而非 upgrade
2. **Docker**：预置 install 文件 + `CAT2BUG_UPGRADE_SKIP=true` → 换 JAR 后自动 migrate
3. **回滚**：还原 JAR；若 Flyway 已执行需 DB 备份恢复
4. **开发验证**：见 `TESTING.md`

## Open Questions

- Phase 2：setup UI 对 `existing` 模式显示「将跳过建表，版本升级在 install 完成后进行」专用文案（MVP 可用通用 hint）
- H2 库名是否允许子路径（如 `data/custom/name`）—— MVP 限制 `[A-Za-z0-9_]+` 与 MySQL 一致
