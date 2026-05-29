## Why

Cat2Bug 已具备首次安装向导（`/setup`），但老版本实例升级到新版本时仍依赖静默配置迁移（`InstallConfigMigrationRunner`）与手工 Flyway/文档步骤：用户无法感知升级内容、无法在失败时重试，且配置合并可能覆盖已有定制。需要在版本升级场景提供与安装向导对称的 **Legacy 升级向导**，在升级完成前全锁系统、保留用户自定义配置，并在失败时提供可重试流程。

## What Changes

- 新增 Legacy 升级向导（前端 `/upgrade` + 后端 Upgrade API），在检测到 `upgradeRequired` 时引导用户完成配置确认与数据库迁移
- **升级期间全锁系统**：`upgradePending` / `upgradeFailed` / `restartRequired` 期间仅放行 `/upgrade/**`、静态资源与 `/version`，拒绝全部业务 API（含登录）
- **失败重试向导**：升级失败后进入 `upgradeFailed` 状态，展示错误摘要与已完成步骤，支持按步骤幂等重试
- **配置 merge 保留优先**：从 Environment / 旧 profile 与 classpath 模板合并写入 `application-install.yml` 时，保留磁盘与运行时已有有效值，仅补缺失项；用户在向导中显式修改的项可覆盖
- **改造静默迁移**：`InstallConfigMigrationRunner` 不再启动时静默写 completed install，改为设置 `upgradeRequired` 并进入升级向导
- 升级提交时执行 Flyway migrate（复用 `SetupMigrationService`），成功后标记 `restartRequired` 并提示重启
- 文档声明：**暂不支持多实例/集群在线升级**（升级前需确保仅一个进程运行）
- **BREAKING（行为变更）**：Legacy 实例首次启动到新版本时，不再自动静默生成 completed install 文件，必须先完成升级向导（可通过 `CAT2BUG_UPGRADE_SKIP=true` 或等价配置跳过，供 Docker 预配置场景）

## Capabilities

### New Capabilities

- `legacy-upgrade-wizard`：升级状态检测、Upgrade 模式全锁、配置确认与保留式 merge、Flyway 迁移执行、失败重试与重启引导

### Modified Capabilities

- `install-config-single-source`：Legacy 自动迁移从「静默写 completed install」改为「设置 upgradeRequired 并由升级向导确认写入」；merge 规则明确保留已有有效配置
- `first-run-setup-wizard`：`SetupFilter` / 路由逻辑扩展：已安装但 `upgradeRequired` 时重定向至 `/upgrade` 而非正常业务；与 Setup 模式互斥

## Impact

- **后端**：新增 `UpgradeController` / `UpgradeService` / `UpgradeFilter`；改造 `InstallConfigMigrationRunner`；扩展 `InstallService` 升级状态；新增 `UpgradeConfigMerger`；复用 `SetupMigrationService`、Flyway
- **前端**：新增 `views/upgrade/` 多步向导；`permission.js` 增加 `upgradeRequired` 重定向；复用 setup 步骤组件（连接测试、配置表单）
- **配置**：新增 `cat2bug.upgrade.*` 状态键（`state`、`lastError`、`lastStep`、`attemptCount`、`targetVersion`）；可选 `cat2bug.upgrade.skip`
- **数据库**：Flyway 脚本照常执行；升级状态可存 `sys_config` 或 install 文件侧车字段
- **部署**：单机 JAR / Docker 单容器为主要场景；多实例部署不在本变更范围
- **文档**：更新 `readme/production/faq.md` 升级章节，说明升级向导与单实例前提
