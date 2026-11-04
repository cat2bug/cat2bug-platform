## Why

同一项目里常出现多条实质相同的缺陷（重复提单、多渠道上报）。现有批量只有流转与删除，无法在对比字段后保留一条并清掉其余。需要在批量操作中提供「合并」：用户选定主缺陷、按字段择优覆盖，一次提交更新主缺陷并软删从缺陷。

## What Changes

- 缺陷表格批量操作增加「合并」菜单项（独立权限 `system:defect:merge`；勾选 ≥2 且均有权合并时可用）
- 打开宽抽屉：左侧为合并结果可编辑表单；右侧为勾选缺陷只读列表
- 右侧每项提供「主缺陷」按钮：将该条全部可编辑字段写入左侧；若左侧已有内容则先确认覆盖
- 点选左侧某字段时，右侧列表对应字段着重高亮，并提供「用此属性」：用该源缺陷的**同一字段**覆盖左侧（替换，不拼接多条）
- 未选定主缺陷时「合并」不可提交；确认后调用单一后端接口：事务内更新主缺陷、软删其余从缺陷；合并不可拆回
- 浅合并：不迁移日志/评论/计划项 CSV/甘特依赖边/分享；从缺陷进回收站即可；主缺陷可写 MERGE 审计日志（含被合并编号）
- Flyway 增加菜单权限点；默认仅 `project.create-by` / `project.admin` / `project.tester`

## Capabilities

### New Capabilities

- `defect-merge-duplicates`：缺陷重复合并的交互、权限、单一合并 API 与浅合并语义

### Modified Capabilities

- `defect-batch-operations`：表格批量菜单增加「合并」入口与可用性条件（≥2、独立权限；不走现有批量流转弹窗）

## Impact

- 前端：`defect/index.vue` 批量菜单、`defect-action-visibility.js`、新建合并抽屉（复用 `DefectFormOrderedFields`）、i18n
- 后端：`DefectResource` 新接口、`SysDefectService` 事务更新+软删、可选 `DefectLogState.MERGE`、权限注解
- 迁移：Flyway 菜单 `system:defect:merge`（版本号取当前产品段下一空号，避开并行变更已占号）
- 文档回写：`docs/05-接口文档/接口清单-缺陷与用例计划.md`、术语表如需补「合并」一行
- 不推翻已有 ADR；不做 MCP 专用工具（后续可走通用 `call_operation`）
