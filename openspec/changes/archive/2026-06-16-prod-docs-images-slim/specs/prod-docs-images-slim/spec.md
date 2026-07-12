## ADDED Requirements

### Requirement: 文档截图宽度上限

`readme/production/images/**` 下所有 PNG MUST 满足 `pixelWidth ≤ 1600`。优化脚本 MUST 仅缩小超宽图，MUST NOT 将宽度小于 1600 的图片放大。

#### Scenario: 无超宽 PNG 残留

- **WHEN** 维护者执行 `scripts/check-docs-images.sh`（或 CI 等价步骤）
- **THEN** 不存在 `pixelWidth > 1600` 的 PNG
- **AND** 脚本以 exit code 0 结束

#### Scenario: 边界宽度不被放大

- **WHEN** 某 PNG 原始 `pixelWidth` 为 1569
- **THEN** 优化流水线不改变其像素尺寸
- **AND** 输出文件 `pixelWidth` 仍为 1569

### Requirement: 文档截图优化流水线

项目 MUST 提供可重复执行的文档图片优化命令（如 `npm run optimize:docs-images`），对 `readme/production/images/**` 中 PNG 依次执行：条件 resize（宽 > 1600 → 1600）、**pngquant**（`--skip-if-larger`）、**oxipng**  strip/压缩。MUST NOT 仅做 resize 而跳过 pngquant。

#### Scenario: 批量优化降低总体积

- **WHEN** 对当前基线（~26MB / 132 张超宽）执行完整 optimize 流水线
- **THEN** `readme/production/images` 总字节数 ≤ **22 MB**
- **AND** 相对优化前净减少 ≥ **3 MB**

#### Scenario: skip-if-larger 防止劣化

- **WHEN** pngquant 对某文件输出大于输入
- **THEN** 流水线保留较小者（不替换为更大文件）
- **AND** 该文件路径仍通过 width check

### Requirement: 消除重复文档 PNG

项目 MUST 识别 `readme/production/images/**` 内 MD5 相同的 PNG，保留 canonical 路径（优先 `user-guide/**`），删除重复副本，并更新所有引用 Markdown 中的相对图片路径，使 `/docs/images/**` 仍可访问。

#### Scenario: 重复文件去重后文档可显示

- **WHEN** 去重完成且执行 `build:embedded`
- **THEN** `/system/doc` 下曾引用被删重复图的页面仍正常显示图片
- **AND** GET `/docs/images/**` 返回 200

#### Scenario: 去重带来可度量收益

- **WHEN** 去重与 optimize 均完成
- **THEN** 相对 Phase 1 基线，`docs/images` 至少减少重复项等价字节（约 **1 MB** 量级）

### Requirement: CI 文档图片门禁

当 PR 变更 `readme/production/images/**` 时，CI MUST 运行尺寸/体积检查脚本；存在 `pixelWidth > 1600` 的 PNG 时 MUST 失败。

#### Scenario: PR 引入超宽图失败

- **WHEN** PR 新增或修改 PNG 且 `pixelWidth > 1600`
- **THEN** CI check-docs-images 步骤失败
- **AND** 日志指出违规文件路径

#### Scenario: 仅 Markdown 变更不触发宽图失败

- **WHEN** PR 仅修改 `readme/production/**/*.md` 且未改 images
- **THEN** images width check 通过（或跳过）

### Requirement: embedded 产物 docs/images 体积回归

标准 Release `build:embedded` 写入 JAR 的 `static/docs/images/**` MUST 满足体积上限，并与源目录 `readme/production/images` 字节数一致（CopyWebpackPlugin 直拷贝，无膨胀）。

#### Scenario: embedded JAR 内 docs/images 上限

- **WHEN** 完成 `npm run build:embedded` + Maven `embedded` profile package
- **THEN** JAR 内 `BOOT-INF/classes/static/docs/images/` 解压总字节 ≤ **22 MB**
- **AND** `/system/doc` 与 `/docs/images/**` 冒烟通过

#### Scenario: 相对 backend-prod-packaging-slim Phase 1 额外下降

- **WHEN** 本 change 验收构建完成
- **THEN** embedded JAR 体积相对 Phase 1 基线额外减少 ≥ **3 MB**（或与 `docs/images` 净减一致）

### Requirement: 截图贡献规范

`readme/production` 贡献说明 MUST 文档化：新截图导出宽度建议 **1280–1600px**；禁止未经 optimize 脚本提交 **3600px** 全屏截图；Playwright 截图脚本 SHOULD 默认 viewport 宽度 ≤ 1600。

#### Scenario: 贡献者按规范导出

- **WHEN** 贡献者阅读文档约定的截图导出章节
- **THEN** 可获知推荐 viewport 宽度与 `optimize:docs-images` 用法
- **AND** 获知 CI 将拒绝 `width > 1600` 的 PNG
