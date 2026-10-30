## Context

缺陷甘特（`DefectGantt`）时间轴现用固定 `GANTT_COL_WIDTH`（日 36 / 周 72 / 月 96）与工具条 radio 切换 `scale`。列宽不可调；缩略图已按 `axisBodyScroll` 内容尺寸映射。用户需要在轴上拖格缝连续改密度，并在过窄/过宽时双向自动切档。

## Goals / Non-Goals

**Goals:**

- 拖时间头**格缝**调节全局 `colWidth`（同尺度所有列同宽）。
- 每档有 min/max；缩过下限 → 日→周→月；拉过上限 → 月→周→日。
- 切档锚定视口中心日期 + 短暂提示；工具条尺度与拖拽一致；每档列宽本地记忆。
- 缩略图随轴宽/scroll 继续正确；格缝与框选/条拖隔离。

**Non-Goals:**

- 连续 `pxPerDay` 统一缩放模型（B 方案）；本期仍三档离散尺度 + 档内可变宽。
- 单列独立宽度、滚轮/捏合缩放（可后续）。
- 服务端偏好同步、第三方库。

## Decisions

1. **方案 A（档内可变宽 + 阈值切档）**  
   - 否决 B（连续 pxPerDay）：改动面大，与现有 `buildAxisUnits` / `barGeometry(scale)` 耦合深。  
   - `colWidth` 从「只读常量」变为每档可覆盖状态；默认仍取 `GANTT_COL_WIDTH`。

2. **阈值（初值，实现可微调）**  
   - 日：默认 36，约 `[22, 64]`；`<22` → 周；`>64` 不升档（已是最细）。  
   - 周：默认 72，约 `[44, 110]`；`<44` → 月；`>110` → 日。  
   - 月：默认 96，约 `[56, 160]`；不降档；`>140` → 周。  
   - 切档后新档列宽用该档默认或映射到默认附近，避免立刻再次触发阈值。

3. **切档锚点**  
   - 切档前取视口水平中心对应日期；切档并重算 units 后 `scrollLeft` 使该日仍近中心。  
   - 提示：`ElMessage` 或等价短 toast（「已切换到周」等 i18n）。

4. **手势**  
   - 时间头 minor 格右缘（或列间缝）`col-resize` hit 区约 6px；`pointerdown` `stopPropagation`。  
   - 拖过程 rAF 更新 `colWidth`；松手持久化。

5. **偏好**  
   - 保留 `defect.gantt.scale`；新增 `defect.gantt.colWidths` JSON `{ day, week, month }`。  
   - Radio `onScaleChange` 切档时应用该档记忆宽（无记忆则默认）。

6. **缩略图**  
   - 不改协议：`axisWidthPx` / scroll 变化后现有 `updateMinimapViewport` + `minimapBarMarks` 自然更新；切档后 `$nextTick` 刷一次即可。

7. **ADR**  
   - 不新增 ADR。

## Risks / Trade-offs

- [切档列数突变导致跳动] → 中心日期锚点 + 切档后新档用默认宽避开阈值带。  
- [与框选抢手势] → 仅头部分缝可拖，`stopPropagation`。  
- [拖时频繁重排性能] → rAF；units 仅在切档时重建，档内只改宽。  
- [阈值抖振] → 切档后跳到默认宽并短暂忽略反向阈值（或 hysteresis）。

## Migration Plan

1. 前端上线；无 `colWidths` 键时用常量默认。  
2. 回滚：去掉格缝拖与阈值逻辑即可。

## Open Questions

- 无（探索已确认：双向升/降档、全局列宽、切档提示、缩略图跟随）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（甘特尺度；实现后补列宽拖拽一句）
- `docs/08-版本记录/2026-10.md`（合入后当月一行）
- `openspec/specs/defect-list-gantt-view/spec.md`
- `openspec/specs/defect-gantt-minimap/spec.md`
- 拟新增 ADR：无
