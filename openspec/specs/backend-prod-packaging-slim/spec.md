## ADDED Requirements

### Requirement: 生产 embedded JAR 不含构建分析产物

`npm run build:embedded` 写入 `cat2bug-platform-admin/src/main/resources/static/` 的产物 MUST NOT 包含 Webpack Bundle Analyzer 生成的 `stats.json` 或 `report.html`（或等价分析报告文件）。

#### Scenario: embedded JAR 内无 stats.json

- **WHEN** 执行标准 Release 构建链（`build:embedded` + Maven `embedded` profile package）
- **THEN** 生成的 `cat2bug-admin.jar` 内不存在 `BOOT-INF/classes/static/stats.json`
- **AND** 不存在 `BOOT-INF/classes/static/report.html`

#### Scenario: 可选 analyze 构建

- **WHEN** 开发者执行文档约定的 analyze 命令（如 `npm run analyze`）
- **THEN** 分析报告写入约定目录（非 `admin/.../static/`）
- **AND** 不影响后续 embedded 生产构建产物

### Requirement: embedded 生产 JAR 保留 SPA 与 system/doc 文档

标准 Release **`embedded`** profile 生成的 fat JAR MUST 内嵌 Vue SPA 静态资源（`index.html`、js/css 等）以及 `static/docs/**`（来自 `readme/production`），以支持单 JAR 部署与 `/system/doc` 文档浏览。Phase 4 后 **主推 Release 为 Spring Native embedded ELF**；JVM embedded JAR 仍 MUST 满足本 Requirement 供开发/回滚，Native 体积见 `spring-native-embedded-delivery` stretch 目标。

#### Scenario: 单 JAR 加载 SPA

- **WHEN** 仅启动 `embedded` profile 构建的 `cat2bug-admin.jar` 并访问根路径
- **THEN** 前端 SPA 正常加载
- **AND** `/prod-api` 请求由同一进程处理

#### Scenario: system/doc 可访问文档

- **WHEN** 用户登录后访问 `/system/doc` 并打开任意已注册文档节点
- **THEN** 页面通过 GET `/docs/**` 成功加载 Markdown 渲染内容
- **AND** 文档内图片路径 `/docs/images/**` 可正常显示

### Requirement: api-only 后端 JAR 不含 embedded static

Maven `api-only` profile 生成的 fat JAR MUST NOT 包含 `BOOT-INF/classes/static/**`（含 SPA 与 docs），MUST 仍包含后端运行所需 classpath 资源（如 `application.yml`、Flyway 脚本、`defaults/`、i18n 等）。

#### Scenario: api-only JAR 无 static 目录

- **WHEN** 执行 `mvn package -Papi-only`
- **THEN** 产物 JAR 内不存在 `BOOT-INF/classes/static/`
- **AND** 应用主类与 `/prod-api/**` API 可正常启动

#### Scenario: Docker 后端与 nginx 前端配合

- **WHEN** Docker Compose 使用 `api-only` 后端 JAR 与 nginx 托管的 `build:prod` 前端 static
- **THEN** 经 nginx 反代访问 SPA 与 `/prod-api` 行为正常
- **AND** `/system/doc` 经 nginx 提供的 `/docs/**` 可访问

### Requirement: 生产包剔除 devtool 依赖与 API

默认生产 profile（`embedded` 与 `api-only`）构建的 fat JAR MUST NOT 包含 `cat2bug-generator` 模块及其 REST API；MUST NOT 包含 Knife4j UI、`swagger-ui` 等交互式 OpenAPI 浏览器依赖。H2 与 MySQL JDBC 驱动 MUST 仍包含在 fat JAR 中。

#### Scenario: 生产 JAR 无代码生成 API

- **WHEN** 使用生产 profile 构建并启动应用
- **THEN** `/tool/gen/**`（或等价代码生成路径）不可访问或返回 404
- **AND** `BOOT-INF/lib/` 中不存在 `velocity-engine-core` 等 generator 专属传递依赖（若可辨识）

#### Scenario: 生产 JAR 无 Swagger UI

- **WHEN** 使用生产 profile 构建并启动且 `swagger.enabled` 为 false
- **THEN** `/swagger-ui/**`、`/doc.html`（Knife4j）等 UI 路径不可访问
- **AND** `BOOT-INF/lib/` 中不存在 `swagger-ui-*.jar`、`knife4j-openapi3-ui-*.jar`

#### Scenario: H2 仍可用于安装向导

- **WHEN** 未安装实例使用 embedded 或 api-only 生产 JAR 进入 `/setup`
- **THEN** H2 引导与安装向导数据库选项仍可用
- **AND** `BOOT-INF/lib/` 仍包含 H2 驱动 JAR

### Requirement: 生产前端构建剔除 tool dev 路由

生产前端构建（`build:embedded` 与 `build:prod`）MUST NOT 向产物注册或打包「代码生成」「表单构建」等业务 dev 页面路由（`tool/gen`、`tool/build` 等）；系统菜单中对应入口 MUST 对最终用户不可见（隐藏或移除）。

#### Scenario: 生产 SPA 无 tool/gen 路由

- **WHEN** 用户在生产构建的前端中直接访问 `/tool/gen`
- **THEN** 无法进入代码生成页面（404 或重定向至合法页）
- **AND** 侧边栏不展示「代码生成」菜单项

### Requirement: Lombok 不进生产 fat JAR

所有模块声明的 `lombok` 依赖 MUST 为 `provided` scope；spring-boot repackage 后的生产 fat JAR MUST NOT 在 `BOOT-INF/lib/` 中包含 `lombok-*.jar`。

#### Scenario: 生产 JAR lib 无 lombok

- **WHEN** 执行生产 profile package
- **THEN** `BOOT-INF/lib/lombok-*.jar` 不存在

### Requirement: 生产 JAR 体积验收阈值

Release 构建完成后，项目 MUST 可验证 embedded 与 api-only 产物体积符合约定上限，以防止分析产物或 static 误打包回归。embedded JAR MUST 额外满足 **`static/docs/images` 解压总字节 ≤ 22 MB**（在 `prod-docs-images-slim` 落地后）。

#### Scenario: embedded JAR 体积上限

- **WHEN** 完成标准 Release 构建链（含已优化的 `readme/production/images`）
- **THEN** `cat2bug-admin.jar`（embedded）压缩后体积 ≤ **145MB**
- **AND** JAR 内 `static/docs/images/` 解压总字节 ≤ **22 MB**

#### Scenario: api-only JAR 体积上限

- **WHEN** 完成 `mvn package -Papi-only,!embedded`
- **THEN** api-only 产物 JAR 压缩后体积 ≤ **100MB**

#### Scenario: docs/images 无超宽 PNG 进入 static

- **WHEN** 完成 `build:embedded`
- **THEN** `static/docs/images/**` 中不存在 `pixelWidth > 1600` 的 PNG

### Requirement: Native embedded 构建沿用 slim 规则

Spring Native embedded 构建链（`build-native-spring.sh` + `-Pembedded -Pnative`）MUST 遵守 `backend-prod-packaging-slim` 的 static 卫生规则：`stats.json`、`report.html` 不得进入 `static/**`；`static/docs/**` MUST 保留；生产 MUST 排除 generator 与 Swagger/Knife4j UI 运行时依赖。

#### Scenario: Native static 无分析产物

- **WHEN** 执行标准 Spring Native embedded Release 构建
- **THEN** Native 镜像资源中不存在 `static/stats.json` 或 `static/report.html`

#### Scenario: Native 保留 docs

- **WHEN** embedded Native 运行且访问 `/docs/**`
- **THEN** 文档与图片与 slim embedded JAR 场景一致

### Requirement: api-only 与 Native embedded 职责分离

Maven **`api-only`** profile 仍用于 Docker 后端（无 SPA）；**Native Release 主路径为 embedded 单文件**，不得将 `api-only` 作为默认 Native 产物。若提供 Native api-only 变体，MUST 为可选脚本参数（如 `SKIP_EMBEDDED=true`）且文档明确与主 Release 区别。

#### Scenario: 默认 Native 脚本含 embedded

- **WHEN** 执行 `./deploy/build-native-spring.sh` 且未设置 `SKIP_EMBEDDED=true`
- **THEN** 构建前执行 embedded 前端且产物含 SPA

#### Scenario: 可选 API-only Native 文档化

- **WHEN** 运维设置 `SKIP_EMBEDDED=true` 构建 Native
- **THEN** README 说明需独立前端且体积度量单独记录

#### Scenario: JVM embedded 单 JAR 仍可用

- **WHEN** 执行 `mvn package -Pembedded -DskipTests`
- **THEN** 产物可单进程提供 SPA 与 `/system/doc`
- **AND** 无 `stats.json`

#### Scenario: Native 为文档默认 Release

- **WHEN** 查阅 Release 构建文档
- **THEN** 默认产物描述为 Native ELF 而非仅 JAR
