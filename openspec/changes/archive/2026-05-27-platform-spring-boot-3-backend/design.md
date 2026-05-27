## Context

- 当前：Spring Boot **2.5.15**，`java.version=1.8`，约 **681** 个 Java 源文件；广泛使用 `javax.servlet` 等；`SecurityConfig` / `ApiSecurityConfig` 继承 `WebSecurityConfigurerAdapter`；OpenAPI 为 **springfox 3 + knife4j 3.0.3**。
- 缓存：业务通过 `RedisCache` → `CacheChannel`（J2Cache），依赖 **`j2cache-core 2.8.5`** + **`j2cache-spring-boot2-starter 2.8.0`**；生产 **Caffeine L1 + Redis L2**，H2 开发 **L2=none**。
- 约束（已决策）：**仅后端**；**Maven 版本保持 0.6.2**；**必须保留 J2Cache 二级缓存**；**Spring Boot 3.3.x 最新补丁由 CI 定期对齐**；前端 Vue2 不改动。

## Goals / Non-Goals

**Goals:**

- 后端在 **JDK 17** 上运行于 **Spring Boot 3.3.x**（补丁随 3.3 线滚动）。
- J2Cache 行为与配置路径与现网一致：`RedisCache` API、`j2cache.config-location`、各 cache region 不变。
- Vue2 客户端无改代码即可对接（REST + JWT + WebSocket + 嵌入 static）。
- CI 可检测 BOM 过时并自动或半自动对齐 3.3.x 最新补丁后全量编译测试。

**Non-Goals:**

- Vue3 / Element Plus。
- 产品版本号改为 0.7.0（仅用文档说明运行时要求变化）。
- 用 Spring Cache 双层方案替换 J2Cache。
- 重写业务 Controller 契约或数据库结构。

## Decisions

### 1. Spring Boot 版本：3.3.x 最新补丁 + CI 对齐

- **决策**：根 `pom.xml` 使用属性 `spring-boot.version`，取值 **3.3.x 当前最新补丁**；CI 每周（或 Dependabot）检查 [Spring Boot 3.3 releases](https://github.com/spring-projects/spring-boot/releases) 并提 PR  bump 该属性，合并前必须 `mvn clean verify` 通过。
- **理由**：兼顾安全补丁与可重复构建；避免锁死过时补丁。
- **实现要点**：
  - 文档记录「构建时使用的最低 SB 版本」与「推荐跟随 CI 合并的 BOM」。
  - 禁止在未跑 CI 的情况下手动跳到 3.4+（主版本升级另开 change）。
- **备选**：写死 `3.3.13` → 拒绝，用户要求 CI 对齐。

### 2. Java 17

- **决策**：`java.version=17`，`maven-compiler-plugin` 使用 `release` 17；Docker/文档统一 JDK 17。
- **理由**：Spring Boot 3 官方基线。

### 3. J2Cache：自研 Spring Boot 3 Starter

- **决策**：新增模块 **`cat2bug-j2cache-spring-boot3`**（artifact 名可调整），参考 `j2cache-spring-boot2-starter` 实现 `@AutoConfiguration`，仅依赖 **`j2cache-core` 2.8.5-release**（或经验证兼容的补丁版）；`common` 模块改为依赖新 starter，**移除** `j2cache-spring-boot2-starter`。
- **理由**：Maven Central **无官方 boot3 starter**；业务已深度绑定 `CacheChannel` 与 properties 文件。
- **必须保持**：
  - `application.yml` 中 `j2cache.config-location: classpath:${spring.database-type}-j2cache.properties`
  - `RedisCache` 与各 `*_CACHE_REGION` 常量
  - `@ConditionalOnProperty(prefix = "j2cache.L2", ...)` 的 `RedisConfig` / `RateLimiterAspect`
- **验证**：H2（L2 关闭）启动；MySQL + Redis（L2 开启）登录/token/验证码/字典/防重复提交。

### 4. OpenAPI 栈

- **决策**：删除 springfox；引入 `springdoc-openapi-starter-webmvc-ui` + `knife4j-openapi3-jakarta-spring-boot-starter`（版本随 SB 3.3 BOM 对齐）。
- **注解迁移**：`io.swagger.annotations.*` → `io.swagger.v3.oas.annotations.*`（可分批，以编译通过为准）。
- **文档**：README 更新文档入口 URL（如 `/swagger-ui.html` → springdoc 默认路径）。

### 5. Spring Security 6

- **决策**：两套过滤链（主站 + API）改为 `@Bean SecurityFilterChain`，`@EnableMethodSecurity`；`AuthenticationManager` 通过 `AuthenticationConfiguration` 注入。
- **理由**：`WebSecurityConfigurerAdapter` 在 SB3 已移除。

### 6. 前端契约冻结

- **决策**：迁移期间禁止改动 `AjaxResult` 结构、登录接口、Token Header、`/websocket/**` 消息格式；以 **手工 + 现有 Playwright（若覆盖 API）** 回归。
- **理由**：前端不动，后端是唯一变量。

### 7. 产品版本 0.6.2 不变

- **决策**：`${cat2bug.version}` / 各模块 `0.6.2` 不改；`CHANGELOG` 增加 **Runtime requirements** 小节。
- **理由**：用户明确要求；降低发行标签混乱，用文档承担语义。

## CI 补丁对齐（设计草案）

```
┌─────────────┐     每周 / Dependabot      ┌──────────────────┐
│ 查询 SB 3.3 │ ─────────────────────────▶ │ 最新 3.3.x 补丁   │
│  release    │                            └────────┬─────────┘
└─────────────┘                                     │
                                                    ▼
                                          更新 pom spring-boot.version
                                                    │
                                                    ▼
                                          mvn clean verify (全模块)
                                                    │
                                    ┌───────────────┴───────────────┐
                                    ▼                               ▼
                              PR 自动创建                      失败则阻断合并
```

- 可在 `.github/workflows/` 增加 `spring-boot-3.3-bom-check.yml`，或配置 Dependabot `package-ecosystem: maven` 仅允许 `org.springframework.boot` 的 **3.3.** 前缀更新。
- `readme` 说明：生产建议跟随已通过的 CI BOM 提交构建，而非长期停留在旧补丁。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| J2Cache boot2 starter 与 SB3 不兼容 | 自研 starter，单列 tasks 与验收 |
| CI 自动 bump 引入破坏性补丁 | 限制主版本 3.3.x；verify 失败不合并 |
| springfox 移除导致文档链接失效 | README + 部署文档更新 |
| jjwt 0.9.1 过旧 | 升到 0.12.x，保持 token 声明字段兼容 |
| `cat2bug-spring-boot-junit` 不兼容 SB3 | 验证或替换为 spring-boot-starter-test |
| 用户误以为 0.6.2 仍可用 Java 8 | CHANGELOG 与 Docker 镜像明确 JDK 17 |

## Migration Plan

1. 分支 `feature/spring-boot-3-backend` 开发。
2. 阶段合并：先 `mvn compile` + J2Cache starter，再 Security + springdoc，最后全量测试。
3. 发布：合并主干后打 **0.6.2** 构建产物（或同 tag 重建），发布说明强调 JDK 17。
4. 回滚：保留 SB2 分支/tag；数据库无迁移则回滚仅换 JAR。

## Open Questions

- `j2cache-core` 是否尝试升级到 gitee `ld/J2Cache` 新分支：默认 **保持 2.8.5**，升级另做 spike。
- Redis 客户端：现配置 `Jedis`，是否与 Spring Data Redis 3 默认 Lettuce 并存——设计为 **J2Cache 走 J2Cache 配置，限流走 RedisTemplate**，互不替换。
