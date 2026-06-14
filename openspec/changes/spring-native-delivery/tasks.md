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
- [x] 1.3 首次 Native 编译通过（本机 GraalVM；`repackage.classifier=exec` + `MyBatisNativeConfiguration`）；Linux 容器构建待验证
- [x] 1.4 修复 Phase 1 运行时 blocker：MapperFactoryBean 构造参数 Class vs String（进行中）；Security/Flyway/H2 数据源 — 2026-06-14：`MyBatisConfig` AOT `databaseId=h2`；Native 启动与 H2 SQL 冒烟通过
- [x] 1.5 Linux 容器冒烟：`GET /` 返回 HTML；`GET /static/js/*.js` 200；`GET /login`（Accept: text/html）SPA fallback；`POST /login` API 可用 — 2026-06-14：embedded arm64 容器 `/`、`/static/js/app.js`、登录 API 均 200
- [x] 1.6 arm64 容器构建通过（`build-native-spring.sh aarch64`）— 2026-06-14：`cat2bug-admin-linux-arm64` 338MB raw / 82MB UPX
- [x] 1.7 记录 Phase 1 体积至 `METRICS.md`（raw，未优化）— 双架构 raw/UPX 已填；冷启动 ~9.9s / RSS ~803MiB（amd64 UPX，2026-06-14）
- [x] 1.8 编写 `PHASE-1.md` + Kill Gate 结论 — **通过**（arm64 embedded + amd64 API-only + SPA/API/H2 冒烟）

## 3. Phase 2 — 全功能 Native parity

- [ ] 2.1 MyBatis：全量 `mapper/**/*.xml` 与 Mapper 接口 RuntimeHints；PageHelper 分页 Native 验证
- [ ] 2.2 Security：JWT + Redis 会话（`TokenService`）；`@PreAuthorize` / 数据权限 AOP Native 验证
- [ ] 2.3 J2Cache：Native profile **Redis-only** 或等价方案；local-only 部署文档说明
- [ ] 2.4 迁移/验证 Open API 模块（`/api/**` API Key 鉴权）
- [ ] 2.5 WebSocket / IM：`/websocket/{memberId}/message` Native 连接与推送冒烟
- [ ] 2.6 AI 模块：Ollama/OpenAI 配置读取与至少一条 `/ai/**` 接口 Native 验证
- [ ] 2.7 Quartz：定时 Job Native 启动无报错（或 Native profile 改用 `@Scheduled` + 文档说明差异）
- [ ] 2.8 Setup 向导：`/setup/status`、test/submit、H2/MySQL 安装写 `config/install`、安装后重启流程 — 2026-06-14：`GET /setup/status` L5 冒烟通过；完整安装流程待验
- [ ] 2.9 Upgrade 向导：pending 检测、submit/retry/rollback、Legacy 库抽样
- [ ] 2.10 Excel：cherry-pick Quarkus **FastExcel** 路径；Native **排除 POI**；导入导出冒烟 — **部分完成（2026-06-14）**：SPI + 业务服务已移植；P0 Controller 已接线；FastExcel 单测 2 项通过；`deploy/test/native-excel-smoke.sh` **5/5 通过**（arm64 Native @2020）；修复 H2 `SysDashboardMapper` FORMATDATETIME；Native 构建需 admin 显式 POI compile（process-aot）+ `--initialize-at-build-time=com.fasterxml.aalto`；**POI 仍可达**（binary strings ~60 处，待 Phase 2 后续剔除 monitor/ExcelUtil 路径）
- [ ] 2.11 Captcha：cherry-pick **CaptchaPngRenderer**（无 AWT）；`/captchaImage` Native 验证 — 2026-06-14：`GET /captchaImage` L1/L5 冒烟通过；`libawt.so` 剔除待验
- [ ] 2.12 native profile 排除：generator、devtools、Knife4j UI、swagger-ui
- [ ] 2.13 回归：`mvn test` 关键模块通过；编写 `PHASE-2.md` 手工清单（对照 JVM 版）

## 4. Phase 3 — 体积优化与 UPX

- [x] 3.1 合并/完成 `backend-prod-packaging-slim`：embedded 无 stats/report；tool 路由生产剔除 — 见 `PHASE-3.md`
- [x] 3.2 Graal 裁剪：评估 `-H:-AddAllCharsets`、移除 unused autoconfig、按部署裁剪 locale（保留 zh）— 见 `PHASE-3.md`
- [ ] 3.3 验证 Native 二进制不含 `libawt.so`（Captcha/上传路径）— `target/libawt.so` 仍存在
- [x] 3.4 `deploy/build-native-spring.sh` 默认 UPX；RPM 仍用未压缩 ELF — `UPX_COMPRESS` 默认 true；`run-native-spring-minimal.sh` 优先 `.upx`
- [ ] 3.5 amd64 + arm64 实测：raw 体积、UPX 体积、冷启动（未压缩 vs UPX）、空闲 RSS → 填入 `METRICS.md` — 双架构体积 + amd64 冷启动/RSS 已填；raw 冷启动待补
- [x] 3.6 stretch 目标评估：raw **< 250MB**、UPX **< 65MB**；未达标则文档化原因与后续项，**不阻塞 Phase 4** — 338MB/82MB，未达标已文档化
- [x] 3.7 编写 `PHASE-3.md`

## 5. Phase 4 — 交付、RPM 与切换默认 Release

- [ ] 4.1 `deploy/rpm/cat2bug/`：支持 Spring Native 二进制打包（`build-rpm-spring.sh` 或参数化）；systemd unit 无 `java-*` Requires — **进行中**：骨架已建（2026-06-14）；AlmaLinux 实机冒烟待验
- [ ] 4.2 AlmaLinux 9：`smoke-install.sh` 对 Spring Native RPM 通过（`/version`、health、setup status）— 脚本已建；待 RPM 实机构建验证
- [ ] 4.3 Docker：`deploy/docker/native-minimal/` + `run-native-minimal.sh` 默认指向 Spring Native `.upx` 或未压缩版 — **基本完成**：别名脚本 + symlink 已建；见 `deploy/README.md`
- [ ] 4.4 CI：`deploy/ci/spring-native.yml`（双架构构建 + 冒烟 job 草案）— **进行中**：`deploy/ci/spring-native.yml` 草案已建；CI 实跑待验
- [ ] 4.5 更新根 `README.md`、`deploy/README.md`、`CLAUDE.md`：默认 Release = `./deploy/build-native-spring.sh`；JVM JAR 作 dev/回滚说明 — **进行中**：`deploy/README.md` 已建
- [ ] 4.6 标注 `feature/quarkus-full-migration` 为 archived/reference；记录 Quarkus → Spring Native 决策摘要
- [ ] 4.7 编写 `PHASE-4.md` 与 Go-Live 检查清单 — **进行中**：`PHASE-4.md` 草案 + Go-Live 清单已建

## 6. 横切任务

- [x] 6.1 每个 Phase 结束：`mvn -pl cat2bug-platform-admin -am package -DskipTests` 确认 **JVM embedded 构建未回归** — 2026-06-14：`h2-mapper-smoke.sh` + Maven package 通过
- [x] 6.2 新增 OpenSpec specs：`specs/spring-native-embedded-delivery/spec.md`、`specs/spring-native-graal-hints/spec.md` 及 modified capability specs（backend-api-compat、backend-prod-packaging-slim、setup/upgrade）
- [ ] 6.3 安全审查：Native 包不含 devtools/generator；install 配置不写死密钥
- [ ] 6.4 全 Phase 文档保持 UTF-8 中文
