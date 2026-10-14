# 安装向导把预建空 MySQL 库判为 existing

Docker Compose 首次安装失败、提示「未检测到表结构」的原因。

- 日期：2026-10-14
- 涉及：后台 / 部署

## 现象

Docker Compose 起栈后在 `/setup` 选 MySQL 提交，安装失败，提示「未检测到表结构」。同样的流程用 H2 正常。

## 根因

Compose 的 MySQL 服务通过 `MYSQL_DATABASE` 预建了空库 `cat2bug_platform`。安装向导按「库是否存在」判断模式：库存在就判为 `existing`（附着旧库，安装阶段不跑迁移），随后检查表结构时发现是空库而报错。H2 那边早就按「有没有成功的 Flyway 迁移」判断，两条路径逻辑不一致。

## 解决办法

MySQL 改为与 H2 一致：库不存在，或存在但没有成功的 Flyway 迁移记录 → `new`，提交时执行 Flyway 建表；已有迁移记录 → `existing`。改动在 `DatabaseExistenceProbe.probeMysqlMode` 与 `SetupDatabaseTestService.testMysql`，回归测试 `SetupMigrationMysqlTest.emptyExistingDatabaseIsProbedAsNewUntilMigrated`（提交 `543c1085`）。

## 以后怎么避免

- 判断「是不是新库」以迁移记录为准，不以库或表是否存在为准；改 H2 / MySQL 任一边的探测逻辑，另一边同步。
- 部署方式改动（如 Compose 改为预建库）后，必须真实走一次 `/setup` 冒烟，见 [[安装向导预检与环境变量预填]]。
