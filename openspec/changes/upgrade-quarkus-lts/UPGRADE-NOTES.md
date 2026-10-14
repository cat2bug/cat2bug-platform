# 升级记录

## 基线（升级前，tag `quarkus-3.15-final`）

- 全量测试：215 个通过，跳过 2 个（MySQL 真库测试默认跳过）。
- arm64 Native：UPX 前 180,954,096 字节，UPX 后 41,535,128 字节；`smoke-native-docker.sh arm64` 默认模式与 `FRESH=true` 均通过（见 `docs/08-版本记录/2026-10.md`）。

## 目标版本

| 组件 | 旧 | 新 | 依据 |
| --- | --- | --- | --- |
| Quarkus 平台 | 3.15.7 | 3.27.6 | 3.27 LTS 最新补丁 |
| quarkus-mybatis | 2.3.0 | 2.4.1 | 按 Quarkus 3.24.2 构建；2.4.2 按 3.29.2 构建，超出 3.27 |
| quarkus-mcp-server-http | — | 1.9.1（仅验证） | 按 Quarkus 3.27.2 构建 |

两个版本属性统一放在根 `pom.xml`，子模块不再重复定义。

## 迁移指南核对（3.16～3.27）

| 版本 | 变化 | 对本项目 |
| --- | --- | --- |
| 3.16 | 删除 RESTEasy Reactive 旧名兼容 | 已用 `quarkus-rest`，无影响 |
| 3.17 | 停用或无 URL 的数据源被静态注入时启动失败 | `InstallEffectiveDataSource` 通过 `DataSources` 动态取池，无影响；安装后 `quarkus.datasource.active=false` 的旧配置启动验证通过 |
| 3.17 | 调度器未启动时方法抛异常 | `quarkus.scheduler.start-mode=forced`，无影响 |
| 3.17 | Native 默认 locale 改为 `en_US` | 国际化走资源包与 `Accept-Language`，Native 冒烟验证 |
| 3.18 | 空的 String 查询参数（`?x=`）由 `""` 改为 `null` | 扫描 154 处 String 查询参数，只有 `/system/user/profile/updatePwd` 会空指针，已补判空；MyBatis 条件均同时判断 `null` |
| 3.18 | Flyway 11 | 迁移脚本与 `sys_db_version` 表不变，测试通过 |
| 3.19 | Native 构建镜像默认 UBI 9 | `build-native.sh` 显式指定构建镜像，见第 4 组 |
| 3.22 | BOM 不再管理 JGit | 根 pom 自管 `jgit.version`，无影响 |
| 3.26 | 扩展须用 `@ConfigMapping` | quarkus-mybatis 2.4.1 构建与运行正常 |

## 配置项

- `quarkus.mybatis.mapper-packages`：2.3.0 与 2.4.1 都没有这个键，一直被忽略（Mapper 靠 `@Mapper` 发现），已删除。
- `quarkus.datasource.jdbc.enable-metrics`：3.27 标记废弃（将来删除，官方建议改用 micrometer）。不装 micrometer 时它仍是打开 Agroal 连接池计数的唯一开关，「数据监控」页依赖这些计数，暂时保留。
- 安装向导写出的运行期配置（`quarkus-runtime.properties`）里的键在 3.27 全部仍被识别，无需兼容映射。

## 验证

- 全量测试：215 个通过，跳过 2 个，与基线一致；测试日志中除 `enable-metrics` 废弃提示外没有新增警告。
- 新增 `cat2bug-platform-admin/smoke-upgrade-jvm.sh`：用 3.15.7 单 JAR 走安装向导并登录，再换 3.27.6 单 JAR 在同一目录启动、登录，比对用户 / 角色 / 字典 / 项目数量，并确认运行期配置未被改写。H2 与 MySQL 两种均通过。
