## Why

外部人员（项目外部人员角色）创建缺陷后，前端因 `createById` 匹配而显示删除按钮，但后端删除/恢复接口仅校验 `system:defect:remove` 权限，导致点击后返回 403「当前操作没有权限」。这与前端 `DefectTools` 及用户指南「创建人可删除自己缺陷」的预期不一致，也不同于文档删除接口已有的「创建人 OR 权限」模式。

## What Changes

- 后端缺陷删除与恢复 API 允许：**拥有 `system:defect:remove` 权限的用户**，或**缺陷创建人（`create_by_id` 等于当前用户）** 执行操作
- Service 层对非 `remove` 权限的创建人请求校验 `createById`，禁止删除/恢复他人缺陷
- Excel 视图批量删除逻辑与 `DefectTools` 对齐，允许创建人删除自己创建的行
- 更新外部人员角色指南：明确可删除**自己提交**的缺陷，不可删除他人缺陷
- 不改变角色菜单权限配置（不为外部人员全局授予 `system:defect:remove`）

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `defect-soft-delete`：补充删除与恢复 API 的授权规则——创建人可删除/恢复自己创建的缺陷，无需 `system:defect:remove` 权限；持 `remove` 权限用户仍可删除/恢复任意缺陷

## Impact

- **后端**：`SysDefectController`（delete、restore 端点授权）、`SysDefectServiceImpl`（创建人校验）
- **前端**：`cat2bug-platform-ui/src/views/system/defect/list/excel.vue`（`handleExcelDeleteSelectedRows` 权限判断）
- **文档**：`readme/production/role-guide/external.md`
- **API**：`DELETE /system/defect/{defectIds}`、`PUT /system/defect/{defectId}/restore` 行为扩展，非破坏性变更
- **数据库/角色**：无 schema 或 `sys_role_menu` 变更
