## Why

缺陷甘特二期已能看见处理人负荷过载与 FS 依赖冲突，但消除过载仍靠人手一条条拖开。需要 OmniPlan 式「自动排列」体验：开启后拖动锚定条时，同人重叠的可改条自动往后挤，避免落点资源冲突，且不颠倒显式依赖语义（本版拖挤以锚定条为钉，同伴只后移）。

## What Changes

- **自动排列开关**：甘特「图层」下拉内勾选「自动排列」（本地键 `defect.gantt.layers.autoLevel`）；需 `system:defect:edit`，无权限则禁用。
- **拖动推挤**：开关开启且单条拖移/改期时，同主处理人（`handleBy[0]`）泳道上与锚定落点重叠、且当前用户可改的未关闭已排期条，工期不变地往后挤到锚定条（及已挤条）之后；拖动中预览，松手与锚定条一并写回。
- **权限**：项目创建人 / `project.admin` 可推全部可改同伴；其他人仅可推自己 ∈ `handleBy` 的条。无权改与已关闭视为空气（不挡推挤）。
- **不做**：一键全图 ASAP 预览确认；多选批量平移时推挤；改处理人/交付物/工期；把空气当硬障碍；服务端求解器。
- **不推翻**既有 ADR；属甘特交互扩展。

## Capabilities

### New Capabilities

- `defect-gantt-auto-level`: 缺陷甘特自动排列——图层开关、拖动同人重叠后移、权限过滤与写回

### Modified Capabilities

- （视图/二期入口文案与分工由后续变更 `defect-gantt-level-resources` 定稿；本变更只落地拖动推挤能力本身）

## Impact

- 前端：`gantt.vue` / `defect-gantt.js`（`pushOverlapsAfterPinned` 等）；写回复用 `updateDefect`。
- 后端：无新接口。
- 文档：`docs/04` 甘特段、`docs/08` 当月一行。
