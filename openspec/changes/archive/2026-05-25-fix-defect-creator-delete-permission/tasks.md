## 1. 后端授权与校验

- [x] 1.1 在 `SysDefectServiceImpl` 新增 `assertCanDeleteOrRestore(Long defectId)`：有 `system:defect:remove` 则通过；否则校验 `create_by_id == 当前用户`，失败抛权限异常
- [x] 1.2 在 `deleteSysDefectByDefectId`、`restoreSysDefectByDefectId` 开头调用上述断言（在软删除/恢复逻辑之前）
- [x] 1.3 调整 `SysDefectController` 的 delete/restore 端点 `@PreAuthorize`：允许已登录且具备缺陷模块访问能力的用户进入（或移除单一 `remove` 约束），具体以 Service 断言为最终门禁
- [x] 1.4 确认批量删除 `deleteSysDefectByDefectIds` 任一条无权限时整批失败（事务回滚）

## 2. 前端 Excel 对齐

- [x] 2.1 修改 `excel.vue` 的 `handleExcelDeleteSelectedRows`：有 `system:defect:remove` 则允许；否则要求所选 persisted 行全部 `createById === 当前用户`
- [x] 2.2 无权限时使用明确的权限错误提示（非泛化 save-failed 文案）

## 3. 文档

- [x] 3.1 更新 `readme/production/role-guide/external.md`：缺陷管理章节改为「可删除自己提交的缺陷，不可删除他人缺陷」

## 4. 验证

- [x] 4.1 以外部人员账号创建缺陷，列表/详情删除成功，不再 403
- [x] 4.2 外部人员尝试删除他人缺陷，后端拒绝
- [x] 4.3 外部人员恢复自己已删缺陷成功
- [x] 4.4 持 `system:defect:remove` 的用户仍可删除/恢复任意缺陷
- [x] 4.5 Excel 批量删除：仅自己的行可删，混合选择被前端拦截
