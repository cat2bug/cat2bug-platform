## 1. 列宽状态与持久化

- [x] 1.1 在 `defect-gantt.js`（或 gantt 状态层）引入每档可覆盖 `colWidth`，默认仍取 `GANTT_COL_WIDTH`；导出阈值常量（日/周/月 min·max）
- [x] 1.2 读写 `localStorage` 键 `defect.gantt.colWidths`（JSON `{ day, week, month }`）；无键时用默认宽
- [x] 1.3 工具条 `onScaleChange` 切档时应用该档记忆宽；与现有 `defect.gantt.scale` 共存

## 2. 格缝拖拽与阈值切档

- [x] 2.1 时间头 minor 格右缘增加约 6px `col-resize` hit 区；`pointerdown` `stopPropagation`，与框选/条拖隔离
- [x] 2.2 拖过程用 rAF 更新当前档 `colWidth`（同尺度全局同宽）；松手写入 `colWidths`
- [x] 2.3 实现双向阈值：缩过下限日→周→月，拉过上限月→周→日；切档后落默认宽并做短暂 hysteresis，避免抖振
- [x] 2.4 切档前取视口水平中心日期，重建 units 后调整 `scrollLeft` 使该日仍近中心
- [x] 2.5 切档时短 toast（`ElMessage` + i18n，如「已切换到周」）；工具条尺度 radio 与拖拽结果同步

## 3. 几何与缩略图跟随

- [x] 3.1 条几何 / 轴内容宽统一消费可变 `colWidth`（档内只改宽、不重建 units，切档才重建）
- [x] 3.2 列宽或尺度变化后 `$nextTick` 刷新缩略图 viewport 与 bar marks，确认与 `axisBodyScroll` 成比例

## 4. 验证与长期知识

- [x] 4.1 手动验证：档内拖宽、双向切档锚点、toast、工具条一致、缩略图跟随、与框选不抢手势
- [x] 4.2 回写长期知识：`docs/04-UI规范/组件与交互约定.md` 补甘特列宽拖拽一句；`docs/08-版本记录/2026-10.md` 当月一行；无新 ADR（勾掉并注明无）
