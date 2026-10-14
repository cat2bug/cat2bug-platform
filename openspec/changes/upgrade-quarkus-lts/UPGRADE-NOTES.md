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
- 新增 `cat2bug-platform-admin/smoke-upgrade-jvm.sh`：用 3.15.7 单 JAR 走安装向导并登录，再换 3.27.6 单 JAR 在同一目录启动、登录，比对用户 / 角色 / 字典 / 项目数量，并确认运行期配置未被改写。H2 与 MySQL 两种均通过；新旧都用 3.27.6 JAR 时（即新版全新安装）也通过。

## MCP 扩展引入验证（已移除）

- 临时加入 `quarkus-mcp-server-http` 1.9.1 与一个 `ping` 工具。
- JVM：`initialize`、`notifications/initialized`、`tools/list`、`tools/call` 均正常；原有 `/login` 与 `/system/**` 未登录返回 JSON `code` 401 不变。
- Native（arm64）：可构建、可启动，容器内同样完成上述 MCP 调用，日志无错误。
- 注意：`/mcp` 是 Vert.x 路由，不经过现有的 JAX-RS 鉴权过滤器，引入后默认不鉴权，由 `personal-access-token-oauth` / `mcp-server` 负责加上令牌校验。
- 验证完成后已移除依赖与工具代码。

## Native

- 首次构建失败：Quarkus 3.27 BOM 把 commons-compress 从 1.27.0 升到 1.28.0，其 `ZipFile.createZstdInputStream` 直接引用 zstd-jni，项目没有该库，构建期链接报 `NoClassDefFoundError: com/github/luben/zstd/ZstdInputStream`。新增 `nativeimage/Target_CommonsCompressZipFile` 把该方法替换为抛 `IOException`（项目不读取 zstd 压缩的 zip 条目），未固定旧版本库。
- 构建镜像沿用 `ubi-quarkus-mandrel-builder-image:jdk-21`，无需修改。
- arm64 体积：UPX 前 189,342,720 字节（+4.6%），UPX 后 43,036,488 字节（约 41.0 MiB，+3.6%），未超过 10% 评估线。
- `smoke-native-docker.sh arm64` 默认模式（沿用 3.15 版 Native 冒烟留下的 H2 安装数据）与 `FRESH=true` 均通过。

## 交付回归

- Docker Compose（MySQL + Redis）：安装向导、登录、监控页正常；重建后端容器后仍可登录，日志无 ERROR。
- Native 容器浏览器回归（全新 H2）：建团队与两个项目并切换；缺陷新建 → 指派 → 修复 → 驳回 → 重新打开 → 再修复 → 通过，状态依次 0/1/3/0/1/4，评论成功，生成 7 条通知；用例、报告页面正常；建仓 `regress-repo` 后 `git clone`、`git push`、重新克隆正确，错误密码被拒；在线浏览文件树、README、提交、贡献者正常；WebSocket `/websocket/1/message` 连接成功。

## 回归中发现的既有问题（与本次升级无关）

- 缺陷接口不返回 `defectStateName` / `defectTypeName`，列表状态标签显示「未知」、类型图标空白。Spring 线在 `SysDefectMapper.xml` 中用 typeHandler 映射这两个字段，迁到 Quarkus 时遗漏；3.15 版同样存在，需另开变更修复。
- 首页致谢头像使用相对路径，在 `/{team}/{project}/code/...` 下加载为 404，属前端既有问题。
