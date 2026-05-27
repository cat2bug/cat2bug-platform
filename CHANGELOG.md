# Changelog

## [0.6.2] — 运行时要求更新（后端 Spring Boot 3.3）

Maven 版本号仍为 **0.6.2**，以下变更主要影响**后端构建与部署**：

### Runtime requirements

- **JDK 17+**（不再支持 Java 8/11 运行后端）
- **Spring Boot 3.3.x**（当前 BOM 见根 `pom.xml` 的 `spring-boot.version`）
- Docker 单容器镜像基础 JRE 升级为 **17**（`deploy/docker/after-dockerfile`）

### 后端

- Jakarta EE 命名空间迁移（`javax.*` → `jakarta.*`，保留 `javax.sql` 等 JDK API）
- Spring Security 6 过滤链重构
- OpenAPI：springdoc + Knife4j 4 替代 springfox
- J2Cache：在 `cat2bug-platform-common` 内提供 Spring Boot 3 自动配置（替代 `j2cache-spring-boot2-starter`）
- JJWT 升级至 0.12.x；MySQL 驱动改为 `mysql-connector-j`

### 未变更

- 前端仍为 Vue 2.7 + Element UI，无需为本次升级修改前端代码
- REST / JWT / WebSocket 对外契约保持兼容

详见 [docs/backend-spring-boot-3.md](docs/backend-spring-boot-3.md)。
