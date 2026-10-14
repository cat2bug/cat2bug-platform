# Quarkus 测试连到开发库

记录 Quarkus 测试误写开发数据库的原因与防护。

- 日期：2026-10-14
- 涉及：Quarkus 后端 / 数据库

## 现象

跑 `mvn -Pquarkus ... test` 后，本地开发库 `cat2bug_platform` 被写入大量测试数据；或测试因开发库的历史数据（排序规则、乱序迁移记录）而失败。

## 根因

- `dev.sh` / `run-quarkus-jar.sh` 会 `export QUARKUS_CONFIG_LOCATIONS` 指向 `config/install/application-install.yml` 与 `quarkus-runtime.properties`；`InstallStartupSupport` 也会读环境变量 `CAT2BUG_INSTALL_CONFIG_PATH`。
- 在同一个 shell 里接着跑测试，这两个环境变量会覆盖测试配置，测试就连上了开发库。
- 早期测试配置本身也指向开发库（`tasks.md` 7.8 之前）。

## 解决办法

- 测试配置改用独立库 `cat2bug_platform_test`（`cat2bug-platform-quarkus-admin/src/test/resources/application.properties`，`createDatabaseIfNotExist=true`，启动自动迁移）。
- 跑测试前清掉环境变量：

```bash
unset QUARKUS_CONFIG_LOCATIONS CAT2BUG_INSTALL_CONFIG_PATH
mvn -pl cat2bug-platform-admin -am test
```

2026-10-14 模块改名后（见 [[ADR-010-移除Spring后端并统一模块命名]]），测试配置位于 `cat2bug-platform-admin/src/test/resources/application.properties`，运行脚本为 `dev.sh` / `deploy/run-jar.sh`。

## 以后怎么避免

- 跑测试前确认当前 shell 没有上述两个变量（`env | grep -E 'QUARKUS_CONFIG|CAT2BUG_INSTALL'`）。
- 不要在跑过 `dev.sh` 的终端里直接跑测试。
- 待确认：是否在测试启动时检测并拒绝连接 `cat2bug_platform` 库，做成硬防护。
