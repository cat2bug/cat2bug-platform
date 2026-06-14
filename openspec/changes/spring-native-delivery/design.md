## Context

Cat2Bug 当前为 **Java 17 + Spring Boot 3.3.13** 多模块单体（`core` 分支）：

| 模块 | 职责 |
|------|------|
| `cat2bug-platform-admin` | 启动入口、`classpath:/static/**` embedded |
| `cat2bug-platform-framework` | Security、`EmbeddedSpaFallbackFilter`、AOP、Token |
| `cat2bug-platform-system` | 核心业务与 MyBatis（~122 Mapper 文件） |
| `cat2bug-platform-im` | WebSocket/STOMP |
| `cat2bug-platform-ai` | Ollama / OpenAI 集成 |
| `cat2bug-platform-quartz` | 定时任务 |
| `cat2bug-platform-api` | Open API |
| `cat2bug-platform-common` | 工具、POI、DTO |
| `cat2bug-platform-generator` | 代码生成（生产不应进 Native） |

**已有能力（core，无需重写）**：

- `EmbeddedSpaFallbackFilter`：Vue history 路由 fallback（读 `classpath:/static/index.html`）
- `SecurityConfig`：`/static/**`、HTML/JS/CSS `permitAll`
- `InstallStartupSupport` / Setup / Upgrade 向导 MVC 实现
- `npm run build:embedded` → `admin/src/main/resources/static/**`

**Quarkus 线对照数据**（`feature/quarkus-full-migration`，embedded Native 已实测）：

| 指标 | Quarkus embedded Native |
|------|-------------------------|
| 未压缩 ELF | ~190 MB |
| UPX (`--best --lzma`) | ~42 MB |
| 冷启动 | ~0.3–1.5 s（未 UPX） |

**约束**：

- 从 **`core`** 开 worktree `feature/spring-native-upx`；与 Quarkus 分支 **禁止整分支 merge**
- Vue2 前端与 API 契约不变（`backend-api-compat`）
- **最终交付物**：单个 Native ELF，**含 embedded SPA**（非 API-only）
- 开发日常仍用 JVM：`mvn spring-boot:run` + `npm run dev`
- 文档 UTF-8 中文，存放 `readme/spring-native-delivery/`

## Goals / Non-Goals

**Goals:**

- 全量 `cat2bug-platform-admin` 编译为 **linux/amd64 + arm64** Native ELF（embedded SPA 内嵌）
- 用户 **无需 JRE、无需 nginx** 即可运行完整产品（API + 前端 + Setup/Upgrade）
- 与 JVM 版 **功能 parity**（im/ai/quartz/api/Excel/权限/缓存）
- 提供 `deploy/build-native-spring.sh`、可选 UPX 分发包、RPM/systemd（未压缩 ELF）
- 记录 `METRICS.md`；stretch 目标 raw **< 250MB**、UPX **< 65MB**（Quarkus 190/42 为对照）
- Cherry-pick Quarkus 线 **与框架无关** 的优化（FastExcel、Captcha 去 AWT、packaging-slim、UPX 脚本）

**Non-Goals:**

- 重写 Controller 或迁移至 Quarkus/JAX-RS
- Vue3 / 前端架构升级
- macOS/Windows Native 安装包
- Phase 1 即删除 JVM 构建或归档 Quarkus 分支
- 本 design 内一次性解决所有体积优化（Phase 3 专项）

## Decisions

### 1. 分支与并行策略

**决策**：在 `core` 创建 `feature/spring-native-upx`（git worktree）；Quarkus 线继续至 Phase 5 后 **冻结为对照/archive**。

**Cherry-pick 规则**：

| 可 cherry-pick | 不可 cherry-pick |
|----------------|------------------|
| FastExcel / Captcha 无 AWT | quarkus-* 模块、JAX-RS Resource |
| packaging-slim（去 stats.json） | Quarkus 配置、`application-native.properties` 语义 |
| UPX / RPM / Docker 脚本 **结构** | CDI、Panache、Quarkus Security |

**理由**：避免双栈 API 重写；Quarkus 线已验证的 Native 优化可直接移植到 Spring 代码路径。

### 2. Native 构建 toolchain

**决策**：

- **插件**：`spring-boot-maven-plugin` + `org.graalvm.buildtools:native-maven-plugin`（根 `pom.xml` 取消注释并 pin 版本，与 SB 3.3 BOM 对齐）
- **JDK**：源码 **Java 17**；Native **编译用 GraalVM/Mandrel JDK 21**（容器内）
- **容器镜像**：`ghcr.io/graalvm/native-image-community:21` 或 `quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21`（与 Quarkus 线统一，减少 CI 缓存种类）
- **Maven profile**：`-Pnative`（激活 AOT + native-maven-plugin）；`-Pembedded`（打包 static，与 packaging-slim 对齐）；Release 组合 **`-Pnative -Pembedded -Prelease`**
- **构建命令**（目标形态）：

```bash
npm run build:embedded          # 或 deploy/scripts/ui-build.sh embedded
mvn -pl cat2bug-platform-admin -am -Pnative -Pembedded -Prelease package -DskipTests \
  -Dspring.native.container-build=true
```

**产物路径**：`cat2bug-platform-admin/target/cat2bug-admin`（或 spring-boot 配置的 `imageName`）

**备选**：Paketo `spring-boot:build-image` —— 产出 OCI 镜像而非裸 ELF，不符合「单文件分发」主目标，不采用为主方案。

### 3. Embedded SPA 与 Native 资源

**决策**：

- **默认 Native 构建始终含 embedded**（不设 `SKIP_EMBEDDED` 为主路径）；构建前 **必须** `mvn clean` 或专用脚本保证 `target/classes/static/**` 与本次前端构建一致
- 新增 **`application-native.properties`**（或 `application-native.yml`）：
  - 保留 `spring.web.resources.static-locations=classpath:/static/`
  - 关闭 Native 不需要的 dev 特性（devtools、swagger UI）
  - 按需裁剪：`spring.native.remove-unused-autoconfig=true`（SB 3.3 默认行为验证后启用）
- 新增 **`Cat2BugNativeRuntimeHints`**（或分模块 `*RuntimeHintsRegistrar`）：
  - 注册 `classpath*:static/**` 为 **RuntimeHints.resources**（补充 AOT 未覆盖的大目录）
  - 注册 `EmbeddedSpaFallbackFilter`、`InstallStartupSupport` 等反射/序列化需求
- **不**改用 `forward:/index.html`（与 Thymeleaf 冲突，core 已有 Filter 方案）

**理由**：Quarkus 线证明 embedded ~70MB static 是体积主因，但产品要求单文件交付；Spring 侧 `EmbeddedSpaFallbackFilter` 已在 JVM 验证。

### 4. Graal 配置策略（分模块）

**决策**：优先 **Spring Boot 3 AOT 自动生成** + **`RuntimeHintsRegistrar`**；手工 `META-INF/native-image/*.json` 仅作兜底。

| 区域 | 策略 |
|------|------|
| **MyBatis** | `@MapperScan` 包下全部接口注册 reflection；`classpath*:mapper/**/*.xml` 注册 resources；评估 `mybatis-spring-native`（若与 3.0.4 兼容） |
| **Spring Security** | 依赖 AOT；Filter 链 bean 注册；`@PreAuthorize` 方法 security hints |
| **PageHelper** | 拦截器 bean 注册；Native 下验证分页 SQL |
| **J2Cache** | Phase 2 优先 **Redis-only**（`cat2bug.cache.type=redis`）；local Caffeine L1 需额外 hints 或 Native profile 禁用 L1 |
| **Flyway** | `classpath:db/migration/**` resources；H2/MySQL 双 dialect 脚本均保留 |
| **WebSocket / IM** | `MessageWebsocket` 注册 reflection + `@ServerEndpoint` 或 Spring WS 等价 hints；STOMP 配置 AOT 验证 |
| **Quartz** | Job 类 reflection；Native 下 `--initialize-at-run-time` 按需；备选 `@Scheduled` 仅 Native profile |
| **POI → FastExcel** | **Phase 2 末 / Phase 3 初** cherry-pick Quarkus FastExcel 实现，Native **排除 POI** |
| **Captcha** | cherry-pick `CaptchaPngRenderer`（无 AWT），避免 `libawt.so` |
| **H2 / MySQL** | 保留双驱动；Native 按需 `-H:IncludeResources` JDBC 驱动配置 |
| **Thymeleaf** | Setup 向导若用 Thymeleaf 页面，注册模板 resources；mostly REST JSON 则验证即可 |

**理由**：Spike 与 Quarkus 全量迁移已分级 P0/P1 blocker；Spring Native 路径 **同样顺序**，但无需重写 API。

### 5. Native profile 依赖裁剪

**决策**：profile **`native`** 或 **`release`** 下：

| 依赖 | Native 处理 |
|------|-------------|
| `spring-boot-devtools` | `provided` / 排除 |
| `knife4j-*` / swagger-ui | 排除（与 packaging-slim 一致） |
| `cat2bug-generator` | 排除 |
| `spring-boot-starter-test` | 仅 test scope |
| POI（Phase 3 后） | `provided`；运行时 FastExcel |

**理由**：与 prod 语义一致，减体积与 Graal 配置面。

### 6. 构建脚本与 UPX

**决策**：新增 `deploy/build-native-spring.sh`，行为对齐 `deploy/build-native-quarkus.sh`：

```
ui-build embedded → mvn clean package -Pnative -Pembedded → 复制为 cat2bug-admin-linux-{amd64|arm64}
→ 可选 UPX（默认 true，RPM 用未压缩）
→ 输出 smoke 提示（/actuator/health 或 /version）
```

**健康检查路径**：Spring Boot 3 使用 **`/actuator/health`**（若已启用）或现有 **`/version`**；脚本与文档统一，不引入 Quarkus `/q/health`。

**UPX**：`-UPX_COMPRESS=false` 可关闭；stretch 目标 **UPX < 65MB**，非阻塞发布。

### 7. RPM / systemd / Docker

**决策**：

- 复用 `deploy/rpm/cat2bug/` 结构，新增 **`build-rpm-spring.sh`** 或参数化现有脚本（二进制路径指向 Spring Native 产物）
- systemd unit：`ExecStart=/usr/bin/cat2bug-admin`（Native ELF）；**Requires 不含 java-\***
- Docker：`deploy/docker/native-minimal/` 运行 Linux ELF（与 Quarkus 脚本共用 `run-native-minimal.sh` 思路，改默认 BIN 路径）
- AlmaLinux 9 冒烟：`smoke-install.sh` 对 Spring Native RPM 同样适用

### 8. 测试与验收

**决策**：

| Phase | 自动化 | 手工 |
|-------|--------|------|
| 1 | `NativeEmbeddedSmokeTest`（Testcontainers 或 `@Disabled` + CI 脚本） | GET `/`、`/login` HTML、静态 JS 200 |
| 2 | 现有 `mvn test` + 关键集成测试 Native profile | Setup/Upgrade、IM、Excel 冒烟清单 |
| 3 | 构建脚本断言体积写入 METRICS | UPX 冷启动计时 |
| 4 | CI `deploy/ci/spring-native.yml` | AlmaLinux dnf 安装 |

**Kill Gate（Phase 1 结束，8 周内）**：

- embedded Native **`native:compile` 失败** 且 blocker 无明确解法 → 终止本 change，Quarkus 升为交付路径
- 或 SPA 冒烟不通过（`/`、`/static/js/*.js`、login API）

### 9. 切换默认 Release

**决策**：Phase 4 验收后：

- 根 `README.md` / `deploy/README.md` / `CLAUDE.md` 默认构建改为 `./deploy/build-native-spring.sh`
- JVM fat JAR **保留** `-Pembedded` 构建（开发/回滚）；CI Release 主产物为 Native ELF + 可选 UPX
- Quarkus 分支文档标注 **archived / reference**

**回滚**：同数据库下切回 JVM JAR 或 Quarkus Native；无 schema 变更。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| Spring Native 体积 > Quarkus embedded | stretch 目标非硬卡；Phase 3 专项优化；UPX 分发 |
| MyBatis 122 Mapper AOT 不全 | 分模块 `RuntimeHintsRegistrar`；CI 全量集成测试 |
| IM WebSocket Native 失败 | Phase 2 单独闸门；短期 JVM-only IM 为 **No-Go**（功能 parity 要求） |
| J2Cache Native 初始化 | Native profile Redis-only |
| POI Graal 不兼容 | 优先 FastExcel cherry-pick |
| embedded 构建 8GB+ RAM / 40min | Docker 构建；CI 缓存 `.m2` 与 Graal layer |
| UPX + embedded 冷启动慢 | RPM 未压缩；文档说明 portable 包用 UPX |
| 双分支 cherry-pick 冲突 | 仅 pick 独立 commit；禁止 merge quarkus 分支 |
| Quarkus 已投入沉没成本 | Phase 1 Kill Gate 早判；Quarkus 作对照与优化库 |

## Migration Plan

```
Phase 0 ──► 分支 + native-maven-plugin + build-native-spring.sh 骨架 + METRICS.md 模板
Phase 1 ──► embedded Native 编译 + SPA 冒烟 + Kill Gate（8 周）
Phase 2 ──► 全功能 parity（MyBatis/Security/IM/AI/Quartz/Setup/Upgrade/Excel）
Phase 3 ──► 体积优化（FastExcel/Captcha/packaging-slim/Graal 裁剪/UPX 度量）
Phase 4 ──► RPM/systemd/CI/AlmaLinux 冒烟 + 切换默认 Release + 文档
```

每 Phase 结束：更新 `readme/spring-native-delivery/PHASE-N.md`；Phase 3 起更新 `METRICS.md` 实测列（含 Quarkus 对照列）。

**Rollback**：任意 Phase 可继续以 JVM JAR 生产；Native 仅为 feature 分支产物直至 Phase 4 切换。

## Open Questions

1. **Health 端点**：生产是否统一启用 `spring-boot-starter-actuator` `/actuator/health`，或继续仅用 `/version` + 业务 API 冒烟？（建议 Phase 0 启用 read-only health，与 Quarkus 对照一致）
2. **J2Cache Native**：是否接受 Native profile **强制 Redis**（无 local-only 部署）？（建议：Native 包文档要求 Redis 或 embedded 单机用 local 简化配置）
3. **Phase 4 后 Spring JVM JAR**：长期双产物还是仅 Native + 源码 dev？（建议：Release 仅 Native；JVM JAR 保留 CI 构建作回归，不对外主推）
4. **Quarkus 分支归档时机**：Spring Phase 2 通过即可 archive，还是必须 Phase 4 全切换？（建议：Phase 4 切换后再 archive）
