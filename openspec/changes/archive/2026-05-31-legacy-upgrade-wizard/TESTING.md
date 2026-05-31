# Legacy 升级向导 — 手工测试清单

本文档用于验证 `legacy-upgrade-wizard` 变更的端到端行为：从旧版本升级、无 completed install 文件、或存在待执行 Flyway 脚本时，进入 `/upgrade` 向导；升级期间全锁业务访问；完成后需重启。

建议在**单实例**、维护窗口内执行（文档声明不支持多 JVM 同时升级）。

## 前置条件

- JDK 17+
- 后端：`cd cat2bug-platform-admin && mvn spring-boot:run`
- 前端（开发模式）：`cd cat2bug-platform-ui && npm run dev`，访问 `http://localhost:2222`
- 或嵌入式前端：先 `npm run build:embedded` 再启动 JAR
- 升级前**备份**数据库与 `config/install/`、`uploadPath` 等业务目录

## 1. L2 Legacy 配置迁移（无 install 文件，有 user_id=1）

适用于：磁盘无 `application-install.yml`（或 `cat2bug.install.completed` 不为 true），但库中已有 legacy 数据（如 `sys_user.user_id = 1` 或 Flyway 已成功迁移）。

- [ ] 移走或删除 `./config/install/application-install.yml`（若存在）
- [ ] 保留含 legacy 管理员/业务数据的 H2 或 MySQL 库（**不要**删库）
- [ ] 确认**未**设置 `CAT2BUG_UPGRADE_SKIP=true`
- [ ] 启动应用后访问 `/` 或 `/login`，应重定向到 **`/upgrade`**（**不是** `/setup`）
- [ ] `GET /prod-api/upgrade/status` 返回 `upgradeRequired: true`、`state: pending`
- [ ] 启动时**不应**静默生成 completed install 文件（旧版静默迁移已改为 `markPending`）
- [ ] 升级概览步骤显示：当前版本 → 目标版本、**单实例**提醒、备份提醒

验证库表示例：

```sql
SELECT user_id, user_name FROM sys_user WHERE user_id = 1;
```

## 2. L3 Flyway 待执行迁移

适用于：已有 completed install 文件，但新版本 JAR 内含未执行的 Flyway 脚本。

- [ ] 使用已安装实例的数据库，替换为含新 migration 的版本 JAR
- [ ] 启动后 `GET /prod-api/upgrade/status` 的 `pendingMigrations` 列出待执行脚本（或概览步骤展示列表）
- [ ] `upgradeRequired: true`，进入 `/upgrade`
- [ ] 完成向导提交后 Flyway 执行成功，`state` 变为 `restart_required`

## 3. 升级期间全锁（登录与业务 API 不可用）

在 `state` 为 `pending`、`running`、`failed` 或 `restart_required` 期间：

- [ ] 访问 `/login` 或 `POST /prod-api/login` 被拒绝，错误文案提示需完成升级向导
- [ ] 访问业务 API（如 `/prod-api/system/user/list`）被拒绝
- [ ] `GET /prod-api/upgrade/status`、`GET /prod-api/upgrade/preflight` 无需登录可访问
- [ ] `GET /version` 可访问
- [ ] 静态资源（`.html`、`.js`、`.css`、`/static/` 等）可加载，升级页 UI 正常

## 4. 配置确认与 merge（保留优先）

- [ ] 步骤 2 展示 diff：已有 JDBC URL、profile 路径等**默认保留**
- [ ] 模板新增项（如缺失的 `cat2bug.cache.type`）高亮「建议补全」
- [ ] 在向导中显式修改 Redis host 后提交，写入 install 文件为用户修改值
- [ ] 步骤 3 预检：数据库连接、可选 Redis、路径可写（复用 setup 测试 API）

## 5. 提交、失败与重试

- [ ] 从 `pending` 提交合法配置：`POST /prod-api/upgrade/submit` 成功
- [ ] 磁盘生成/更新 `application-install.yml`，迁移成功前**不**应仅因提交就把 `cat2bug.install.completed` 设为 true（完成于 migration 成功后）
- [ ] 全部成功后 `state=restart_required`，UI 提示**重启应用**
- [ ] 模拟 Flyway 失败（错误连接、损坏脚本等）：`state=failed`，`lastStep=migration`，`lastError` 有摘要，UI 显示**重试**
- [ ] `POST /prod-api/upgrade/retry`：若 config 已成功则跳过重写，仅重试 migration（幂等）
- [ ] checksum 冲突场景（若适用）：重试前可触发 repair，再次 submit/retry

## 6. CAT2BUG_UPGRADE_SKIP=true 静默路径

适用于 Docker / CI 预配置，恢复旧版静默迁移行为。

- [ ] 设置 `export CAT2BUG_UPGRADE_SKIP=true`（或 `cat2bug.upgrade.skip=true`）
- [ ] Legacy 有数据、无 install 文件时启动，**不**进入 `/upgrade`
- [ ] 启动后可自动生成 completed `application-install.yml`（与升级前静默行为一致）
- [ ] 可正常访问 `/login` 与业务功能（无升级全锁）

## 7. 全新空库仍进入 /setup

- [ ] 删除 install 文件与 H2 数据文件（或空 MySQL 库）
- [ ] 库中**无** legacy admin / schema
- [ ] `GET /prod-api/upgrade/status` 返回 `upgradeRequired: false`
- [ ] 访问 `/` 重定向到 **`/setup`**，而非 `/upgrade`

## 8. 重启后恢复正常

- [ ] 升级提交成功且 `state=restart_required` 后，**停止并重启** JAR/进程
- [ ] 重启后 `GET /prod-api/upgrade/status`：`state=completed`，`upgradeRequired: false`
- [ ] 访问 `/upgrade` 被拒绝或重定向（与已安装实例一致）
- [ ] `/login` 与业务 API 正常；可用原 admin 账号登录
- [ ] `cat2bug.install.completed=true` 在 install 文件与运行态一致

## 9. 后端单元测试

```bash
mvn test -pl cat2bug-platform-common -Dtest=UpgradeConfigMergerTest
mvn test -pl cat2bug-platform-framework -Dtest=InstallConfigMigrationRunnerTest,UpgradeServiceTest,UpgradeFilterTest
```

- [ ] `UpgradeConfigMergerTest`：保留 base 非空值、模板补缺失、向导覆盖优先
- [ ] `InstallConfigMigrationRunnerTest`：未 skip 时 `markPending`、skip 时静默写 completed install
- [ ] `UpgradeServiceTest`：skip 标志、`markPending`、active 状态判定
- [ ] `UpgradeFilterTest`：升级 active 时放行 upgrade API、拦截 login
