# 最新迁移脚本须可重复执行：MySQL 加列没有 IF NOT EXISTS

- 日期：2026-10-15
- 涉及：后台 / 数据库迁移 / 测试

## 现象

新增 `V1_1_0_41` 后全量测试：`QuarkusUpgradePendingMigrationTest` 报 `Duplicate column name 'oper_channel'`，随后其余 160 个测试因 Flyway 校验失败被跳过。

## 根因

- 该测试会删除 `sys_db_version` 里**最新一条**成功记录，验证升级页能提示待执行迁移，结束时再执行一次迁移恢复。最新脚本因此会在已有表结构上再跑一遍。
- MySQL 8 的 `ALTER TABLE ... ADD COLUMN` 不支持 `IF NOT EXISTS`（H2 支持），第二次执行必然失败；DDL 不在事务里，失败后历史表留下 `success=0` 的记录，后续启动全部校验失败。

## 解决办法

MySQL 脚本里的加列 / 加索引改用 `information_schema` 判断后动态执行：

```sql
SET @db := DATABASE();
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_oper_log' AND COLUMN_NAME = 'oper_channel');
SET @q := IF(@exists = 0, 'ALTER TABLE sys_oper_log ADD COLUMN oper_channel VARCHAR(16) NULL', 'SELECT 1');
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;
```

测试库已留下失败记录时，删掉该版本的历史行再跑：`DELETE FROM sys_db_version WHERE version='1.1.0.41'`（只在测试库 `cat2bug_platform_test` 做）。

## 以后怎么避免

- 新迁移脚本一律写成可重复执行：建表用 `IF NOT EXISTS`，MySQL 加列 / 索引用上面的模板（参考 `V0_6_2__robot_agent_system.sql`、`V1_1_0_41__mcp_server.sql`）。
- 写完先跑 `QuarkusUpgradePendingMigrationTest`。
- 相关：[[Flyway版本号冲突与乱序补跑]]、[[数据库]]。
