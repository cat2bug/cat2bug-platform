## Context

甘特节点拖拽与批量平移曾用全局 `canEdit`（`system:defect:edit`）；自动排列/拖避让已用 `isProjectPlanAdmin` + `canAutoLevelEditDefect`（管理员全部，其他人仅 `handleBy` 含自己）。基线 freeze API 仅 `@RequiresPermissions("system:defect:edit")`；工作日历写为 `system:project:edit` + 响应 `editable`。产品要求节点写与基线/日历写对齐项目创建人/管理员特权，依赖线与图层等保持宽松；**开发角色不得因此获得 `system:defect:edit`**，故节点写改为独立权限 `system:defect:gantt:plan`。

约束：不改缺陷工作流状态机；甘特计划写走 `PUT /system/defect/plan-node`；角色判定与现有 `isProjectPlanAdmin(project, userId)` 一致（`createBy` 或成员角色 `project.create-by` / `project.admin`）。

## Goals / Non-Goals

**Goals:**

- 统一「甘特计划节点可写」判定，前后端一致
- 节点写权限与表格缺陷修改分离（`system:defect:gantt:plan`）
- 冻基线 / 清基线 / 写工作日历仅项目创建人/管理员
- 依赖线仍按 `system:defect:edit`，不跟处理人
- UI：不可写条不可拖；不可冻基线时禁用入口；非管理员打开日历只读

**Non-Goals:**

- 改表格/Excel 里其它字段的编辑权限模型
- 用团队角色（`team.*`）替代项目角色
- 给 `project.develop` 挂 `system:defect:edit`
- 改自动排列算法本身（仅保证与节点写同一谓词）

## Decisions

1. **单一谓词 `canEditGanttPlanDefect(defect, ctx)`**  
   语义等同现有 `canAutoLevelEditDefect`：`ctx.isProjectAdmin` → true；否则当前用户 ∈ `handleBy`/`handleByList`。前端拖条、落池、批量平移、拖避让推挤、自动排列候选共用；可保留旧名作别名以免大面积改测试。  
   **备选**：两套函数 — 否决，易漂移。

2. **管理员判定继续 `isProjectPlanAdmin`**  
   拉 `getProject`（或已有项目上下文）看 `createBy` / members.roles。后端 freeze/日历写用同等角色查询（项目成员角色表），不信任前端布尔。  
   **备选**：仅菜单 perms — 否决，`defect:edit` 过宽。

3. **服务端校验范围**  
   - 甘特触发的计划写：`planStartTime` / `planEndTime` / 经甘特改 `handleBy`/`moduleId` 时，非管理员且非处理人 → 403。实现可在缺陷更新服务对「计划相关字段变更」分支调用同一检查；若难精确区分来源，至少对计划日期变更做检查（拖人/交付物同路径则一并覆盖）。  
   - `POST .../plan-baseline/freeze` 与 `DELETE .../plan-baseline`：管理员 + 建议仍要求 `defect:edit` 或项目编辑权。  
   - 工作日历 PUT：显式校验项目创建人/管理员（若现仅靠 `system:project:edit` 且该 perm 只赋给管理员，可保留 perm 并在服务层双重校验角色，避免以后 perm 扩散）。  
   **备选**：只改前端 — 否决，可绕过。

4. **批量平移**  
   选中集可含不可写条；拖时只更新可写子集，不可写跳过并汇总失败/跳过提示（与「部分失败」一致）。不可写条不应成为拖动手柄（或按下即拒绝批量）。

5. **依赖线**  
   保持 `system:defect:edit`；规格不改为处理人约束。

6. **无新 ADR**  
   延续自动排列已采用的项目角色排期特权，写入 docs/04 一句即可。

## Risks / Trade-offs

- [表格里仍可用 edit 改他人计划日期] → 本变更聚焦甘特路径；若产品要全局一致，后续另开变更收紧 `updateDefect` 全字段策略  
- [getProject 失败导致 isAdmin=false] → 降级为仅自己可写；冻基线禁用  
- [多人 handleBy 任一人可改] → 产品已确认  
- [非管理员误以为能冻基线] → 入口禁用 + API 403 文案

## Migration Plan

`V1_1_0_54`：新增菜单 `system:defect:gantt:plan`（2428），赋给创建人/管理员/开发/测试等；若曾误给 develop 挂 2022（`defect:edit`）则撤回。回滚需去掉该菜单与 `plan-node` 接口，甘特改回走 `updateDefect`。

## Open Questions

（无 — 依赖线宽松、handleBy 任一成员可写已拍板）

## 相关长期文档

- `docs/03-技术方案/权限模型与鉴权链路.md`
- `docs/04-UI规范/组件与交互约定.md`
- `docs/05-接口文档/接口清单-项目团队成员.md`（工作日历）
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`（若有基线接口行则同步）
- `docs/08-版本记录/2026-10.md`
- 无新 ADR
