## 1. 前端节点写与入口

- [x] 1.1 统一/别名 `canEditGanttPlanDefect`（复用 `canAutoLevelEditDefect` + `isProjectPlanAdmin`）；甘特加载时解析管理员标记
- [x] 1.2 单条拖/缩、未排期落轴、拖到处理人/交付物组：仅可写条可交互；不可写条禁用拖动手柄
- [x] 1.3 多选批量平移：只提交可写子集，跳过不可写并提示；不可用不可写条作拖动手柄
- [x] 1.4 更新基线菜单项仅项目管理员可点；依赖线/图层等保持现有 edit/可视逻辑

## 2. 后端校验

- [x] 2.1 缺陷更新：计划日期（及甘特路径改 handleBy/moduleId）非管理员且非处理人 → 拒绝
- [x] 2.2 `plan-baseline/freeze` 与 clear：仅项目创建人/项目管理员
- [x] 2.3 工作日历 PUT：显式校验项目创建人/管理员；GET `editable` 与之对齐

## 3. 回归与文档

- [x] 3.1 确认自动排列/拖避让仍用同一可写谓词；依赖线仍仅需 `system:defect:edit`
- [x] 3.2 回写 `docs/04-UI规范/组件与交互约定.md`、接口清单（基线/日历若需）、`docs/08-版本记录` 当月一行；无新 ADR

## 4. 独立甘特计划权限（修正：不给 develop 挂 defect:edit）

- [x] 4.1 迁移 `V1_1_0_54`：菜单 `system:defect:gantt:plan`（2428）；赋给开发/测试等；撤回误给 develop 的 2022
- [x] 4.2 `PUT /system/defect/plan-node` + 前端甘特改走该接口；`canEdit` 用 gantt:plan，依赖线仍用 defect:edit
- [x] 4.3 回写 docs/03、04、05、08 与 OpenSpec 产物
