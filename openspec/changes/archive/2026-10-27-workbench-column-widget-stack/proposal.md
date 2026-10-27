## Why

工作台编辑里一列目前只能放一个组件（拖入/点选会整列替换），但数据模型与部分预设已支持 `widgets[]` 列内堆叠。成员无法在编辑器里搭出「一列上下多个组件并可拖排序」的布局，和预设（如 manager 右侧源码+文档）体验不一致。

## What Changes

- 列正式以有序 `widgets[]` 存放多个组件实例；展示与编辑均为上下自动排列。
- 从目录拖入或选择器放入：目标列**默认追加**（非空列也可继续添加）；数量不设上限。
- 组件级拖拽：支持**列内改序**与**跨列移动**（落到目标列时追加到落点位置）；不再以「整列互换」为主要交互。
- 同列内 `type` **唯一**：选择器排除本列已有类型；拖入导致同列类型重复时提示且不落子。
- 行高：列共用行高；`content` 模式下行高取各列「组件高度之和 + 间距」的最大值；`row`/`fixed` 仍整行共用，溢出在列内处理。
- 支持单组件移除（列可留空）；更新 `workbench-row-layout` 规格中「每列一个 widget / 整列 swap」等过时要求。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `workbench-row-layout`: 列由单 widget 改为 `widgets[]`；放置改为追加；拖拽改为组件级列内/跨列；同列 type 唯一；明确 content 行高与列共用高度语义。

## Impact

- 前端：`WorkbenchRowEditor.vue`、`WorkbenchRows.vue`、`row-layout.js`（`placeWidgetInCol` 等）、相关 i18n。
- 配置：v3 `rows[].cols[].widgets` 已存在；无库表变更、无后端 API 契约变更（仍存整份布局 JSON）。
- 预设：已用 `stack` 的 manager 等布局与编辑器对齐，无需改语义。
- 文档：可引用 `docs/07-技术决策/ADR-021-工作台固定页与自定义私有共享页签.md`；不推翻 ADR。
