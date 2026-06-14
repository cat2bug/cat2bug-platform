## MODIFIED Requirements

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
