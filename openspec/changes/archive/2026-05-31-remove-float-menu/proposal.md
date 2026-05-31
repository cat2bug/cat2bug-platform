## Why

Cat2BugFloatMenu（悬浮快捷菜单）在应用启动时全局挂载，各业务页在 `created`/`mounted` 中重复 `initFloatMenu()`，Navbar 另有独立开关。多数页面悬浮按钮与工具栏「新建」等操作重复，增加维护成本与运行时开销（scroll 监听、body 单例 DOM），且顶部开关易与主题切换等控件混淆。产品决定下线整套悬浮菜单，统一使用页面内工具栏与表格操作。

## What Changes

- **BREAKING**：移除全局 `Cat2BugFloatMenu` 插件及 `$floatMenu` API
- 移除 Navbar 悬浮菜单开关（`el-switch`）
- 移除约 27 个页面/弹窗中的 `initFloatMenu`、`windowsInit`、`windowsDestory` 及 `@close="initFloatMenu"` 联动
- 删除 `src/components/Cat2BugFloatMenu/` 组件目录
- 清理 `theme-dark.scss` 中 `--float-menu-*` 与 `.cat2bug-float-menu` 样式
- 移除 i18n `float-menu` 键（7 语言）
- 页面工具栏、表格操作、弹窗内按钮等行为不变

## Capabilities

### New Capabilities

- `remove-float-menu`: 系统不再提供全局悬浮快捷菜单；Navbar 无开关；业务页不初始化浮动菜单

### Modified Capabilities

（无：现有 `openspec/specs/` 中无独立 float-menu capability）

## Impact

- **前端**：`main.js`、`Navbar.vue`、各 `views/` 与 `components/` 业务页/弹窗、`theme-dark.scss`、i18n 文件
- **用户可见**：右侧可拖拽悬浮按钮消失；顶部少一个开关；新建/编辑等仍通过页面工具栏完成
- **数据**：localStorage `cat2bug-float-menu-drag-position` / `cat2bug-float-menu-drag-visible` 可遗留，无功能影响
