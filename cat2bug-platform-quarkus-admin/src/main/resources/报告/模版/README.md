# 预制报告模版（classpath）

Spring admin 打包的只读 ISO 预制模版：Markdown 正文 + 预览 PNG + `manifest.json` 清单。

**不要手工改此处 Markdown/PNG**（会被同步脚本覆盖）。源文件在：

- `readme/development/skills/report-templates/29119-3/`
- `readme/development/skills/report-templates/9001/manuals/`
- `readme/development/skills/report-templates/9001/records/`

更新正文后，在仓库根目录执行：

```bash
./scripts/sync-report-preset-templates.sh
```

脚本会复制 `.md`、生成预览图并刷新 `manifest.json`。
