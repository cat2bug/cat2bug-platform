## 1. 后端 — 数据库模式检测与测试 API

- [x] 1.1 新增 `DatabaseExistenceProbe`（H2 `.mv.db` / MySQL `information_schema`）
- [x] 1.2 `SetupDatabaseTestService` 返回 `databaseMode: new|existing`；MySQL 库已存在时测试通过
- [x] 1.3 `SetupSubmitRequest` / DTO 增加 `databaseMode` 字段与校验
- [x] 1.4 Submit 时复验 `databaseMode` 与探测结果一致

## 2. 后端 — Setup 提交按模式分支

- [x] 2.1 H2 install 写入可配置库名 JDBC URL（`./data/{name}`）
- [x] 2.2 `databaseMode=new`：MySQL CREATE + Flyway；H2 Flyway
- [x] 2.3 `databaseMode=existing`：跳过 Flyway 与 MySQL CREATE
- [x] 2.4 existing 模式 `createOrUpdateAdmin` 覆盖 `user_id=1` 用户名/密码

## 3. 后端 — 路由与升级触发调整

- [x] 3.1 `InstallConfigMigrationRunner`：无 install 时不再 `markPending()`
- [x] 3.2 `UpgradeService.isUpgradeRequired()`：无 install 时不因 legacy/schema 触发；install 完成后 pending 触发
- [x] 3.3 `SetupFilter` / `UpgradeFilter`：无 install 仅 setup 路径
- [x] 3.4 新增 `PostInstallFlywayRunner`：`install.completed + upgrade.skip + pending` → 静默 migrate
- [x] 3.5 静默 migrate 失败 `System.exit(1)`

## 4. 前端 — 安装向导

- [x] 4.1 数据库步骤增加 H2/MySQL 共用的「数据库名」字段（默认 `cat2bug_platform`）
- [x] 4.2 测试连接成功后展示 `new` / `existing` 提示文案
- [x] 4.3 Submit 携带 `databaseMode`
- [x] 4.4 `permission.js`：无 install 时不再因 `upgradeRequired` 跳 `/upgrade`

## 5. 测试与文档

- [x] 5.1 单元测试：`DatabaseExistenceProbe`、`SetupDatabaseTestService` existing/new 分支
- [x] 5.2 集成测试：existing H2 attach 跳过 migrate、admin 覆盖
- [x] 5.3 集成测试：install 完成后 pending → upgrade；skip → 静默 migrate / 失败 exit
- [x] 5.4 更新 `readme/production/faq.md` 与 `TESTING.md` 手工清单
- [x] 5.5 Docker 文档补充 `CAT2BUG_UPGRADE_SKIP` 静默 migrate 语义
