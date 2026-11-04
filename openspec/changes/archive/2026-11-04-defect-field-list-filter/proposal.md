## Why

团队「缺陷属性管理」列表字段较多（系统 + 自定义常达数十条），仅靠翻页难以快速定位。需要按来源筛选，并对属性键与显示名称做模糊搜索，减少找字段成本。

## What Changes

- 缺陷属性列表工具条左侧增加**来源下拉**（全部 / 系统 / 自定义）与**属性名模糊搜索框**（同时匹配属性键与显示名称）。
- 「新增属性」移到工具条右侧，整体对齐列表页工具条约定（左过滤、右动作、改值即查）。
- 过滤在前端对已拉取的全量目录生效；分页 `total` 与当前页数据跟随过滤结果。
- 过滤条件变化时重置到第 1 页；空结果展示空表与「共 0 条」，不新增后端查询参数。

## Capabilities

### New Capabilities

- `defect-field-list-filter`: 团队缺陷属性管理列表的来源筛选与属性名（键 ∪ 显示名）模糊搜索行为

### Modified Capabilities

- （无）目录存储、权限与项目只读消费仍由既有 `defect-field-catalog` / `team-case-defect-settings` 约束，本变更不改其需求语义。

## Impact

- 前端：`cat2bug-platform-ui/src/views/system/team/option/defect-fields/index.vue`（工具条、本地 filter、分页 computed）；相关 i18n（7 语）。
- 样式：复用 `.list-page-toolbar`（`project-list-page.scss`），对齐团队成员 / 用例类型等列表页。
- 后端 / API / DB：无变更；继续 `GET /system/team/{teamId}/defect-fields` 全量列表。
- 权限：不变（仍 `system:team:defect-field:*`）。
