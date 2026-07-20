# Testing — national-standard-report-templates

## OCR 抽检

- [x] `_sources/ocr/gb-t-38634-{1,2,3,4}/manifest.json` 存在，且 `pages/` 页数与 `totalPages` 一致（53/54/111/127）
- [x] 第 3 部分目录/术语可读：`测试计划` / `测试状态报告` / `测试完成报告` / `信息项` / `剪裁符合`（`front-toc.md`；`qa.tocReadable=true`）
- [x] 失败/弱页登记：Part3 p2,55,57,58,96；Part4 p2,4,127（见各册 `manifest.json` → `qa.failedPages`；多为空白扉页或图多页）
- [x] 分段读取协议已写入 `SKILL-INDEX.md` / overview

## 模版预览 / DSL

- [x] `defaults/README.md` 清单与 `taxonomy.md` 一致
- [x] 单元测试：`cd cat2bug-platform-ui && npm run test:unit -- --testPathPattern=national-standard-defaults-dsl`（10 passed）
- [ ] 可选：在报告模版编辑器中粘贴 `test-status-report.md` / `test-completion-report.md` / `test-incident-report.md`，确认变量与 `$table` 可预览
- [x] 确认模版未发明未登记的 `${api...}`（单测 `assertKnownTokens`）

## Skills 索引

- [x] `readme/development/skills/SKILL-INDEX.md` 链到 standards、dsl、taxonomy、defaults

## 可选库 seed

- [x] 本变更默认不导入 `sys_report_template`；若产品需要预置，另开变更
