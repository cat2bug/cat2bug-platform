## 1. 路由

- [x] 1.1 新增 `buildPlanChildren()`：`plan` 列表 + `plan/:planId/run` 执行页，并从通用 catch-all 排除 `plan`
- [x] 1.2 配置执行页 meta（projectScoped、projectModule、activeMenu / 侧栏高亮）

## 2. 执行整页

- [x] 2.1 将 `HandlePlanDialog` 从列表抽屉改为整页视图（或抽面板 + `views/system/plan/run.vue`），保留统计/列表/模块树与用例嵌套抽屉
- [x] 2.2 按路由 `planId` 加载计划；返回导航到同项目 `plan` 列表
- [x] 2.3 调整键盘关闭：Esc 先关嵌套用例抽屉，再返回列表

## 3. 列表与深链

- [x] 3.1 列表名称、「执行」、快捷键改为 `router.push` 执行页；移除列表上的执行抽屉挂载
- [x] 3.2 `?planId=` 兼容重定向到 `plan/:planId/run`
- [x] 3.3 更新仪表盘等深链（如 `PlanStatisticsChart`）指向执行页 URL

## 4. 回写长期知识

- [x] 4.1 若有可复用约定则更新 `docs/04-UI规范/组件与交互约定.md`；否则注明无并勾选
- [x] 4.2 修复执行页返回列表白屏：AppMain 对 `noCache` 与 keep-alive 分槽 transition
