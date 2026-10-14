# MyBatis databaseId 误判导致 MySQL 执行 H2 语句

记录双库（H2 / MySQL）下统计 SQL 报错的原因。

- 日期：2026-06-16 ～ 2026-07-05
- 涉及：Spring 后端 / 数据库

## 现象

- MySQL 部署下仪表盘统计接口报 SQL 语法错误。
- 安装向导选择 MySQL 并提交后不重启继续使用，部分页面报错；重启后恢复。

## 根因

Mapper XML 里同一语句按 `databaseId`（`h2` / `mysql`）写两份。`databaseId` 在 MyBatis 初始化时确定：

- 检测逻辑没取到真实数据源类型，默认落到 H2 版本 SQL；
- 安装向导选 MySQL 后热切换数据源，但 MyBatis 已按 H2 初始化，继续执行 H2 方言 SQL。

## 解决办法

- `0be29d06`：DataSource 检测修正 `databaseId`。
- `3fda1078`：`MyBatisConfig` 优先根据 JDBC URL 与 `spring.database-type` 解析 `databaseId`；MySQL 安装后不再热切换，要求重启（`InstallRuntimeActivator`），并补 `MyBatisConfigDatabaseIdTest`、`InstallRuntimeActivatorTest`。
- 另：H2 下 `defect_ids` / `receive_by` 与 BIGINT 比较需 `CAST`（`cc50cbc3`）。

## 以后怎么避免

- 新增按 `databaseId` 区分的 SQL，H2 和 MySQL 两边都要测。
- 切换数据库类型一律走「写配置 + 重启」，不做运行期热切换。
