# backend-prod-packaging-slim Specification

## Purpose
TBD - 早期归档时 delta 头被原样带入主规范，2026-10-14 修正为主规范结构。
## Requirements
### Requirement: 生产 embedded JAR 不含构建分析产物

`npm run build:embedded` 写入 `cat2bug-platform-admin/src/main/resources/META-INF/resources/` 的产物 MUST NOT 包含 Bundle Analyzer 生成的 `stats.json` 或 `report.html`（或等价分析报告文件）。

#### Scenario: embedded 产物内无 stats.json

- **WHEN** 执行标准 Release 构建链（`build:embedded` + `mvn package`）
- **THEN** admin 打包产物（`target/quarkus-app/` 或 Native 资源）中不存在 `META-INF/resources/stats.json`
- **AND** 不存在 `META-INF/resources/report.html`

#### Scenario: 可选 analyze 构建

- **WHEN** 开发者执行文档约定的 analyze 命令（如 `npm run analyze`）
- **THEN** 分析报告写入约定目录（非 admin 的 `META-INF/resources/`）
- **AND** 不影响后续 embedded 生产构建产物

### Requirement: embedded 生产 JAR 保留 SPA 与 system/doc 文档

标准 Release 的 admin 产物（JVM 单 JAR 与 Native 单文件）MUST 内嵌 Vue SPA 静态资源（`index.html`、js/css 等），以支持单进程部署。系统文档 `/system/doc` 以外链方式提供，不要求内嵌 `docs/**`。

#### Scenario: 单进程加载 SPA

- **WHEN** 仅启动 embedded 构建的 admin 产物并访问根路径
- **THEN** 前端 SPA 正常加载
- **AND** `/prod-api` 请求由同一进程处理

### Requirement: 生产包剔除 devtool 依赖与 API

生产构建的 admin 产物 MUST NOT 包含代码生成器及其 REST API；MUST NOT 包含 Knife4j UI、`swagger-ui` 等交互式 OpenAPI 浏览器依赖。H2 与 MySQL JDBC 驱动 MUST 仍包含在产物中。

#### Scenario: 生产产物无代码生成 API

- **WHEN** 使用生产构建并启动应用
- **THEN** `/tool/gen/**` 不可访问或返回 404

#### Scenario: 生产产物无 Swagger UI

- **WHEN** 使用生产构建并启动应用
- **THEN** `/swagger-ui/**`、`/doc.html` 等 UI 路径不可访问

#### Scenario: H2 仍可用于安装向导

- **WHEN** 未安装实例使用生产产物进入 `/setup`
- **THEN** H2 引导与安装向导数据库选项仍可用

### Requirement: 生产前端构建剔除 tool dev 路由

生产前端构建（`build:embedded` 与 `build:prod`）MUST NOT 向产物注册或打包「代码生成」「表单构建」等业务 dev 页面路由（`tool/gen`、`tool/build` 等）；系统菜单中对应入口 MUST 对最终用户不可见（隐藏或移除）。

#### Scenario: 生产 SPA 无 tool/gen 路由

- **WHEN** 用户在生产构建的前端中直接访问 `/tool/gen`
- **THEN** 无法进入代码生成页面（404 或重定向至合法页）
- **AND** 侧边栏不展示「代码生成」菜单项

