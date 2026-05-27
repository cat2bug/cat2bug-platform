## Why

缺陷列表页 Tab 行右侧有统计入口图标，但 Element UI 的 Tab 底部分割线仅覆盖 `el-tabs` 区域，右侧工具区下方出现空白，视觉不连贯。

## What Changes

- 在 `.defect-tools-tab` 容器上绘制全宽底部分割线
- 隐藏 `el-tabs` 内置 `nav-wrap::after`，避免双线
- 微调右侧工具区垂直对齐，使图标与 Tab 底线一致

## Capabilities

### New Capabilities

- `defect-tab-row-border`: 缺陷页 Tab 行（含右侧工具按钮）底部分割线全宽连续

### Modified Capabilities

（无）

## Impact

- **前端**：`cat2bug-platform-ui/src/views/system/defect/index.vue` 样式
