## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: embedded 生产 JAR 保留 SPA 与 system/doc 文档

标准 Release **`embedded`** profile 生成的 fat JAR MUST 内嵌 Vue SPA 与 `static/docs/**`。Phase 4 后 **主推 Release 为 Spring Native embedded ELF**；JVM embedded JAR 仍 MUST 满足本 Requirement 供开发/回滚，体积目标（如 ≤70MB buffer）对 JVM 仍适用，Native 体积见 `spring-native-embedded-delivery` stretch 目标。

#### Scenario: JVM embedded 单 JAR 仍可用

- **WHEN** 执行 `mvn package -Pembedded -DskipTests`
- **THEN** 产物可单进程提供 SPA 与 `/system/doc`
- **AND** 无 `stats.json`

#### Scenario: Native 为文档默认 Release

- **WHEN** 查阅 Release 构建文档
- **THEN** 默认产物描述为 Native ELF 而非仅 JAR
