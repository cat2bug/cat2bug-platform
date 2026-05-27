## ADDED Requirements

### Requirement: Spring Boot 3.3.x 与 Java 17 构建

项目 MUST 使用 **Spring Boot 3.3.x** 作为依赖 BOM，编译与运行目标为 **Java 17**。根 `pom.xml` MUST 通过属性（如 `spring-boot.version`）声明当前采用的 3.3.x 补丁版本，且该属性 MUST 属于 **3.3 主版本线**（不得在未另开变更的情况下使用 3.4+）。

#### Scenario: 全模块编译

- **WHEN** 开发者在 JDK 17 下执行 `mvn clean verify`
- **THEN** 所有后端模块编译与单元测试（若存在）通过，无 `javax.*` 与 Spring Boot 2 专有 API 残留导致的编译错误

#### Scenario: 运行环境

- **WHEN** 用户部署官方构建的 `cat2bug-admin` JAR
- **THEN** 必须使用 JDK 17 或更高版本启动；文档中 MUST 说明此要求且 Maven 坐标版本仍为 **0.6.2**

### Requirement: Jakarta 命名空间迁移

所有后端源码与依赖 MUST 使用 **jakarta.*** 替代 **javax.***（Servlet、Validation、Annotation 等），不得在生产代码中保留与 Spring Boot 3 冲突的 `javax.servlet` 等 import。

#### Scenario: 过滤器与控制器编译

- **WHEN** 编译包含 XSS 过滤器、JWT 过滤器、Controller 的模块
- **THEN** 这些类使用 `jakarta.servlet` 等包且成功编译

### Requirement: Spring Security 6 配置

系统 MUST 使用 Spring Security 6 的 **`SecurityFilterChain` Bean** 配置主站与 API 两套安全链，并使用 **`@EnableMethodSecurity`** 启用方法级安全；MUST NOT 再使用 `WebSecurityConfigurerAdapter`。

#### Scenario: 登录与鉴权

- **WHEN** 用户使用现有账号密码登录并访问受保护接口
- **THEN** 行为与迁移前一致：未认证返回 401、已认证按权限访问

### Requirement: OpenAPI 文档栈（springdoc）

系统 MUST 使用 **springdoc-openapi**（及项目选定的 Knife4j 4 OpenAPI3 集成）提供 API 文档；MUST NOT 依赖 **springfox**。

#### Scenario: 文档 UI 可访问

- **WHEN** 开发环境启用 API 文档配置
- **THEN** 可通过文档 UI 浏览主要 Controller；README 提供当前文档入口路径

### Requirement: CI 定期对齐 3.3.x 最新补丁

仓库 MUST 具备将 `spring-boot.version`（或等价 BOM 版本）对齐 **Spring Boot 3.3 线最新补丁** 的机制（如 Dependabot、定时 workflow 或 documented 手动流程），且对齐后 MUST 运行 **`mvn clean verify`** 作为合并门禁。

#### Scenario: 补丁升级 PR

- **WHEN** CI 或 Dependabot 提交将 `spring-boot.version` 从 `3.3.N` 升至 `3.3.M`（M>N）的 PR
- **THEN** PR 关联的 CI 流水线执行全量 verify；失败则不得合并

#### Scenario: 主版本隔离

- **WHEN** 上游发布 Spring Boot **3.4.0** 或更高主版本
- **THEN** 自动对齐机制 MUST NOT 在无新 OpenSpec 变更的情况下将 BOM 升到 3.4+
