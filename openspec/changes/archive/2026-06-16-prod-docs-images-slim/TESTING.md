# prod-docs-images-slim 验收清单

## 前置

- 安装 `pngquant`、`oxipng`（见 `design.md`）
- 基线记录：`du -sb readme/production/images`

## 脚本

```bash
# 干跑报告
npm run optimize:docs-images -- --dry-run

# 全量优化（确认 dry-run 后）
npm run optimize:docs-images

# 门禁
npm run check:docs-images
# 期望 exit 0；无 pixelWidth > 1600
```

## 功能

- [ ] 登录后打开 `/system/doc` → **用户指南** → 安装向导/setup 步骤：图片清晰可读
- [ ] 打开含 **API code-tabs** 的文档页：截图与代码块布局正常
- [ ] 打开 **缺陷/用例** 相关页：3600 源图替换后无肉眼模糊（小字可辨）
- [ ] 浏览器 Network：`GET /docs/images/...` 均 200
- [ ] 打印当前文档：图片不溢出、不裁切异常

## 体积

```bash
# 源目录
du -sb readme/production/images
# 期望 ≤ 22MB（22 * 1024 * 1024 字节）

# embedded JAR
cd cat2bug-platform-ui && npm run build:embedded
cd .. && mvn -pl cat2bug-platform-admin -am package -Pembedded -DskipTests
jar tf cat2bug-platform-admin/target/cat2bug-admin.jar | grep 'static/docs/images/' | head

# 解压统计 JAR 内 images（示例）
mkdir -p /tmp/jar-docs && cd /tmp/jar-docs
jar xf /path/to/cat2bug-admin.jar BOOT-INF/classes/static/docs/images
du -sb BOOT-INF/classes/static/docs/images
# 期望 ≤ 22MB；且 JAR 总大小相对 Phase 1 额外 ≥ 3MB 下降
```

## 回归

- [ ] JAR 内仍 **无** `static/stats.json`
- [ ] `api-only` 构建 **仍无** `static/**`（本 change 不修改 api-only 语义）
- [ ] 去重后无 Markdown 死链（`rg '\]\(.*images/' readme/production` 抽样）

## CI

- [ ] 故意提交 `width=3600` 测试 PNG → `check:docs-images` / CI **应失败**
- [ ] 仅改 `.md` 的 PR → images check **通过或跳过**
