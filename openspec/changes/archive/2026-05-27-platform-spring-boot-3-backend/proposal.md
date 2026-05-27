## Why

Cat2Bug 后端当前基于 **Spring Boot 2.5.15** 与 **Java 8**，依赖 `javax.*`、Springfox、若依遗留的 `WebSecurityConfigurerAdapter` 等，长期面临安全补丁与生态支持压力。需要在 **不升级 Maven 产品版本号（仍为 0.6.2）**、**不改动 Vue2 前端** 的前提下，将后端迁移至 **Spring Boot 3.3.x**，并 **保留 J2Cache 一级（Caffeine）+ 二级（Redis）缓存** 的现有语义与配置方式。

## What Changes

- 将父 BOM 升级为 **Spring Boot 3.3.x**，版本策略为 **跟踪 3.3 线最新补丁**，由 **CI 定期对齐**（非写死单一补丁号）；本地/文档说明最低运行 **JDK 17**。
- 全仓 `javax.*` → `jakarta.*`（Servlet、Validation 等）。
- Spring Security 6：`SecurityConfig` / `ApiSecurityConfig` 改为 `SecurityFilterChain`；`@EnableMethodSecurity` 替代 `@EnableGlobalMethodSecurity`。
- OpenAPI：**移除 springfox**，改用 **springdoc-openapi** + **Knife4j 4（OpenAPI3）**；对外 API 文档路径在 README 中更新说明。
- **新增 `cat2bug-j2cache-spring-boot3`（或等价模块）**：替代 `j2cache-spring-boot2-starter`，保持 `RedisCache` + `CacheChannel` 及现有 `*-j2cache.properties` 配置契约。
- 升级 Druid、PageHelper、dynamic-datasource、JJWT、Flyway、MySQL 驱动等至 SB3/Jakarta 兼容版本。
- **Maven `version` 仍为 0.6.2**；在 `readme` / `CHANGELOG` 注明：自本变更合并后的 0.6.2 构建需 **JDK 17+** 与 **Spring Boot 3.3.x**。
- CI：增加/调整步骤，定期将 `spring-boot.version`（或 BOM import）对齐 **3.3.x 最新补丁** 并跑 `mvn verify`。
- **不在本变更内**：Vue3 迁移、Element Plus、Maven 版本号 bump（如 0.7.0）、业务 API 字段/路径重构、数据库 schema 大改。

## Capabilities

### New Capabilities

- `backend-spring-boot-33`：SB 3.3.x BOM、Java 17、jakarta 迁移、Security 6、依赖升级与 CI 补丁对齐策略。
- `backend-j2cache-boot3`：J2Cache 二级缓存在 Spring Boot 3 下的 Starter/自动配置，保持 `RedisCache` 与配置文件契约。
- `backend-api-compat`：对现有 Vue2 前端的 REST、JWT、WebSocket、静态资源嵌入等契约保持不变的可验证要求。

### Modified Capabilities

- （无）不修改 `openspec/specs/` 下既有业务域规格。

## Impact

- **Maven 模块**：根 `pom.xml` 及 `cat2bug-platform-{admin,framework,common,system,api,ai,im,quartz,generator}`。
- **新增模块（建议）**：`cat2bug-j2cache-spring-boot3` 或 `framework` 内子包 + 自动配置。
- **配置**：`application*.yml`、`mysql-j2cache.properties`、`h2-j2cache.properties`（尽量保持键名不变）。
- **文档**：根 `readme`、`deploy/`、JDK 要求、Swagger 新 URL。
- **CI**：GitHub Actions / 现有流水线（版本对齐 job 或 Dependabot 规则）。
- **前端**：无代码变更；需联调回归。
- **部署**：Docker 基础镜像需 JDK 17；单 JAR 启动命令不变。
