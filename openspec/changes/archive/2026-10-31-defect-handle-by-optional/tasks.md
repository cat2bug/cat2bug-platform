## 1. 后端与字段元数据

- [x] 1.1 `DefectBuiltinFieldRegistry`：`handleBy` 的 `required` 改为 0，保留 `ALWAYS_ENABLED_FIELD_KEYS`
- [x] 1.2 `DefectExcelColumnSupport`：处理人列改为非必填；确认导入空处理人可成功
- [x] 1.3 核对创建/更新缺陷 API：空 `handleBy` 不返回校验错误；必要时修测试断言

## 2. 前端表单与列表

- [x] 2.1 `AddDefect.vue` / `EditDefectDialog.vue` / `HandleDefect.vue`：去掉 `handleBy` 的 required 规则（保留字段展示）
- [x] 2.2 `table-options.js`：处理人列去掉 `required: true`
- [x] 2.3 Excel 前端校验（若有「处理人不能为空」硬拦截）：改为允许空
- [x] 2.4 确认 `AssignDialog` / `RepairDialog` / 批量指派弹框选人仍必填，未误改

## 3. 验收与文档

- [x] 3.1 手工：不选处理人创建 → 看板「未指派」出现；指派后进入对应状态列；选处理人创建仍进状态列
- [x] 3.2 手工：指派/修复弹框空人选人仍拦；列表处理人表头不再红标
- [x] 3.3 回写 `docs/04-UI规范/组件与交互约定.md`（必填列先例、未指派前提）；`docs/08-版本记录/2026-10.md` 一行；无新 ADR / 无新踩坑则注明
