## Why

Cat2Bug 当前生产交付为 **Spring Boot 3.3 + Java 17** fat JAR（embedded 约 **94–148MB**），依赖 **JRE** 分发，冷启动与空闲内存偏高。`quarkus-migration-spike` 与 `quarkus-full-migration` 已验证 GraalVM Native 在本项目技术栈下可行（embedded Native **~190MB**、UPX 分发 **~42MB**），但 Quarkus 全量迁移需重写 Controller/Security/CDI，估 **12–24 人月**。

产品侧希望 **最终交付物为单个 Native 可执行文件**：含 **embedded Vue SPA**（`classpath:/static/**`），用户下载后 **无需 JRE、无需 nginx**，即可运行完整 Cat2Bug（API + 前端 + 安装/升级向导）。在 **不重写业务 API** 的前提下，**Spring Boot 3 GraalVM Native（AOT）** 是更直接的路线：复用现有 `cat2bug-platform-admin` 全模块（framework/system/im/ai/quartz/api）与 122 个 MyBatis 映射，仅补齐 Native 构建链、RuntimeHints 与体积优化。

本 change 从 **`core` 分支** 独立 worktree（`feature/spring-native-upx`）实施，与 **`feature/quarkus-full-migration` 并行**：Quarkus 线继续作为对照组与技术试验田（FastExcel、Captcha 去 AWT、UPX/RPM 脚本等可 cherry-pick），**Spring Native embedded 验收通过后切换默认 Release 产物**。

## What Changes

- 在 **`core`（Spring Boot 3.3.13）** 上启用 **Spring Native** 构建：`spring-boot-maven-plugin` + `native-maven-plugin`（GraalVM/Mandrel **JDK 21** 容器构建），产出 **linux/amd64** 与 **linux/arm64** 双架构 ELF
- **默认交付形态：embedded 单文件 Native**——构建前 `npm run build:embedded`，将 Vue 产物打入 `cat2bug-platform-admin/src/main/resources/static/**`，Native 二进制内嵌 SPA + `static/docs/**`（`/system/doc` 依赖）
- **全量 `cat2bug-platform-admin` 范围**：含 im/ai/quartz/api/generator（generator 在 native profile 中 **provided/排除**，与 prod 语义一致）、Setup/Upgrade 向导、Excel 导入导出、WebSocket
- 新增 Maven profile **`native`** / **`embedded-native`**：与现有 JVM 构建并行；开发日常仍用 `mvn spring-boot:run` + `npm run dev`，Release 走 Native 流水线
- 新增 **`deploy/build-native-spring.sh`**（及 CI job 草案）：embedded 前端 → Native 容器构建 → 可选 **UPX**（`--best --lzma`）分发包；RPM 使用 **未压缩 ELF**
- 分 **4 个 Phase** 实施：构建链与 embedded 编译过关 → 全功能 parity → 体积优化 → 交付切换（RPM/systemd、AlmaLinux 冒烟、文档）
- **体积为优化目标（非硬卡）**：记录 `readme/spring-native-delivery/METRICS.md`；stretch 目标参考 Quarkus embedded（raw **< 250MB**、UPX **< 65MB**）；Quarkus 实测 embedded **190MB / 42MB UPX** 为对照基线
- **Phase 1 Kill Gate**：8 周内 embedded Native 无法 `native:compile` 或未通过 SPA 冒烟 → 评估终止本路线、保留 Quarkus 为交付路径
- **不**在本 change 初期：删除 Spring JVM 构建、Vue 前端大改、微服务拆分；**不**强制立即归档 Quarkus 分支

## Capabilities

### New Capabilities

- `spring-native-embedded-delivery`：Spring Boot 3 AOT/Native 构建链、embedded SPA 资源纳入 native-image、`EmbeddedSpaFallbackFilter` 与静态资源 RuntimeHints、双架构 ELF + 可选 UPX + RPM/systemd 交付
- `spring-native-graal-hints`：MyBatis Mapper XML、Security Filter 链、J2Cache、WebSocket/STOMP、Quartz、Flyway、POI/FastExcel、Captcha（无 AWT）等组件的 GraalVM reachability 配置与 native profile 依赖裁剪

### Modified Capabilities

- `backend-api-compat`：运行时实现仍为 Spring Boot 3 MVC，**对外 API/响应/WebSocket/静态路径契约 MUST 不变**；补充 **Native embedded 单文件部署** 下的行为要求（`GET /`、`GET /login` SPA fallback、静态资源 200）
- `backend-prod-packaging-slim`：embedded 前端构建须剔除 `stats.json` 等冗余；native profile 排除 Knife4j UI、devtools、generator 运行时依赖；与 Native 体积优化对齐
- `first-run-setup-wizard`：安装向导在 Native 二进制下行为与 JVM 版一致（H2/MySQL、Flyway、`config/install` 写入）
- `legacy-upgrade-wizard`：升级向导在 Native 二进制下状态机与全锁行为不变

## Impact

- **代码**：`cat2bug-platform-admin` 及 framework/system/common/im/ai/quartz 的 **RuntimeHints / native profile / reflect-config**；根 `pom.xml` 启用 `native-maven-plugin`；可能 cherry-pick Quarkus 线的 **FastExcel 替代 POI**、**CaptchaPngRenderer 去 AWT**
- **分支**：从 `core` 创建 `feature/spring-native-upx`（git worktree）；与 `feature/quarkus-full-migration` 并行，禁止互相 merge 整分支
- **API**：路径、Header、JSON 形态不变；无 Controller 重写
- **前端**：继续使用 Vue2；Release 构建 `build:embedded`；开发模式不变
- **部署**：新增单文件 Native 为主发布物；Docker `native-minimal` 可复用 Quarkus 脚本思路；RPM 仍用未压缩 ELF
- **构建环境**：Native 构建需 Docker + **8GB+ 堆**（embedded static ~70MB）；首次构建约 **20–40 分钟**
- **风险**：Spring Native 全量编译难度高（MyBatis 122 文件、IM WebSocket、J2Cache、Quartz）；Spring 框架 AOT 产物通常 **大于** Quarkus 同等功能；UPX + embedded 冷启动慢于未压缩版
- **与 Quarkus 关系**：Quarkus 线完成 Phase 5 后 **冻结为对照/archive**；Spring Native 线 cherry-pick 通用优化（Excel/Captcha/packaging/UPX 脚本），**不** port JAX-RS 重写
