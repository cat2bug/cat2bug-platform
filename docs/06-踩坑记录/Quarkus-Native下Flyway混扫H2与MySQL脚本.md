# Quarkus Native 下 Flyway 混扫 H2 与 MySQL 脚本

记录 Quarkus Native 全新安装时迁移出错的原因。

- 日期：2026-10-14
- 涉及：Native / Quarkus 后端 / 数据库

## 现象

Quarkus Native 二进制全新安装（`/setup/submit`）时 Flyway 迁移失败，或 MySQL 库里执行了 H2 方言脚本（反之亦然）；JVM 模式正常。

## 根因

- Native 下 Flyway 的 classpath Scanner 被 Quarkus 替换，无法在运行时扫描目录，只能用构建期索引的迁移文件。
- 构建期索引按路径**前缀**匹配。H2 脚本在 `db/migration/h2`、MySQL 脚本在 `db/migration/mysql`，同处 `db/migration` 前缀下，两套脚本被混在一起。

## 解决办法

见 `openspec/changes/archive/2026-10-14-restore-quarkus-full-delivery/NATIVE-RELEASE.md`「Native 适配要点」与 `tasks.md` 7.5：

- `SetupMigrationService` 在 Native 下改用 Quarkus 构建期索引的迁移文件；
- MySQL 脚本目录移到 `db/mysql`（与 `db/migration` 不同前缀），`SetupMigrationService.migrationLocation` 按数据库产品名选择目录。

## 以后怎么避免

- Quarkus 线新增 MySQL 迁移放 `db/mysql`，不要放回 `db/migration/mysql`。
- 两套迁移目录名不能互为前缀。
- Native 冒烟必须跑一次 `FRESH=true ./deploy/smoke-native-docker.sh` 全新安装。
