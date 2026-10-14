# Native 镜像与操作日志对等

说明后端为对齐原 Spring 行为所做的两项横切实现：GraalVM Native 镜像的运行期初始化与反射注册（`NativeRuntimeInitFeature`），以及替代 Spring `@Log` 切面的操作日志规则表；引入新依赖、新增 domain/VO 或新增需要记日志的接口时参考。

## 概述

- **Native**：Quarkus 关闭了 GraalVM 自动 ServiceLoader 注册，且 MyBatis / Jackson / fastjson2 / JJWT 大量依赖反射。`NativeRuntimeInitFeature` 在构建期按**包前缀批量注册反射**，并把会在镜像堆中留下随机数、线程或构建机状态的类**推迟到运行期初始化**。
- **操作日志**：资源类上没有 `@Log` 注解；规则表 `oper-log-rules.tsv` 最初由原 Spring 控制器的 `@Log` 生成，`OperLogFilter` 按「HTTP 方法 + 路径模板」匹配后写 `sys_oper_log`，效果与原 Spring `LogAspect` 对等。Spring 控制器已删除，规则表现在需手工维护。

## 关键流程

### Native 构建

1. `--features=com.cat2bug.admin.nativeimage.NativeRuntimeInitFeature` 通过 `quarkus.native.additional-build-args` 注入（`cat2bug-platform-admin/pom.xml` 的 native profile 与 `application-native.properties` 各写了一份）。
2. `beforeAnalysis`：
   - 运行期初始化：Netty `PlatformDependent*`、JGit `WindowCache` / `WorkQueue` / `FileUtils` / `HttpAuthMethod$Digest`、Hutool `SmUtil` / `DefaultSSLInfo` / `HttpGlobalConfig` / `NanoId`、commons-compress、整个 `io.jsonwebtoken` 包等。
   - 整类反射（构造器/方法/字段）前缀：`com.cat2bug.common.`、`com.cat2bug.system.git.browse.domain.`、`com.cat2bug.system.mybatis.`、`com.cat2bug.admin.domain.`、`com.cat2bug.framework.web.PageOrder`（Mapper OGNL 静态调用，见 [[列表排序与注入防护]]）、`io.jsonwebtoken.impl.`、`io.jsonwebtoken.jackson.`。
   - `org.eclipse.jgit.` 下只注册枚举（JGit `Config` 反射调 `values()`）。
   - ServiceLoader：扫描 classpath 中 JJWT `Serializer` / `Deserializer` / `CompressionCodec` 的 `META-INF/services` provider 并注册反射；对应资源由 `quarkus.native.resources.includes` 打入镜像。
   - 方法替换：第三方库引用了项目没有的可选依赖时，在 `nativeimage/` 下用 `@TargetClass` / `@Substitute` 切断该路径。现有 `Target_CommonsCompressZipFile`（commons-compress 1.28 的 zstd 入口改为抛 `IOException`），见 [[Quarkus3.27下commons-compress引用zstd导致Native构建失败]]。
3. 打包：`./deploy/build-native.sh arm64|amd64`，默认产出原始 ELF 与 UPX 压缩版；冒烟用 `cat2bug-platform-admin/smoke-native-docker.sh`。

### 操作日志

1. `OperLogRules` 启动时加载 classpath `oper-log-rules.tsv`（列：HTTP 方法、路径模板、标题、业务类型、是否记请求、是否记响应、排除参数），路径参数统一归一为 `{}`。
2. `OperLogFilter`（请求/响应过滤器 + `ReaderInterceptor`）对命中的资源方法记录：请求参数（`password`、`oldPassword`、`newPassword`、`confirmPassword` 等字段脱敏或排除）、响应 JSON、耗时、操作人、IP；HTTP 状态 ≥400 记为失败；异步写库。

## 关键代码位置

- `cat2bug-platform-admin/src/main/java/com/cat2bug/admin/nativeimage/NativeRuntimeInitFeature.java`
- `cat2bug-platform-admin/src/main/resources/application.properties`、`application-native.properties`（`quarkus.native.*`）
- `cat2bug-platform-admin/pom.xml`（native profile）
- `deploy/build-native.sh`、`cat2bug-platform-admin/smoke-native-docker.sh`
- `cat2bug-platform-admin/src/main/java/com/cat2bug/admin/log/OperLogRules.java`、`OperLogFilter.java`
- `cat2bug-platform-admin/src/main/resources/oper-log-rules.tsv`
- 交付记录：`openspec/changes/archive/2026-10-14-restore-quarkus-full-delivery/NATIVE-RELEASE.md`、`GATE-NOTES.md`

## 注意事项

- 新增需要 JSON 序列化或作为 MyBatis `resultType` 的类，放在已注册前缀的包内；放在别处会在 Native 下出现字段丢失或反射失败，而 JVM 模式一切正常。
- 新依赖若在静态初始化里创建 `Random` / `SecureRandom` / 线程，需加到运行期初始化列表；`pom.xml` 与 `application-native.properties` 的 `--initialize-at-run-time` 列表要同步（两者当前不完全一致，例如 `sun.java2d.Disposer` 只出现在后者）。
- 新增需记日志的接口：在 `oper-log-rules.tsv` 手工补一行（路径模板与资源 `@Path` 拼接后一致），否则不会记录。
- Native 体积口径以 UPX 后计，2026-10-14 首次交付实测 arm64 ≈38.8 MiB（产品负责人已接受），详见 `NATIVE-RELEASE.md`；模块与包名改名后的重建体积见 [[2026-10]]。
- 包名已统一为 `com.cat2bug.*`，上述反射前缀与 `--features` 类名随之更新；以后改包结构时必须同步 `NativeRuntimeInitFeature` 前缀与 `application-native.properties`，否则只在 Native 运行期暴露 ClassNotFound / 反射失败。
- Native 进程以**工作目录作为数据目录**（`config/install`、`data`、`upload`、`git`），需整体持久化。

## 相关规格

- `openspec/specs/quarkus-backend-parity/spec.md`、`openspec/specs/quarkus-native-slim-binary/spec.md`（归档变更 `openspec/changes/archive/2026-10-14-restore-quarkus-full-delivery/`）
- `openspec/specs/spring-native-graal-hints/spec.md`、`openspec/specs/spring-native-embedded-delivery/spec.md`：Spring Native 对照线，变更 `retire-spring-and-unify-modules` 归档后移除
- `openspec/specs/backend-prod-packaging-slim/spec.md`

## 待确认

- `oper-log-rules.tsv` 最初的生成脚本/工具位置（`tasks.md` 只写了「按 Spring `@Log` 生成」；Spring 控制器删除后已无法再生成）。