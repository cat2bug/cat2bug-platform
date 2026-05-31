## 1. 移除全局插件与组件

- [x] 1.1 `main.js` 移除 `Vue.use(floatMenu)` 及 Cat2BugFloatMenu import
- [x] 1.2 删除 `src/components/Cat2BugFloatMenu/` 目录

## 2. Navbar

- [x] 2.1 移除悬浮菜单 `el-switch`、`floatMenuVisible`、`floatMenuContent`、`handleFloatMenuVisible`

## 3. 业务页与弹窗清理

- [x] 3.1 移除各 `views/` 列表页中的 `initFloatMenu` / `windowsDestory` 及模板 `@close="initFloatMenu"`
- [x] 3.2 移除各 `components/` 弹窗（AddCase、AddDefect、HandlePlanDialog 等）中的 float menu 逻辑
- [x] 3.3 移除 `HandlePlanDialog/CaseList`、`DefectList` 的 `init-float-menu` emit

## 4. 样式与 i18n

- [x] 4.1 清理 `theme-dark.scss` 中 `--float-menu-*` 与 `.cat2bug-float-menu` 规则
- [x] 4.2 移除 7 语言 `float-menu` i18n 键

## 5. 验证

- [x] 5.1 全库 grep 无 `$floatMenu` / `initFloatMenu` / `Cat2BugFloatMenu` 残留
- [ ] 5.2 冒烟：计划/缺陷/用例列表工具栏新建；Navbar 无悬浮开关；页面无右侧浮动按钮
