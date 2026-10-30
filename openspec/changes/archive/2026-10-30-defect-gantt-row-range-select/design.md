## Context

甘特二期已有 `marqueeSelectedIds` + 轴空白框选，以及右侧条 mousedown 上 Ctrl/⌘/Shift 一律当作加减选。左侧行头走 `focusGanttRow` → `selectDefect` 无修饰键，永远单选。批量平移依赖多选集合，左侧无法高效选相邻行。

约束：Vue 3 Options API 的 `gantt.vue`；不改缺陷 API；与现有框选、拖避让「多选不推挤」共用同一选中集合。

## Goals / Non-Goals

**Goals:**

- 左右已排期行点选语义对称（Ctrl/⌘ 加减、Shift 区间、Ctrl/⌘+Shift 并入区间）
- 区间按可见扁平行序、可跨组；折叠组内不进；镜像行按 defectId 去重
- 未排期池仍单选

**Non-Goals:**

- 未排期池多选 / 跨池与已排期的区间
- 键盘方向键扩展选区、全选快捷键（可后续）
- 改框选几何或批量写库协议

## Decisions

1. **统一入口 `selectDefect(defect, { additive, range, mergeRange })`**  
   左侧 click 与右侧条 mousedown 都走同一方法。无修饰：单选并设锚点，左侧可继续 scrollIntoView。`additive`（Ctrl/⌘）：toggle 并更新锚点到当前。`range`（Shift）：用锚点与当前在「可见已排期扁平行序」上做闭区间，替换 `marqueeSelectedIds`。`mergeRange`（Ctrl/⌘+Shift）：区间并入集合。  
   **备选**：左右各写一套 — 否决，易漂移。

2. **可见扁平行序**  
   遍历当前 `groups`（未折叠组的 `rows`），按列表自上而下收集唯一 `defectId`（镜像只留首次）。区间索引在该数组上取 `[min,max]`。跨组自然包含。  
   **备选**：仅同组内区间 — 否决（产品要求可跨组）。

3. **锚点 `selectionAnchorId`**  
   无修饰单选 / Ctrl 加减后设为当前 id；纯 Shift 区间不移动锚点（与 Finder 一致）；清空选中时清锚点。无锚点时 Shift 退化成单选。

4. **Shift 不再当加减**  
   右侧条原先把 Shift 与 Ctrl 同等对待；本变更改为区间。框选启动时若按住 Shift，仍可保留「叠加框选」既有行为（与点选 Shift 语义不同：框选是拖势），实现时在 `startMarquee` 保持 additive 含 Shift，点选路径单独分支。

5. **未排期池**  
   池行 click 继续 `selectDefect` 单选（或不进 `marqueeSelectedIds` 多选语义）；不响应区间。批量平移候选本来就只含已排期条。

## Risks / Trade-offs

- [Shift 用户习惯曾是加减] → 规格与 docs 写明；Ctrl/⌘ 仍是加减  
- [折叠组导致区间「跳空」] → 预期：只含可见行  
- [分段加载未进视口的行不在 groups] → 区间只覆盖已加载可见行；与框选一致  
- [左侧多选仍触发打开详情] → 确认无修饰单击/双击策略与现条行为对齐，修饰键 click 不 emit 详情

## Migration Plan

纯前端行为；无迁移。回滚即还原点选分支。

## Open Questions

（无 — 产品已拍板跨组、池外、Finder/Excel 修饰键）

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`
- `docs/08-版本记录/2026-10.md`
- 无新 ADR
