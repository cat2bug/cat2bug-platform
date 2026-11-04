## 1. 路由与整页骨架

- [x] 1.1 扩展 `buildPlanChildren()`：增加 `plan/new`、`plan/:planId/edit`（`new` 须排在动态 `planId` 段之前），meta 对齐执行页（`projectModule: plan`、`noCache` 等）
- [x] 1.2 新增计划表单整页视图（如 `views/system/plan/form.vue`），按路由区分新建/编辑；页头含返回、标题、取消/保存
- [x] 1.3 扩展 `tests/unit/router/project-scoped-routes.spec.js`（或等价）覆盖 `plan/new` 与 `plan/:planId/edit` 不被 catch-all / `run` 吞掉

## 2. 表单与选用例区迁移

- [x] 2.1 将 `AddPlanDialog` 元数据表单与提交逻辑迁入整页（`addPlan`/`updatePlan`、校验、临时 planId 拉用例、编辑 `getPlan` 回填）
- [x] 2.2 迁入选用例区：查询条 + `vue-multipane` + `TreePlanItemModule` + 用例多选表；高度同步对齐用例页，勿再依赖 dialog body 高度
- [x] 2.3 左右分栏拖拽、侧栏显隐与宽度缓存可用；`deliverable` 团队闸门行为与现弹窗一致
- [x] 2.4 页边距与计划列表一致（外层左右约 20px，内容区不叠多余左右 padding）
- [x] 2.5 将原 Dialog 关闭/键盘逻辑迁到路由离开确认（`onBeforeRouteLeave` 等），严格度不低于现 `before-close`

## 3. 入口与导航

- [x] 3.1 计划列表「新建」「修改」改为 `router.push` 到 `plan/new` / `plan/:planId/edit`，移除主路径上的 `AddPlanDialog` 挂载
- [x] 3.2 工作台 `QuickCreate`（及其它打开 `openAdd`/`openUpdate` 的入口）改为跳转整页
- [x] 3.3 保存成功后导航回计划列表并刷新；取消/返回同样回列表且侧栏高亮计划模块
- [x] 3.4 清理无用的 Dialog 样式/键盘 mixin 引用，或删除已无入口的 `AddPlanDialog`（以实现时为准）

## 4. 回写长期知识

- [x] 4.1 若 UI 约定需补充「计划新建/编辑为整页」一句，更新 `docs/04-UI规范/组件与交互约定.md`；无新 ADR / 无可复用踩坑则注明「无」并勾掉
