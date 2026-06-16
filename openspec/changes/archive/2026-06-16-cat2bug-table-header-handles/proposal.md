## Why

`Cat2BugTable` 已支持列拖拽排序、左固定（pin）与本地显隐/顺序缓存，但列宽仅使用默认值 `min-width`、用户无法调宽且不落盘；表头拖序入口隐式（整列 Sortable 且与 pin 区域冲突），体验不清晰。需要在表头提供明确的拖序与调宽手柄，并将列宽与显隐、固定、顺序一并写入本地缓存，且全站 `Cat2BugTable` 页面一次上线。

## What Changes

- 表头 **hover** 时在每列显示两个控件：**左侧**圆角矩形六点拖序手柄（在 pin **之前**）、**右侧**圆角竖条调宽手柄；**未 hover 时**拖序/调宽透明且不可点，但 **pin、标题、排序箭头位置不得位移**。
- **保留**现有表头 **pin**（`header-left` / `header-right`）点击切换左固定，**不**迁入「显示字段」弹层。
- 拖序仅通过左手柄触发（Sortable `handle`），与 pin 点击、列排序互不干扰。
- 用户调宽写入列配置 **`width`（px）**，与 `visible`、`fixed`、列顺序一并持久化到现有 `field-list` 本地缓存；最小列宽 **60px**。
- **排除**调宽：append 操作列（`cat2bug-operate-column`）、prepend 固定辅助列（如侧栏展开列）等；操作列仍走自动列宽逻辑。
- **第一期不同步** Excel 视图独立列宽缓存（`excel-col-widths`）；表格与 Excel 仍共享列顺序/显隐/固定。
- `mergeDefectTableColumnPrefs` 等合并逻辑读取并应用用户 `width`；无缓存宽度时回退 `TableOptions` 默认及多语言 `width_xx`。
- 暗色主题下手柄样式与表头 token 一致。

## Capabilities

### New Capabilities

- `cat2bug-table-header-handles`: `Cat2BugTable` 表头 hover 拖序/调宽手柄、布局稳定性、列宽持久化及与固定列双表头/自定义横条/操作列的协调。

### Modified Capabilities

- `table-required-column-header`: 表头 DOM 增加拖序/调宽占位与 hover 显隐后，必填列红色标题与 pin 的布局稳定性要求需扩展（两种表头结构仍须生效且位置不跳变）。

## Impact

- **前端**：`cat2bug-platform-ui/src/components/Cat2BugTable/index.vue`（表头模板、Sortable、调宽、样式）、`cat2bug.scss` / `theme-dark.scss`。
- **列缓存**：`defect-custom-field-columns.js`（`mergeDefectTableColumnPrefs`、`syncNewDefectTableColumnsIntoFieldListCache`）、`defect-display-field.js` 导出列配置若需含 width。
- **消费页**（一次全开）：缺陷/用例/计划/文档/报告列表及计划抽屉内 `CaseList` / `DefectList`（无单独开关，随 `Cat2BugTable` 升级）。
- **无后端/API 变更**；本地缓存 JSON 结构向后兼容（旧条目无 `width` 时按默认宽）。
