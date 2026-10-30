## Context

缺陷甘特（`DefectGantt`）已具备左列表 + 右时间轴、`axisBodyScroll` 双向滚动、图层下拉（关联线 / 关键路径 / 资源负荷 / 基线叠加）及本地 `defect.gantt.layers` 等偏好。轴内容常宽于可视区，滚动条导航成本高。本变更在同一自研壳上增加时间轴缩略导航，不引入第三方库，不新增 API。

## Goals / Non-Goals

**Goals:**

- 固定尺寸浮层缩略图：灰底 + **每行状态色矮条** + 红框映射 `axisBodyScroll` 视口。
- 图层菜单「缩略图」开关默认开；× 关闭与开关同步；偏好本地记忆。
- 拖标题改面板位置并本地记忆；拖内容/红框改轴 scroll；与框选隔离。
- 只导航时间轴，不驱动左侧列表独立 scroll、不导航未排期池；色块不触发选中。

**Non-Goals:**

- 缩略图内绘制关键路径 / 依赖线 / 基线条。
- 用户拉角缩放面板、多显示器贴边吸附策略深化。
- 服务端偏好同步。

## Decisions

1. **锚点容器**  
   - 浮层挂在甘特「已排期主区」（`defect-gantt-scheduled` 或等价 root）`position: relative` 内，默认 `right/bottom` 贴角；有记忆坐标时用 `left/top`（钳制在主区内）。  
   - 备选：`position: fixed` 相对视口——否决（与页面其它滚动/抽屉错位）。

2. **内容映射只认轴滚动容器**  
   - 比例：`scroll / contentSize * miniSize`；拖移反算 `scrollLeft/Top`，纵向继续走现有 left↔axis `scrollTop` 同步。  
   - 内容宽或高 ≤ 可视时：红框可接近铺满；仍可显示面板（默认开）。

3. **条块绘制（二期补齐）**  
   - 遍历与主轴相同的组/行布局（含折叠跳过、负荷轨占位、处理人镜像**每行都画**），用现有 `barGeometry` 得 `x/width`，`y` 累加组头+行高。  
   - 映射到 mini：`left/width/top/height = 轴坐标 / contentSize * miniSize`；宽保底约 2px，高约为行带比例且至少 1px。  
   - 颜色用主条同一套 `state-*`；色块 `pointer-events: none`，命中仍归 body 导航（只改 scroll，不选缺陷）。  
   - 红框叠在色块之上。

4. **偏好键**  
   - `showMinimap` 并入现有 `defect.gantt.layers` JSON（缺省 `true`），或并列 `defect.gantt.minimap` + `defect.gantt.minimapPos`；实现选一种并在 tasks 写清。面板位置 `{ x, y }` 单独键。  
   - 缺省：无记忆时 `showMinimap === true`，位置右下。

5. **手势隔离**  
   - 缩略图 `mousedown`/`pointerdown` `stopPropagation`，避免触发 `onAxisBlankDown` 框选。  
   - 标题拖与内容拖用不同 hit 区；拖过程 `pointer-events` / `user-select` 防选中文字。

6. **ADR**  
   - 不新增 ADR（自研甘特壳交互扩展，不改变「不引入 GPL/商用甘特库」决策）。

## Risks / Trade-offs

- [轴内容高度变化导致红框跳动] → 监听 scroll + ResizeObserver/已有 viewport measure，按帧或 rAF 更新。  
- [默认开在空数据时占位] → 接受；空态轴仍可滚动/通底，红框近满幅。  
- [拖面板拖出可视] → 坐标钳制在主区 padding 内。  
- [与跟手幽灵 z-index 冲突] → 缩略图低于 dialog、高于轴条即可。  
- [行很多时 DOM 色块偏多] → 仅已展开组的已排期行；甘特本身有页 cap，可接受。

## Migration Plan

1. 前端加面板与图层项；旧客户端无 `minimap` 键时默认开。  
2. 回滚：去掉面板与菜单项即可，无库表。

## Open Questions

- 无（已确认：默认开、只导航轴、固定尺寸、图层入口；色块=每行都画、只导航不选中）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（甘特视图与图层约定；实现后补缩略图一句）
- `docs/08-版本记录/2026-10.md`（合入后当月一行）
- `openspec/specs/defect-list-gantt-view/spec.md`
- `openspec/specs/defect-list-gantt-phase2/spec.md`
- 拟新增 ADR：无
