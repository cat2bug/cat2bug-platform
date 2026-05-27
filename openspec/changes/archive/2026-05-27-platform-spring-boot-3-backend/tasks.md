## 1. 基线与 BOM（Spring Boot 3.3.x + CI）

- [x] 1.1 根 `pom.xml`：增加 `spring-boot.version`（3.3.x 最新补丁）、`java.version=17`、BOM import `spring-boot-dependencies`
- [x] 1.2 启用/配置 CI 或 Dependabot：仅自动 bump `org.springframework.boot` **3.3.** 前缀；合并前 `mvn clean verify`
- [x] 1.3 添加 workflow 或文档：`docs/backend-spring-boot-3.md` 说明 JDK 17、BOM 对齐策略、0.6.2 运行时要求
- [x] 1.4 更新 `readme` / `CHANGELOG`：0.6.2 构建需 JDK 17 + SB 3.3.x（Maven 版本号不变）

## 2. Jakarta 与编译通过

- [x] 2.1 全仓 `javax.servlet` / `javax.validation` 等 → `jakarta.*`（脚本 + 手工修 Security/过滤器）
- [x] 2.2 `maven-compiler-plugin` 使用 `release` 17；移除对 Java 8 的假设
- [x] 2.3 `mysql-connector-java` → `mysql-connector-j`；Flyway、Druid、PageHelper、dynamic-datasource 升至 SB3 兼容版

## 3. J2Cache Boot3 Starter

- [x] 3.1 新建模块 `cat2bug-j2cache-spring-boot3`：`@AutoConfiguration`、`CacheChannel` Bean、加载 `j2cache.config-location`
- [x] 3.2 `cat2bug-platform-common`：替换 `j2cache-spring-boot2-starter` 为新 starter；保留 `j2cache-core 2.8.5`
- [x] 3.3 验证 `h2-j2cache.properties`（L2=none）与 `mysql-j2cache.properties`（L2=Redis）启动（H2 内存库冒烟已通过；MySQL+Redis 需本地环境）
- [ ] 3.4 回归：`RedisCache` — 登录 token、验证码、dict/config、防重复提交

## 4. Spring Security 6

- [x] 4.1 重写 `SecurityConfig`：`SecurityFilterChain` + `@EnableMethodSecurity`
- [x] 4.2 重写 `ApiSecurityConfig`：API 链路与 JWT/API Token 过滤器顺序与迁移前一致
- [ ] 4.3 回归：匿名白名单、CORS、登出、权限注解接口

## 5. OpenAPI（springdoc + Knife4j 4）

- [x] 5.1 移除 springfox 依赖；引入 springdoc + knife4j-openapi3
- [x] 5.2 迁移 `SwaggerConfig` 与 Controller 上 swagger 注解至 OpenAPI3（以编译通过为准）
- [x] 5.3 更新 README 中文档入口 URL

## 6. 其它依赖与测试

- [x] 6.1 升级 JJWT（保持 token 字段兼容）；处理 `CachingConfigurerSupport` 弃用
- [x] 6.2 验证 `cat2bug-spring-boot-junit` 或改用 `spring-boot-starter-test`
- [x] 6.3 `mvn clean verify` 全绿；修复因 BOM bump 导致的回归

## 7. 前端契约验收（Vue2 不改动）

- [ ] 7.1 本地：Vue2 `npm run dev` + SB3 后端联调 — 登录、缺陷列表、上传、通知
- [ ] 7.2 单 JAR：`build:embedded` + `cat2bug-admin.jar` 冒烟
- [ ] 7.3 对照 `specs/backend-api-compat/spec.md` 勾选验收场景

## 8. 部署与 Docker

- [x] 8.1 更新 `deploy/` Docker 镜像基础 JDK 17
- [ ] 8.2 验证 docker-compose / 单容器启动文档
