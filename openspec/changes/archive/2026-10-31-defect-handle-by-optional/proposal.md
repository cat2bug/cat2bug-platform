## Why

缺陷看板已有「未指派」泳道（`kanban-unassigned-lane`），但创建/编辑表单、Excel 与内置字段元数据仍把处理人（`handleBy`）标为必填，正常建单永远进不了该列，泳道沦为空壳。要把未指派做成「先记下来再认领」的收件箱，必须把**建单与编辑保存**时的处理人改为非必填；状态流转里选下一任处理人仍保持必填。

## What Changes

- **创建 / 编辑缺陷**：`handleBy` 改为可选；空值以 `null` 或空数组持久化均可，读取侧按「无处理人」统一判断。
- **内置字段元数据**：`handleBy` 的 `required` 从必填改为非必填；仍保留在 `ALWAYS_ENABLED`（字段始终展示/不可关闭，但非必填）。
- **Excel 导入/模板**：处理人列改为非必填；空处理人允许导入活跃态缺陷。
- **列表表头**：缺陷列表处理人列去掉 `required: true` 红标（与表单对齐 `list-required-column-header`）。
- **不变**：指派（`assign`）、提测/修复选审核人（`repair`）等**动作弹框**里选人仍必填；`repair`/`reject`/`pass` 仍要求当前用户是处理人；无人时仅创建人/有指派权者可 `assign`（已有可见性）。
- **看板**：保留「未指派」泳道与只出不进/`assign` 出列行为；无处理人的活跃卡入该列。创建可不选人 → 落未指派。
- 不推翻现有 ADR；无新权限点、无新 API 形状（MCP create 本就可空，与网页对齐）。

## Capabilities

### New Capabilities

- `defect-handle-by-optional`：创建/编辑/Excel/内置字段上将处理人改为可选；动作弹框与流转处理人规则保持；与未指派泳道入列闭环

### Modified Capabilities

- （无独立主规格硬编码「处理人必填」需改写）`list-required-column-header` 已约定「表头红标跟随表单必填」——实现上改缺陷列默认配置即可，不合入 delta。看板行为由既有 `kanban-unassigned-lane` 承接。

## Impact

- 前端：`AddDefect.vue`、`EditDefectDialog.vue`、`HandleDefect.vue` 校验规则；`table-options.js` 处理人列；Excel 单元格校验文案路径；`defect-field-layout.js` / 字段管理展示若暴露 required。
- 后端：`DefectBuiltinFieldRegistry`（`handleBy` required=0）；`DefectExcelColumnSupport` 处理人列非必填；导入校验与既有空列表容忍对齐；确认创建接口不因空 `handleBy` 拒绝。
- 文档：`docs/04-UI规范/组件与交互约定.md`（必填列先例、看板未指派前提）；`docs/08-版本记录` 当月一行。
- 关联已合入变更：`kanban-unassigned-lane`（前提从「可空」改为产品真实可空）。
