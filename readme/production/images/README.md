# 文档截图规范

本目录为 `/system/doc` 用户文档配图，构建时拷贝至 `static/docs/images/**` 并打入 embedded JAR / Native 产物。

## 导出建议

| 项 | 建议 |
|----|------|
| 宽度 | **1280–1600px**（不得超过 **1600px**） |
| 格式 | PNG（UI 截图） |
| 禁止 | 3600px 全屏 Retina 原图直接提交 |

Playwright 截图示例：

```javascript
const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
```

## 维护命令

依赖（macOS）：

```bash
brew install imagemagick pngquant oxipng
```

```bash
cd cat2bug-platform-ui

# 检查宽度门禁
npm run check:docs-images

# 去重 / 批量优化
npm run dedupe:docs-images:dry-run
npm run dedupe:docs-images
npm run optimize:docs-images:dry-run
npm run optimize:docs-images
```

脚本位于 `deploy/scripts/`（`check-docs-images.sh`、`optimize-docs-images.sh`、`dedupe-docs-images.sh`）。

## 流水线说明

1. 仅当 `pixelWidth > 1600` 时等比缩小至 1600（**禁止放大**）
2. `pngquant --quality=65-90 --skip-if-larger`
3. `oxipng -o max --strip safe`
4. 若优化后体积大于原文件，保留原文件

详见 OpenSpec change `prod-docs-images-slim`。
