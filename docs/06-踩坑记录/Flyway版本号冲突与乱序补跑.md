# Flyway 版本号冲突与乱序补跑

记录多分支并行加迁移脚本时，升级检测失效、脚本没执行的问题。

- 日期：2026-06-03 ～ 2026-07-10
- 涉及：数据库 / Spring 后端 / Quarkus 后端

## 现象

1. 出现两个 `V0_6_2_9__*.sql`，Flyway 校验报版本重复，升级向导检测不到待升级。
2. 后插入的低版本脚本（如 `V0_6_2_17`）在已升级到更高版本的库上不执行，菜单清理等数据修正缺失。
3. 早期升级过滤器在请求热路径上反复初始化 Flyway，连接池被耗尽。

## 根因

- 两个分支各自新建了同版本号脚本。
- Flyway 默认不执行低于当前已应用最高版本的新脚本（`outOfOrder=false`）。
- `UpgradeFilter` 每个请求都 new Flyway 做检测。

## 解决办法

- `2a0febad`：合并为 `V0_6_2_9__feishu_menu_and_defect_custom_fields.sql`，新增 `V0_6_2_10` 兼容只跑过飞书菜单的旧库。
- `79aa3814`：Flyway 开启 `outOfOrder`，补跑后插入的 legacy 脚本（`application.yml`、`SetupMigrationService`）；Quarkus 测试配置同样 `out-of-order=true`。
- `4f11fa75`：修复 `UpgradeFilter` 热路径重复初始化 Flyway，并升级 Flyway 至 10.10。

## 以后怎么避免

- 新建迁移脚本前先拉最新代码，按当前最大版本号 +1 命名。
- 已发布的脚本不改内容、不复用版本号；修正用新脚本。
- 开启 `outOfOrder` 后，乱序脚本必须可重复/幂等，不能依赖后续脚本的结构。
- 迁移检测只在启动/向导时做，不放在请求过滤链上。
- 取「当前最大版本号」时也要看**未合并 / 已回滚的分支**：2026-10 回滚前的备份分支 `backup/core-before-rollback-20261014` 用过 1.1.0.12～39，开发库执行过，`core` 上新脚本若从 12 起会与库里已有记录（同版本不同脚本）冲突。因此 `personal-access-token-oauth` 的脚本直接用 `V1_1_0_40`，此后从 41 起编号。查法：`git log --all --name-only -- '**/db/migration/V1_1_0_*'`，以及开发库 `sys_db_version` 的最大版本。
- 已在库里执行过的脚本连注释也不要改：Flyway 校验和覆盖整个文件，改注释同样会报校验和不一致。
