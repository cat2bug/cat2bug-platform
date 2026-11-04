## Why

`defect-display-field-personal-order` 已让列表/Excel「显示字段」个人列序权威，但新建/编辑缺陷表单仍只跟团队 `orderedEnabledFieldKeys`。用户在列表里排好的字段顺序无法带到录入页，录入体验与个人习惯不一致。

## What Changes

- 新建/编辑缺陷表单字段顺序：**优先**读用户隔离的「显示字段」field-list 缓存 key 序；**无个人缓存时**回退团队 `orderedEnabledFieldKeys` / column-layout。
- 表单**显隐**仍只跟团队启用配置（`isBuiltinDefectFieldVisible` / 自定义 `enabled`），不因列表勾选隐藏而少填必填属性。
- 详情属性序本变更不改（仍团队权威）；若产品后续要对齐再开变更。
- 回写 `docs/04` 与踩坑记录中「表单不读个人缓存」的口径。

## Capabilities

### New Capabilities

- `defect-form-personal-field-order`: 新建/编辑缺陷表单字段顺序：个人显示字段序优先，团队属性配置序为回退。

### Modified Capabilities

- （无）`defect-display-field-personal-order` 尚未归档进 `openspec/specs/`；行为延伸以本 capability 为准。

## Impact

- 前端：`defect-form-field-order.js`、`DefectFormOrderedFields.vue`（打开时重算）、单测；可选轻微触及 Add/Edit 打开钩子。
- 后端/API：无。
- 文档：`docs/04-UI规范/组件与交互约定.md`、`docs/06-踩坑记录/缺陷属性团队配置后其他账号仍像默认.md`、`docs/08-版本记录` 当月一行。
- 不推翻 ADR-023（属性归属团队；个人仅覆盖展示/录入序）。
