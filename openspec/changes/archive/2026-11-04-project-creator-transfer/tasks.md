## 1. 后端转让与删除护栏

- [x] 1.1 实现 `transferCreator(projectId, operatorId, targetUserId)`：校验仅创建人、目标为本项目成员且非自己；事务内迁移角色（受让人仅 `project.create-by`，原创建人去 create-by 空则补 `project.admin`）；更新 `sys_project.create_by`
- [x] 1.2 `deleteMember` / preview：持有 `project.create-by` 时拒绝删除
- [x] 1.3 `ProjectResource` 暴露 `POST /{projectId}/transfer-creator`（不依赖 member:remove）；成功后刷新当前用户会话权限

## 2. 前端成员页

- [x] 2.1 `project.js` 增加转让 API
- [x] 2.2 创建人行：仅本人显示「转让」；弹框铭牌选项目成员（排除自己）+ 确认；成功后 getInfo / 权限刷新 + 重拉列表
- [x] 2.3 i18n 中/英（标题、提示、成功/失败、仅创建人）

## 3. 文档与验收

- [x] 3.1 更新 `docs/05-接口文档/接口清单-项目团队成员.md` 增加 transfer-creator；注明删除不可删创建人
- [x] 3.2 手工验收：转让成功字段与角色、非创建人不可见/不可调、删创建人被拒、转让后原创建人可 handoff 删除、缺陷/Git 未因转让变动（用户确认并归档）
- [x] 3.3 回写长期知识：无新 ADR；会话权限沿用已有踩坑；Select 铭牌撑高见踩坑；版本记录已写
