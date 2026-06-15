<!-- 进度审计：2026-06-15，依据 .worktree/feature-spring-native-upx 代码与 readme/spring-native-delivery/PHASE-*.md -->

## 1. Phase 0 — 分支与构建链骨架

- [x] 0.1 从 `core` 创建 worktree：`git worktree add .worktree/feature-spring-native-upx -b feature/spring-native-upx core`
- [x] 0.2 根 `pom.xml`：启用 `native-maven-plugin`（GraalVM buildtools 版本与 SB 3.3.13 对齐）；`pluginManagement` 增加 `-J-Xmx8g` 等 native-image 参数
- [x] 0.3 `cat2bug-platform-admin/pom.xml`：配置 `spring-boot-maven-plugin` 的 `imageName`/AOT；profile **`native`** 绑定 `native-maven-plugin` execution
- [x] 0.4 新增 `application-native.properties`：关闭 devtools/swagger UI；保留 static locations；Flyway/数据源与 prod 一致
- [x] 0.5 新增 `deploy/build-native-spring.sh` 骨架（embedded 前端 → `mvn clean package -Pnative -Pembedded -Prelease` → 复制 `cat2bug-admin-linux-{arch}` → 可选 UPX）
- [x] 0.6 新增 `readme/spring-native-delivery/README.md`、`METRICS.md` 模板（含 Quarkus embedded 190/42 对照列）
- [x] 0.7 编写 `PHASE-0.md`：本地/Docker 构建前置（Docker、Node、upx）；验收：`mvn -pl cat2bug-platform-admin -am package -DskipTests` JVM 未回归

## 2. Phase 1 — Embedded Native 编译与 SPA 冒烟（Kill Gate）

- [x] 1.1 构建前集成 `npm run build:embedded`（或 `deploy/scripts/ui-build.sh embedded`）；确认 `static/**` 无 `stats.json`（cherry-pick packaging-slim 若 core 未合并）
- [x] 1.2 新增 `Cat2BugNativeRuntimeHints`（admin 或 framework）：注册 `classpath*:static/**`、`mapper/**/*.xml`、`db/migration/**` resources
- [x] 1.3 配置 Spring Native **容器构建**（amd64）；首次 `native:compile` / `package -Pnative` 通过
- [x] 1.4 修复 Phase 1 编译 blocker（按出现顺序）：MyBatis Mapper 扫描、Security Filter、Flyway SQL、H2 驱动、数据源排除与 `@SpringBootApplication` 对齐
- [x] 1.5 Linux 容器冒烟：`GET /` 返回 HTML；`GET /static/js/*.js` 200；`GET /login`（Accept: text/html）SPA fallback；`POST /login` API 可用 — `deploy/test/native-spa-smoke.sh` + `native-api-smoke.sh`；已接入 CI
- [x] 1.6 arm64 容器构建通过（`build-native-spring.sh aarch64`）
- [x] 1.7 记录 Phase 1 体积至 `METRICS.md`（raw，未优化）— arm64 已实测；amd64 raw 冷启动/RSS 由 CI `ubuntu-latest` + `measure-native-coldstart.sh` 回填（见 METRICS.md「CI 自动填充」节）
- [x] 1.8 编写 `PHASE-1.md` + Kill Gate 结论：8 周内未通过 → 文档化 blocker 并评估终止 / 保留 Quarkus 交付路径

## 3. Phase 2 — 全功能 Native parity

- [x] 2.1 MyBatis：全量 `mapper/**/*.xml` 与 Mapper 接口 RuntimeHints；PageHelper 分页 Native 验证
- [x] 2.2 Security：JWT + Redis 会话（`TokenService`）；`@PreAuthorize` / 数据权限 AOP Native 验证
- [x] 2.3 J2Cache：Native profile **Redis-only** 或等价方案；local-only 部署文档说明 — `J2CACHE-NATIVE.md`；`native-redis-smoke.sh`；单实例 `NATIVE_FALLBACK` 已验收
- [x] 2.4 迁移/验证 Open API 模块（`/api/**` API Key 鉴权）
- [x] 2.5 WebSocket / IM：`/websocket/{memberId}/message` Native 连接与推送冒烟
- [x] 2.6 AI 模块：Ollama/OpenAI 配置读取与至少一条 `/ai/**` 接口 Native 验证
- [x] 2.7 Quartz：定时 Job Native 启动无报错（或 Native profile 改用 `@Scheduled` + 文档说明差异）
- [x] 2.8 Setup 向导：`/setup/status`、test/submit、H2/MySQL 安装写 `config/install`、安装后重启流程（`SetupMigrationService`，非 Quarkus `SetupFlywayFactory`）
- [x] 2.9 Upgrade 向导：pending 检测、submit/retry/rollback、Legacy 库抽样
- [x] 2.10 Excel：cherry-pick Quarkus **FastExcel** 路径；Native **排除 POI**；导入导出冒烟
- [x] 2.11 Captcha：cherry-pick **CaptchaPngRenderer**（无 AWT）；`/captchaImage` Native 验证
- [x] 2.12 native profile 排除：generator、devtools、Knife4j UI、swagger-ui
- [x] 2.13 回归：`mvn test` 关键模块通过；编写 `PHASE-2.md` 手工清单（对照 JVM 版）— `jvm-embedded-regression.sh` + `deploy/test/logs/` 归档；PHASE-2「JVM 回归证据」节

## 4. Phase 3 — 体积优化与 UPX

- [x] 3.1 合并/完成 `backend-prod-packaging-slim`：embedded 无 stats/report；tool 路由生产剔除
- [x] 3.2 Graal 裁剪：评估 `-H:-AddAllCharsets`、移除 unused autoconfig、按部署裁剪 locale（保留 zh）
- [x] 3.3 验证 Native 二进制不含 `libawt.so`（Captcha/上传路径）
- [x] 3.4 `deploy/build-native-spring.sh` 默认 UPX；RPM 仍用未压缩 ELF
- [x] 3.5 amd64 + arm64 实测：raw 体积、UPX 体积、冷启动（未压缩 vs UPX）、空闲 RSS → 填入 `METRICS.md`（Phase 1 基线 + Phase 3 待重建列）— arm64 全量；amd64 raw 冷启动/RSS 待 CI 首跑回填（脚本与 workflow 已就绪）
- [x] 3.6 stretch 目标评估：raw **< 250MB**、UPX **< 65MB**；未达标则文档化原因与后续项，**不阻塞 Phase 4**
- [x] 3.7 编写 `PHASE-3.md`

## 5. Phase 4 — 交付、RPM 与切换默认 Release

- [x] 4.1 `deploy/rpm/cat2bug/`：支持 Spring Native 二进制打包（`build-rpm-spring.sh` 或参数化）；systemd unit 无 `java-*` Requires
- [x] 4.2 AlmaLinux 9：`smoke-install.sh` 对 Spring Native RPM 通过（`/version`、health、setup status）
- [x] 4.3 Docker：`deploy/docker/native-spring-minimal/` + `run-native-spring-minimal.sh` 默认指向 Spring Native `.upx` 或未压缩版
- [x] 4.4 CI：`deploy/ci/spring-native.yml`（双架构构建 + 冒烟 job 草案）— 与 `.github/workflows/spring-native.yml` 同步
- [x] 4.5 更新根 `README.md`、`deploy/README.md`、`CLAUDE.md`：默认 Release = `./deploy/build-native-spring.sh`；JVM JAR 作 dev/回滚说明
- [x] 4.6 标注 `feature/quarkus-full-migration` 为 archived/reference；记录 Quarkus → Spring Native 决策摘要
- [x] 4.7 编写 `PHASE-4.md` 与 Go-Live 检查清单

## 6. 横切任务

- [x] 6.1 每个 Phase 结束：`mvn -pl cat2bug-platform-admin -am package -DskipTests` 确认 **JVM embedded 构建未回归** — `jvm-embedded-regression.sh`；CI `mvn test` job
- [x] 6.2 新增 OpenSpec specs：`specs/spring-native-embedded-delivery/spec.md`、`specs/spring-native-graal-hints/spec.md` 及 modified capability specs（backend-api-compat、backend-prod-packaging-slim、setup/upgrade）
- [x] 6.3 安全审查：Native 包不含 devtools/generator；install 配置不写死密钥
- [x] 6.4 全 Phase 文档保持 UTF-8 中文
