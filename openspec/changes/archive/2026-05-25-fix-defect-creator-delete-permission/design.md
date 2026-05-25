## Context

缺陷软删除能力（`defect-soft-delete`）已实现删除/恢复 API 与 `del_flag` 机制。前端 `DefectTools` 按「创建人 OR `system:defect:remove`」显示删除/恢复按钮；后端 `SysDefectController` 的 delete/restore 端点仅使用 `@PreAuthorize("@ss.hasPermi('system:defect:remove')")`，在 Spring Security 层即拦截无 `remove` 权限的请求。

项目外部人员角色（`project.outsider`）拥有 `system:defect:add`、`system:defect:edit`，但**不包含** `system:defect:remove`（menu 2023 未分配）。因此外部人员自建缺陷时前端显示删除按钮，后端返回 403。

同类问题：`SysDocumentController.remove` 已实现「无 remove 权限时校验创建人」模式，可作为参考；`excel.vue` 的 `handleExcelDeleteSelectedRows` 仅校验 `remove` 权限，与 `DefectTools` 不一致。

## Goals / Non-Goals

**Goals:**

- 创建人可删除/恢复自己创建的缺陷，无需 `system:defect:remove`
- 持 `system:defect:remove` 权限的用户仍可删除/恢复任意缺陷（现有行为不变）
- 非创建人且无 `remove` 权限的请求被拒绝
- 前端 Excel 与 DefectTools 删除权限判断一致
- 更新外部人员文档，消除与 `defect-delete.md` 的冲突

**Non-Goals:**

- 不为外部人员角色全局添加 `system:defect:remove` 菜单权限
- 不改变软删除数据模型、`del_flag` 语义或日志类型
- 不修改编辑、工作流等其他缺陷操作的授权规则
- 不处理报告（Report）模块的同类前后端不一致（可后续单独变更）

## Decisions

### 1. 授权分层：Controller 放宽入口，Service 校验创建人

**选择**：删除 `@PreAuthorize` 对 delete/restore 的单一 `remove` 约束，改为 `@PreAuthorize` 允许「有 remove 权限 OR 已登录项目成员」进入方法；在 Service 层对无 `remove` 权限的调用者校验 `createById == SecurityUtils.getUserId()`。

**理由**：`@PreAuthorize` 表达式难以在 SpEL 中查库比对 `createById`；Service 层已有 `selectSysDefectByDefectId` 可获取缺陷实体，与 `SysDocumentController` 在 Controller 内循环校验的模式等价，但更符合缺陷模块现有分层。

**备选**：
- *Controller 内逐条校验*（同 Document）：可行，但与 Defect 批量 delete 逻辑重复，Service 更合适
- *SpEL 自定义 PermissionEvaluator*：过度设计

### 2. 批量删除：逐条校验，部分失败即整体拒绝

**选择**：`deleteSysDefectByDefectIds` 在循环每条 `defectId` 前调用统一的 `assertCanDeleteOrRestore(defectId, allowCreator=true)`；任一 ID 无权限则抛异常，不部分提交。

**理由**：与现有 `@Transactional` 批量删除语义一致；避免「有权限删 A、无权限跳过 B」的静默部分成功。

### 3. 前端 Excel：按行判断创建人 OR remove

**选择**：`handleExcelDeleteSelectedRows` 改为：若用户有 `system:defect:remove` 则允许所选行；否则仅当所选行**全部**为当前用户创建时才允许删除，否则提示无权限。

**理由**：与 `DefectTools.deleteVisible` 语义一致；防止无 remove 权限用户通过 Excel 多选删除他人缺陷。

### 4. 错误响应

**选择**：无权限时使用 `ServiceException` + 现有 i18n key（如 `no.permission` 或专用 `defect.delete_no_permission`），HTTP 403。

**理由**：与 GlobalExceptionHandler 现有行为一致；前端 `errorCode.js` 已映射 403 为「当前操作没有权限」。

## Risks / Trade-offs

- **[Risk] 创建人删除后他人无法从「已删除」Tab 代为恢复** → 可接受；持 `remove` 权限的管理员/测试人员仍可恢复
- **[Risk] 外部人员误删有效反馈** → 软删除可恢复；文档建议优先「关闭」已处理缺陷
- **[Trade-off] Controller 去掉严格 PreAuthorize** → Service 必须强制校验，避免遗漏；实现时 delete/restore 共用断言方法

## Migration Plan

1. 部署后端（Service 校验 + Controller 授权调整）
2. 部署前端（Excel 权限对齐）
3. 更新 `external.md` 文档
4. 无需数据库迁移或角色菜单变更
5. **回滚**：还原 Controller/Service/Excel 即可，无数据副作用

## Open Questions

（无）
