## Context

悬浮菜单通过 `Vue.use(floatMenu)` 在 `main.js` 注册：启动时 `new Cat2BugFloatMenu()` 并 `appendChild` 到 `document.body`，暴露 `Vue.prototype.$floatMenu`（`windowsInit`、`resetMenus`、`setVisible` 等）。Navbar 的 `el-switch` 控制 `visible` 并持久化到 localStorage。

各列表页（计划、缺陷、用例、文档、报告、通知等）及弹窗（AddCase、AddDefect、HandlePlanDialog 等）在生命周期中调用 `initFloatMenu()`，向悬浮菜单注册与工具栏重复的快捷操作（如「新建计划」）。弹窗打开时常 `resetMenus([])` 或注册「关闭/保存」按钮。

## Goals / Non-Goals

**Goals:**

- 完全移除 Cat2BugFloatMenu 及全局插件注册
- 移除 Navbar 悬浮菜单开关
- 清理所有 `$floatMenu` / `initFloatMenu` 引用
- 保留页面内工具栏、表格操作、权限控制不变

**Non-Goals:**

- 不新增替代 UI（如固定 FAB）
- 不修改浏览器测试工具页（`/tools/browser`）若其未使用 Cat2BugFloatMenu
- 不清理用户 localStorage 历史键（可选后续）

## Decisions

1. **方案 A：全量删除（非仅隐藏开关）**
   - 删除插件 + 组件 + 各页 init 代码
   - *备选*：仅隐藏 Navbar 开关 — 拒绝，各页仍会 `windowsInit`，无法达到「不加载」目标

2. **不保留 `$floatMenu` 空实现 stub**
   - 删除后若有遗漏引用会在构建/运行时报错，便于发现
   - *备选*：no-op stub — 拒绝，遗留 dead code

3. **弹窗 `@close="initFloatMenu"` 直接移除**
   - 关闭弹窗后不再需要重建悬浮菜单
   - `HandlePlanDialog/CaseList` 的 `init-float-menu` emit 一并删除

## Risks / Trade-offs

- **[Risk] 某页仅有悬浮菜单、工具栏无等价操作** → 实施前已 spot-check：测试计划等页面工具栏与悬浮菜单共用同一 handler；用例/计划执行弹窗内仍有 PlanItemTools 等内联控件
- **[Risk] 遗漏 `$floatMenu` 引用导致运行时错误** → 全库 grep 验证；刷新后冒烟主要列表页
- **[Trade-off] 失去可拖拽的次要快捷入口** → 产品接受，以简化 UI 与代码为准
