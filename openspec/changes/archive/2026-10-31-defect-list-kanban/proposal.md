## Why

缺陷列表已有表格 / Excel / 甘特，缺少按状态泳道推进的敏捷看板视角；日常「修复 → 验证 → 关闭/重开」仍要进行内菜单或批量工具，无法一眼扫进度并拖拽改态。需要在缺陷页增加「看板」视图，与现有视图切换并列，拖拽落点复用既有流转动作与权限，避免再造一套状态机。

## What Changes

- 缺陷页内容视图 radio 在甘特后新增「看板」；本地键沿用 `defect.contentViewComponent` 白名单扩展；共用父级 `queryParams` 筛选与页签范围。
- 三泳道：`处理中`（`PROCESSING` + `REJECTED`，角标区分、列内不互拖）、`待验证`（`AUDIT`）、`已结束`（`CLOSED`）；遗留态 `RESOLVED` 不进泳道（与表格一致视为不可流转死态，列表若筛到可忽略或仅只读提示，不作为拖拽目标）。
- 跨泳道拖拽映射既有单条流转 API（批量多选时按「每条均可」求交后串行调用）：
  - 处理中 → 待验证：`repair`（须弹审核人；描述可选）
  - 待验证 → 处理中：`reject`（描述必填）
  - 任意未关闭 → 已结束：优先 `pass`（仅待验证且当前处理人有 `pass`），否则 `close`（有 `close` 权限，描述必填）；皆不可则拒绝落点
  - 已结束 → 处理中：`open`（描述必填）
- 动作可见性与表格按钮严格同一套（`defect-action-visibility` / 权限码 + 处理人身份）；不新开后端权限点。
- 支持多选后整组拖；落点非法整组回弹并提示。
- 指派：卡片菜单/快捷入口 + 多选时复用批量指派，打开现有 `AssignDialog`（态不变）；一期不做拖到成员 chips。
- 松手后按动作复用现有 DefectTools / 批量弹框；取消弹框则卡片回到原泳道。
- 不引入新状态、不改 `requireTransition` 规则；不推翻现有 ADR。命名用「看板」，与已下线的工作台「看板」页签无关。

## Capabilities

### New Capabilities

- `defect-list-kanban`: 缺陷列表看板视图的入口、三泳道分桶、卡片展示、跨泳道拖拽与动作映射、多选拖拽、指派入口及弹框闭环

### Modified Capabilities

- （无）`defect-batch-operations` / 既有流转规格不改需求语义；看板仅复用其 API 与前端可见性规则

## Impact

- 前端：`views/system/defect/index.vue` 视图切换与组件注册；新增 `list/kanban.vue`（或等价）；复用 `defect-action-visibility`、`DefectTools/*Dialog`、`DefectBatchActionDialog` 模式与 `/system/defect/{id}/{action}`。
- 后端：无新接口硬性需求；列表仍走 `GET /system/defect/list`（可按 `params[defectStates]` 或客户端分桶）；加载策略在 design 定（分段/较大 pageSize）。
- 文档：合入后回写 `docs/04-UI规范/组件与交互约定.md` 缺陷视图切换；`docs/08-版本记录` 当月一行。
