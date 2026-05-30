## Why

当前安装向导对 MySQL 的「测试连接」在库已存在时直接失败，无法附着已有 Cat2Bug 数据库完成首次 install 配置写入；同时无 install 文件的 legacy 实例会被强制进入 `/upgrade`，与「首次安装仍走 setup、版本追赶走 upgrade」的产品模型不一致。需要统一 **库在 → 附着不建表、库不在 → 建库/建表** 的规则，并在 install 完成后通过升级向导（或 Docker 静默 migrate）处理 schema 版本落后。

## What Changes

- H2 / MySQL 安装向导均增加可配置的「数据库名」（默认 `cat2bug_platform`，H2 对应 `./data/{name}.mv.db`）
- **数据库测试 API** 返回 `databaseMode: new | existing`：仅依据库/文件是否存在判定，不再检查业务表
  - H2：`./data/{name}.mv.db` 存在 → `existing`
  - MySQL：schema 在 `information_schema` 中存在 → `existing`
- **MySQL 测试连接**：库已存在时测试通过（与当前「库存在即失败」相反）
- **Setup 提交**：
  - `new` → MySQL CREATE DATABASE + Flyway migrate；H2 Flyway migrate
  - `existing` → **不** Flyway、不建表；仍写 install 配置；**覆盖**向导中的 admin 用户名/密码
- **路由调整**：无 install 文件的 legacy 实例统一进入 `/setup`（existing 模式），不再因 `user_id=1` / Flyway 历史强制 `/upgrade`
- **install 完成后**：每次启动检测 Flyway pending；需要升级时进入 `/upgrade` 全锁向导
- **Docker / 自动化**：`CAT2BUG_UPGRADE_SKIP=true` 时对已安装实例 **后台静默 Flyway migrate**；失败则 **进程非零退出**
- **BREAKING**：无 install + 有 legacy 数据时不再 `markPending` 踢到 upgrade；改为 setup existing 附着

## Capabilities

### New Capabilities

- `setup-database-mode`: 安装向导 H2/MySQL 数据库名、存在性检测、`databaseMode` 测试 API、按模式决定是否 Flyway

### Modified Capabilities

- `first-run-setup-wizard`: 数据库步骤 UI/API、提交时 conditional migrate、existing 库 admin 覆盖、无 install 时一律 setup 路由
- `legacy-upgrade-wizard`: 触发条件收窄为「install 已完成 + pending 迁移」；skip 路径扩展为已安装实例静默 migrate + 失败 exit
- `install-config-single-source`: 移除/替代「无 install 时 legacy 自动写 completed install 或 markPending」；bootstrap H2 路径可配置库名

## Impact

- **后端**：`SetupDatabaseTestService`、`SetupMysqlDatabaseService`、`SetupInstallService`、`SetupConfigWriter`；`InstallConfigMigrationRunner` 行为调整；新增/扩展 `PostInstallMigrationRunner`（skip 静默 migrate）；`UpgradeService.isUpgradeRequired()` 判定顺序
- **前端**：`views/setup/index.vue` H2 库名字段、测试结果显示 new/existing、提交携带 `databaseMode`
- **路由**：`permission.js` 无 install 时仅 `/setup`；有 install + pending + 非 skip → `/upgrade`
- **部署**：Docker Compose 文档补充 `CAT2BUG_UPGRADE_SKIP` 静默 migrate 与失败退出语义
- **文档**：`readme/production/faq.md`、安装/升级测试清单
