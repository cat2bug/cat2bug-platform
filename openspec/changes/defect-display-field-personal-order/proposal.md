## Why

缺陷列表「显示字段」只能勾选显隐，不能拖改顺序；表头虽可拖列，但 `mergeDefectTableColumnPrefs` 刷新后仍按团队 `orderedEnabledFieldKeys` 丢弃个人列序，与「表头拖序即个人偏好」的预期不符。需要让个人列序可持久，并在显示字段面板与表格/Excel 之间双向同步。

## What Changes

- 「显示字段」勾选列表支持拖动手柄重排（含未勾选字段）；松手后立即同步表格列序并写入个人缓存。
- 表头拖序与面板拖序共用同一套个人列序；刷新后仍保留。
- `mergeDefectTableColumnPrefs`（及导出/Excel 合并路径）：**个人缓存中的 key 顺序为权威列序**；团队 `sortOrder` / `orderedEnabledFieldKeys` 仅作默认序与新字段落点。
- Excel 视图「显示字段」同样可拖，并与表格共用用户隔离的 field-list 缓存（对齐 `u{userId}` 键）。
- **不改**团队「缺陷属性」管理页：其拖序/sortOrder 仍是全员默认顺序。
- **不改**新建/编辑表单与详情属性序：继续只跟团队 column-layout。
- 回写 `docs/04` 中「不用个人拖列覆盖字段管理顺序」的口径；无新 ADR（个人列表偏好覆盖默认列序，范围限于列表/Excel）。

## Capabilities

### New Capabilities

- `defect-display-field-personal-order`: 缺陷列表/Excel 显示字段与表头的个人列序持久化、面板拖序，以及与团队默认序的合并规则。

### Modified Capabilities

- （无）既有 OpenSpec 未单独规定「个人列序必须丢弃」；行为变更以本 capability + `docs/04` 为准。

## Impact

- 前端：`defect-custom-field-columns.js` / `defect-display-field.js`、`defect/list/table.vue`（column picker Sortable）、`defect/list/excel.vue`（缓存键与列序）、`Cat2BugTable` 与 `preserve-column-order` 协作；导出列若走同一 merge 则随个人序。
- 后端/API：无变更（团队 column-layout 不变）。
- 文档：`docs/04-UI规范/组件与交互约定.md`、`docs/08-版本记录/2026-10.md`。
- 不推翻 ADR-023（属性仍归属团队；个人仅覆盖列表展示序）。
