# Quarkus 热加载后 Flyway 新 SQL 无法打开 inputstream

- 日期：2026-11-04
- 涉及：后台 / 部署

## 现象

历史版本升级向导在「执行升级」失败，报错类似：

`Unable to obtain inputstream for resource: db/mysql/V1_2_1_15__xxx.sql`

源码与 `target/classes` 里文件都在；日志里 `Flyway pending 检测失败` 也常刷同一句。刚往 `db/mysql`（或 `db/migration`）加了新脚本、尚未整进程重启时最容易出现。

## 根因

Quarkus:dev 的 ClassLoader 对**启动后新拷进 classpath 的资源**经常是：目录枚举能扫到文件名，但 `getResourceAsStream` 仍返回 null。Flyway 按扫描结果去打开流就会失败。Native 另走构建期索引，不受此热加载问题影响。

## 解决办法

1. **立刻恢复**：整进程重启 `cat2bug-platform-admin/dev.sh`（不要只靠热部署），再点升级向导「重试升级」。
2. **代码侧**：`SetupMigrationService.buildFlyway` 在 JVM 下把迁移目录解析为 `filesystem:`（优先 ClassLoader 的 `file:` URL，否则回退模块 `src/main/resources/...`），避免走坏掉的 classpath 流。

## 以后怎么避免

- 新增 Flyway 脚本后若升级/pending 报 `Unable to obtain inputstream`，先重启后台再试。
- 不要只改 SQL 就指望升级向导在不重启的 dev 进程里读到新文件（除非已走 filesystem 回退）。
