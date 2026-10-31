## Why

无人新建的缺陷现在仍是 `PROCESSING`，只靠看板「未指派」投影区分；验证「通过」又与手动「关闭」都落到 `CLOSED`，四种列表视图无法用正式状态表达「已创建 / 通过」。在处理人已可空、未指派泳道已落地之后，需要把这两种语义收回状态机，并在表 / Excel / 甘特 / 看板统一体现。

## What Changes

- **新增状态 `CREATED`（已创建）**：仅表示「尚未指派处理人」的活跃缺陷。无人新建 → `CREATED`；有人新建 → `PROCESSING`。
- **复活 `RESOLVED` 为「通过」**：**BREAKING（行为）**——`pass` 目标态从 `CLOSED` 改为 `RESOLVED`；文案由「已解决」改为「通过」。`RESOLVED` 重新进入状态配置/下拉（此前隐藏）。
- **`CLOSED`（已关闭）** 仅表示手动关闭、不再跟进；与「通过」并列终态，**不要求**通过后再关闭。
- **指派**：从 `CREATED` 指派时 MUST 写入处理人并跃迁到 `PROCESSING`（不能只改人、状态仍停在已创建）。
- **看板四列**：已创建（`CREATED`）｜处理中（`PROCESSING`+`REJECTED`）｜待验证（`AUDIT`）｜已结束（`RESOLVED`+`CLOSED`，卡片角标/状态旗区分通过与关闭）。取消「空 handleBy 投影」作为未指派入列依据（左列改挂 `CREATED`）。
- **表 / Excel / 甘特**：状态筛选、展示、条色覆盖 `CREATED` 与 `RESOLVED`（通过）。
- **历史数据**：不迁移既有 `CLOSED`（含当年 pass 落入关闭的单）；不批量改写旧库。
- **序号**：`CREATED` 追加为码 **5**（不位移既有 0～4）；`RESOLVED` 仍为码 **2**。不推翻现有 ADR；可增补术语表。

## Capabilities

### New Capabilities

- `defect-created-and-passed-states`：已创建 / 通过 状态语义、创建默认态、pass/close/assign 流转、四视图与看板分桶

### Modified Capabilities

- `defect-enum-fields`：`defectStateName` 合法集合纳入 `CREATED`，并重新暴露 `RESOLVED`
- `mcp-usage-guide`：缺陷状态表与 pass 后状态改为通过（`RESOLVED`）

## Impact

- 后端：`DefectConstants` / Open API 枚举、`SysDefectService` 创建默认态与 `pass`/`assign`/`open` 校验、配置 `states` 列表、导入解析、MCP 指南文案。
- 前端：状态旗/i18n、列表筛选、Excel 下拉、甘特色、看板 `laneOf`（已创建列取代空人投影）、动作可见性、批量操作。
- 文档：`docs/01-产品需求/术语表.md`、`docs/04-UI规范/组件与交互约定.md`、`docs/03-技术方案/MCP接入与权限.md`、`docs/08-版本记录`；关联既有变更 `kanban-unassigned-lane`、`defect-handle-by-optional`。
