# MySQL 迁移脚本 DELETE 不能子查询同表

记录 Flyway 脚本在 H2 通过、MySQL 失败的典型写法差异。

- 日期：2026-07-04
- 涉及：数据库

## 现象

`V0_6_2_17__remove_legacy_feishu_menu_duplicate.sql`（清理重复飞书菜单）在 H2 上执行正常，在 MySQL 上迁移失败，升级卡住。

## 根因

MySQL 不允许 `DELETE FROM sys_menu ... WHERE ... (SELECT ... FROM sys_menu ...)` 在子查询里引用正在删除的同一张表（错误 1093：You can't specify target table for update in FROM clause）。H2 没有这个限制。

## 解决办法

提交 `7730ee34`：把同表子查询包进派生表 `(SELECT ... FROM sys_menu) AS t` 再引用，并合并删除语句。

## 以后怎么避免

- `db/mysql` 下的 UPDATE / DELETE 脚本，凡子查询引用目标表，一律用派生表包一层。
- H2 与 MySQL 两套脚本都要真正在对应库上跑一遍（Quarkus 测试库为 MySQL `cat2bug_platform_test`，可覆盖 MySQL 侧）。
