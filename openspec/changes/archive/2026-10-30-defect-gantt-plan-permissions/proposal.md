## Why

甘特拖条、落池、批量平移目前只要有 `system:defect:edit` 就能改任意缺陷计划，与已落地的自动排列/拖避让「管理员改全部、其他人只改自己处理的」不一致；更新基线同样过宽。需要把节点写与基线/工作日历写收紧到项目创建人/项目管理员特权，避免普通成员改动他人排期或覆盖全项目基线。

## What Changes

- 甘特**缺陷节点写**（拖/缩条、批量平移、未排期落轴、拖到处理人/交付物组改归属）：须独立权限 `system:defect:gantt:plan`（`PUT /system/defect/plan-node`）；`project.create-by` / `project.admin` 可改全部；其他持有该权限者仅当自己在该缺陷 `handleBy` 中时可改。**不**给开发角色挂 `system:defect:edit`。
- **更新基线**、**写工作日历**：仅项目创建人/项目管理员。
- **依赖线**建删：仍要 `system:defect:edit`，不按处理人收窄。
- 图层开关、框选、只读看基线叠加/日历等：保持现有（不要求管理员）。
- 前端与后端同步校验；自动排列/拖避让继续复用同一「可改节点」判定（已有逻辑对齐，必要时统一命名/入口）。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `defect-list-gantt-view`: 节点拖拽/落池等写操作改为按项目管理员 vs 处理人授权；基线更新入口仅管理员。
- `defect-list-gantt-phase2`: 多选批量平移与「Authorized users」基线冻结与上述授权对齐。
- `project-work-calendar`: 写日历明确为项目创建人/项目管理员（与可读成员区分）。
- （`defect-gantt-auto-level` / `defect-gantt-level-resources` 已描述同一项目管理员 vs 处理人规则，本变更不改其 requirement 正文，实现复用同一谓词。）

## Impact

- 前端：`gantt.vue` 拖拽/批量入口由全局 `canEdit` 改为按条 `canAutoLevelEditDefect`（或统一别名）；冻基线按钮禁用条件；工作日历对话框写权限展示。
- 后端：缺陷更新路径对计划相关字段做处理人/管理员校验；`plan-baseline/freeze`（及 clear）限管理员；工作日历 PUT 与项目角色对齐（若现仅 `system:project:edit` 菜单权限，确认只挂在创建人/管理员上或改为显式角色校验）。
- 文档：`docs/04` 交互约定、必要时 `docs/03`/`docs/05` 一句；无新 ADR（延续现有项目角色排期特权）。
