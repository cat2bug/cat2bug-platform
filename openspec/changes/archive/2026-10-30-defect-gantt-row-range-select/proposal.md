## Why

甘特已支持轴空白框选与右侧条上的修饰键加减选，但左侧行头仍只能单选；且 Shift 与 Ctrl 同为加减，缺少 Finder/Excel 式「锚点到当前」的区间选。批量改期依赖多选集合，左侧无法高效框选相邻多行会拖慢操作。

## What Changes

- 左侧已排期行头与右侧条使用同一套点选语义：无修饰键单选；Ctrl/⌘ 加减；Shift 按可见扁平行序做闭区间选（可跨组）；Ctrl/⌘+Shift 将区间并入已有选中
- 维护选择锚点；折叠组内行不进区间；多处理人镜像行按 `defectId` 去重
- 未排期池保持单选，不进本变更的多选/区间语义
- 左侧修饰键点选不强制滚轴定位（避免多选时视口乱跳）；无修饰键单选可保留现有 focus/滚动
- 框选与拖已选条批量平移沿用现有 `marqueeSelectedIds` 集合，行为不变

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `defect-list-gantt-phase2`: 扩展 Multi-select 要求——左右对称修饰键点选；Shift 区间选（跨组、可见行序）；明确与框选/未排期池边界

## Impact

- 前端：`gantt.vue` 左侧 `focusGanttRow` / 右侧条 mousedown 的选中路径；选择锚点与区间扫描辅助函数
- 无后端 API / 库表变更
- 文档：回写 `docs/04-UI规范/组件与交互约定.md`、`docs/08-版本记录`
