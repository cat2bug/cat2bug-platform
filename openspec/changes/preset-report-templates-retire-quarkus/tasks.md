## 1. 预制资源与预览流水线

- [x] 1.1 确定并创建 `cat2bug-platform-admin/src/main/resources/报告/模版/` 目录布局（`29119-3/`、`9001/manuals/`、`9001/records/`）
- [x] 1.2 编写同步脚本：从 `readme/development/skills/report-templates/{29119-3,9001/manuals,9001/records}` 复制业务 `.md` 到 resources（排除 SKILL/README/overview 等非模版）
- [x] 1.3 编写 HTML→PNG 批生成（Playwright 或同等）：每 md → 固定样式 HTML → PNG
- [x] 1.4 生成/更新 `manifest.json`（groups、key、title、contentPath、previewPath）
- [x] 1.5 将同步+截图接入可重复执行的 npm/shell 入口，并在 README/skills 或 scripts 说明何时重跑

## 2. 后端 API（Spring admin）

- [x] 2.1 实现分组 picker API（自定义 DB + manifest 预制）
- [x] 2.2 实现预制 content / preview 读取（classpath）
- [x] 2.3 实现「复制预制 → 自定义」接口（写 `sys_report_template`）
- [x] 2.4 生成报告支持直接使用预制 key 初始化正文（若与现 addReport 耦合则扩展参数）
- [x] 2.5 确保 update/delete 无法作用于预制；单测或集成测覆盖拒绝路径

## 3. 前端选择器与管理页

- [x] 3.1 `Cat2BugReportTemplateSelect` 三组布局与 i18n 标题
- [x] 3.2 预制卡片仅复制；隐藏编辑/删除；添加仅自定义
- [x] 3.3 调整分页/滚动与 `report-template-popover-kbd` 以适配分组
- [x] 3.4 模版管理页同步只读/复制规则，避免旁路编辑预制
- [x] 3.5 预览图绑定 manifest/preview URL；缺失时回退占位图

## 4. 退役 Quarkus 交付

- [x] 4.1 全仓检索活跃 Quarkus 交付引用（排除 `openspec/changes/archive`）；列出清单
- [x] 4.2 更新 `CLAUDE.md`、根 `README.md`、相关 `readme/**`：默认后端改为 Spring admin
- [x] 4.3 删除或改写 `deploy/*quarkus*`、`build-embedded-quarkus`、UI `build:embedded-quarkus` 等活跃入口
- [x] 4.4 确认 Maven 默认构建不依赖 Quarkus 模块；清理残留未跟踪 quarkus 目录（若存在）
- [x] 4.5 CI/Docker 文档与脚本去掉 Quarkus 必选路径

## 5. 验证与收尾

- [ ] 5.1 手工：打开生成报告选择器，三组顺序与只读/复制行为符合 spec
- [ ] 5.2 手工：复制预制后可编辑删除；生成报告可用预制
- [x] 5.3 对照 manifest 与 skills 全量模版清单无遗漏（30 = 9+21；`ReportPresetManifestCoverageTest`）
- [x] 5.4 补充或更新前端/后端自动化测试（picker DTO、复制、守卫）
- [x] 5.5 编写 change 内 `TESTING.md` 清单
