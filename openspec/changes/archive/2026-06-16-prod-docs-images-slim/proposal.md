## Why

`backend-prod-packaging-slim` Phase 1 已剔除 `stats.json`、devtool 依赖等大项；`static/docs/**`（来自 `readme/production`）仍约 **26MB**，其中 **`images/` 占 ~25MB（234 张 PNG）**。132 张截图宽度 **> 1600px**（含大量 **3600px** 高分辨率导出），而 `/system/doc` 内容区 CSS 最大宽度仅 **1000px**，存在明显「像素过剩 → 打包冗余」。

本 change 是 `backend-prod-packaging-slim` 设计文档中 **Phase 2（docs/images 压缩）** 的独立落地：在 **不破坏 `/system/doc` 可读性** 的前提下，缩小 embedded JAR 与 Spring Native embedded 二进制中的文档静态资源体积。实测表明 **单纯等比 resize 而不做 palette 重压缩会增大文件**，须采用完整图片流水线。

## What Changes

- 为 `readme/production/images/**` 建立 **只缩小、不放大** 的宽度上限 **1600px** 优化流水线（ImageMagick/`sips` resize + **pngquant** + **oxipng**）
- 新增 **`npm run optimize:docs-images`**（及可选 CI/pre-commit 校验）：批量处理超宽图、输出体积报告
- **去重**：合并 `images/case/` 等与 `images/user-guide/...` 内容相同的 PNG（约 **1.3MB** 零清晰度损失收益），Markdown 引用统一到 canonical 路径
- **入库约束**：新截图宽度 MUST ≤ **1600px**（或经 optimize 脚本处理后再提交）；CI 断言禁止 `width > 1600` 的新增/变更 PNG 进入 `readme/production/images`
- 更新 **`backend-prod-packaging-slim` / `spring-native-delivery`** 体积验收：embedded JAR 与 Native static 段记录优化前后 `docs/images` 字节数
- **不**改变 `/system/doc` API、Markdown 目录结构、`CopyWebpackPlugin` 拷贝语义（仍整包 `readme/production` → `static/docs`）

## Capabilities

### New Capabilities

- `prod-docs-images-slim`：文档截图宽度规范、pngquant 优化流水线、重复资源去重、CI 体积与尺寸回归

### Modified Capabilities

- `backend-prod-packaging-slim`：补充 `docs/images` 体积上限与验收场景；Phase 2 自 Non-Goals 移出
- `spring-native-embedded-delivery`：Native embedded 构建链 MUST 复用同一套已优化的 `readme/production/images`（无额外 Native 特化逻辑）

## Impact

- **资源**：`readme/production/images/**`（批量重编码）、部分 `.md` 图片相对路径（去重后）
- **工具链**：`cat2bug-platform-ui/package.json` 脚本；可选 `scripts/optimize-docs-images.sh`；CI workflow 片段
- **文档**：贡献指南——截图导出建议（宽度 ≤1600、优先 PNG 或 WebP 规范）
- **体积预期（保守）**：`docs/images` **26MB → 18–22MB**（约 **4–8MB** 净减）；embedded JAR 总包约 **2–5%** 额外下降；对 Native embedded（static ~70MB）收益占比更高
- **风险**：pngquant 有损压缩可能导致细线/小字轻微变化；须 `/system/doc` 目检；错误地只做 resize 可能 **增大** 体积（已实测 +37%）
