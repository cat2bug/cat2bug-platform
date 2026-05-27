# 后端 Spring Boot 3.3 升级说明

本文档说明 Cat2Bug-Platform **0.6.2** 后端在 Spring Boot 3.3 线上的构建、运行与依赖对齐策略。Maven 坐标版本仍为 **0.6.2**，仅运行时与构建工具链要求变化。

## 运行时要求

| 项目 | 要求 |
|------|------|
| JDK | **17+**（开发与生产一致） |
| Spring Boot | **3.3.x**（当前 BOM：`3.3.13`，见根 `pom.xml` 中 `spring-boot.version`） |
| Maven | 3.6+ |
| 前端 | 仍为 Vue 2.7，**无需**为 SB3 单独升级 |

> 使用 Java 8/11 构建或运行将失败。Docker 部署请使用 **JDK 17** 基础镜像（见 `deploy/docker/after-dockerfile`）。

## 本地构建

```bash
# 确保 JAVA_HOME 指向 JDK 17（macOS Homebrew 示例）
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

mvn clean verify
```

产物：`cat2bug-platform-admin/target/cat2bug-admin.jar`

## 主要技术变更（相对 SB 2.5）

- **Jakarta EE**：`javax.servlet` / `javax.validation` 等已迁移为 `jakarta.*`
- **Spring Security 6**：`SecurityFilterChain` + `@EnableMethodSecurity`
- **OpenAPI**：springfox 已移除，使用 **springdoc** + **Knife4j 4**（OpenAPI 3）
- **J2Cache**：`j2cache-spring-boot2-starter` 替换为 **`cat2bug-platform-common`** 内 `Cat2BugJ2CacheAutoConfiguration` 自动配置
- **JWT**：JJWT **0.12.x**（Token 字段与 Header 保持兼容）
- **MySQL 驱动**：`mysql-connector-j`（替代 `mysql-connector-java`）
- **Druid**：`druid-spring-boot-3-starter`（替代 `druid-spring-boot-starter`）
- **MyBatis**：`mybatis-spring-boot-starter` **3.0.x**（兼容 Spring Framework 6.2+）

## J2Cache 配置（未变）

`application.yml` 仍使用：

```yaml
j2cache:
  config-location: classpath:${spring.database-type}-j2cache.properties
```

- **H2**：`h2-j2cache.properties`，L2 关闭
- **MySQL**：`mysql-j2cache.properties`，Caffeine L1 + Redis L2

业务仍通过 `RedisCache` → `CacheChannel` 访问缓存。

**序列化**：JDK 17 下 `h2-j2cache.properties` / `mysql-j2cache.properties` 使用 `j2cache.serialization=fastjson`（原 `json` 基于 FST，需额外 `--add-opens`）。升级后 Redis L2 中旧缓存键需自然过期或手动清理。

**Spring Boot 3 行为**：`application.yml` 中 `spring.main.allow-circular-references=true`（保留历史 API Token 等循环依赖）；登录使用 `@Qualifier("manageAuthenticationManager")`。

## API 文档入口

在 `swagger.enabled=true` 时（默认生产为 `false`）：

| 说明 | 路径 |
|------|------|
| Knife4j 文档 | `/doc.html` |
| Swagger UI | `/swagger-ui/index.html` |
| OpenAPI JSON | `/v3/api-docs` |

Security 白名单已包含上述路径，与迁移前行为一致。

## Spring Boot 3.3.x BOM 对齐策略

1. 根 `pom.xml` 属性 **`spring-boot.version`** 锁定 **3.3 线最新补丁**。
2. **Dependabot**（`.github/dependabot.yml`）每周检查 Maven 依赖；对 `org.springframework.boot` **忽略 ≥ 3.4.0** 的版本，避免误升主版本。
3. **CI**（`.github/workflows/backend-verify.yml`）在 push/PR 上执行 `mvn clean verify`（JDK 17）。
4. 合并 Dependabot PR 前必须 CI 全绿；**禁止**在未评审情况下将 BOM 升到 **3.4+**（另开 OpenSpec 变更）。

## 前端联调

后端 API 契约未变：登录、`Authorization` Header、WebSocket、`AjaxResult` 结构均保持兼容。使用现有 Vue2 工程：

```bash
# 终端 1：后端（admin 模块，JDK 17）
cd cat2bug-platform-admin && mvn spring-boot:run

# 终端 2：前端
cd cat2bug-platform-ui && npm run dev
```

访问 `http://localhost:2222`，API 代理至 `http://localhost:2020`。

## 嵌入式打包

```bash
cd cat2bug-platform-ui && npm run build:embedded
cd .. && mvn clean package -DskipTests
java -jar cat2bug-platform-admin/target/cat2bug-admin.jar
```
