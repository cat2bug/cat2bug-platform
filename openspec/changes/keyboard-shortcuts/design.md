## Context

- 左侧菜单为运行时动态生成（`layout/components/Sidebar/index.vue`），按顶层 `name` 分组（`team` / `project` / `team-options` / `admin`），每组 `children` 为真实菜单项，来源于 store 的 `sidebarRouters`（后端 `getRouters`）。
- 顶部工具栏（`layout/components/Navbar.vue`）含：主题切换、官网、源码(Git)、通知(`/notice/index`)、系统文档、国际化(`lang-select` 下拉)、个人头像(`el-dropdown`：个人中心 / 退出)。
- 模块路由为项目级动态路由：`/project/defect`、`/project/case`、`/project/plan`、`/project/document`、`/project/report`、`/project/option` 等；导航即 `$router.push({ path })`。
- 已有依赖 `v-hotkey`（`main.js` 已 `Vue.use`），但其为组件级指令，不适合全局动态 + OS 无关 + 自定义 + 面板的需求，故自建集中式引擎；`v-hotkey` 可保留给局部组件，不在本变更扩展使用。
- 偏好持久化既有模式：`localStorage`（`themeMode`、`layout-setting`）。

## Goals / Non-Goals

**Goals:**

- 一套全平台一致、零浏览器冲突的键盘操作模型（引导键 + 单字母 / 单数字）
- 导航目标从实时菜单树 + 顶部工具栏自动派生，菜单变化自动适配
- 统一的居中悬浮命令面板，覆盖导航、页面动作、下拉二级
- 用户可自定义并本地持久化，含冲突检测与恢复默认
- 输入态全面禁用，避免误触

**Non-Goals:**

- 后端存储 / 跨端同步
- 缺陷页以外的页面动作面板（引擎留扩展点）
- 多键修饰组合、宏、序列录制
- 移动端适配

## Decisions

### 1. 键盘模型：引导键序列 + 双档修饰键浮层

```
STAGE 1 引导键            STAGE 2 居中悬浮面板          STAGE 3 字母/数字
  g     ──────────────▶  导航面板(左菜单+顶部项)  ──▶  叶子项→跳转 / 下拉项→子面板
  空格  ──────────────▶  当前页动作面板            ──▶  执行动作 / 数字跳转

修饰键浮层（无面板，控件角标）：
  ⇧⌘ / Shift+Ctrl  ──▶  布局级：左侧菜单 + 顶栏 + 折叠按钮（与 g 同字母表）
  ⌘ / Ctrl（无 Shift）──▶  页面级：缺陷工具栏 / 统计区 / 表单字段等（各页自建映射）
```

- 引导键后进入"待接收"状态（默认超时 2000ms 自动取消），期间面板可见。
- 仅捕获 **单字母 / 单数字**；不依赖 `Cmd/Ctrl/Meta`，因此**无需 OS 分支**，也不触碰浏览器保留组合。
- `Esc` 或点击遮罩或超时关闭面板；按未绑定键无操作并轻提示。
- 子面板（下拉二级）为面板内栈式切换，`Esc` 返回上一级。

> 取舍：放弃「单独 `Cmd+字母` 做全局导航」。原因：`Cmd+W/T/N/Q/R/V` 等浏览器保留组合无法拦截。全局导航改用 `g` 序列键；**布局级角标**则用 `Shift+Cmd+字母`——Shift 档位与页面级 `Cmd` 浮层分离，且 Shift 组合较少与浏览器冲突。

### 1.1 布局级浮层（`layout-nav-hints`）

| 修饰键 | 范围 | 字母来源 |
|---|---|---|
| **Shift+Cmd/Ctrl** | 左侧菜单项、Navbar 图标、汉堡折叠按钮 | `buildNavItems()` + `nav.layout.sidebarToggle`（默认 `` ` ``） |
| Cmd/Ctrl alone | 当前页工具栏、表单、抽屉等 | 各页 `page-action-hints` / `form-field-hints` |

**实现：** `mixins/layout-nav-hints.js` 挂于 `layout/index.vue`；Navbar / LangSelect / Hamburger 锚点带 `data-layout-nav`；侧栏通过 `el-menu-item[index]` 与路由 `to` 匹配。徽标复用 `page-kbd-hints.scss` 浮层样式；侧栏项徽标靠左（`center-left-inset`），顶栏靠右下角。

**互斥：** `page-action-hints`、`form-field-hints`、`handle-defect-kbd-hints` 在 `e.shiftKey` 时跳过；布局浮层在遮挡层（dialog/drawer 等）打开时不显示。

### 2. 引擎架构

```
src/plugins/shortcut/
  ShortcutService.js    # 单例：document keydown 监听、引导态状态机、焦点守卫、派发
  shortcut-store.js     # 默认映射(派生) + localStorage 覆盖 + 冲突检测 + 序列化
  registry.js           # 运行时动作注册表（页面 register/unregister）
  install.js            # Vue 插件：Vue.prototype.$shortcut

src/components/Shortcut/
  CommandPalette.vue    # 居中悬浮面板（导航/动作/子面板通用，渲染字母角标）
  ShortcutBadge.vue     # 右下角字母角标小组件

src/views/member/keyboard/
  index.vue             # 「键盘设置」配置页
```

- 引擎在 `layout/index.vue` 挂载（仅登录后布局内生效）；`CommandPalette` 同层渲染一份全局实例。
- 暴露 API：
  - `this.$shortcut.registerPage(scopeKey, actions[])` / `unregisterPage(scopeKey)`：页面在 `mounted/activated` 注册动作面板内容，`beforeDestroy/deactivated` 注销。
  - `this.$shortcut.open('nav' | 'action')`：编程触发面板（供按钮/帮助调用）。

### 3. 焦点守卫（禁用规则）

引导键在以下情形 **MUST NOT** 拦截，按键透传给页面：

- `document.activeElement` 为 `input` / `textarea` / `[contenteditable=true]` / `select`
- 处于 Excel 编辑态（`vue-excel-editor` 单元格编辑）
- 由 IME 组合输入中（`event.isComposing`）
- 焦点位于 `button` / `a` 或 `role=button|link|menuitem` 等原生可交互元素（避免劫持空格/回车）

**动作引导键（空格）额外限制**（仅缺陷页「页面动作」）：

- 当前页 MUST 已通过 `registerPage` 注册动作（缺陷列表页）
- 页面上 **MUST NOT** 存在可见遮挡层，包括但不限于：
  - `el-dialog` / `el-drawer` / `el-message-box` 包装层
  - 命令面板遮罩 `.cmdp-mask`
  - 成员/交付物组合下拉 `.select-project-member-popover` / `.select-module-popover`
  - Element 浮层：`.el-select-dropdown`、`.el-picker-panel`、`.el-dropdown-menu` 等
- 实现：`shortcut/service.js` 中 `canOpenDefectPageActions()` + `hasBlockingUiLayer()`

导航引导键 `g` 仍遵循输入态守卫（输入框内可输入 `g`）；**不受**遮挡层列表限制。

例外：命令面板自身打开时，引擎接管全部按键（面板是受控态）。

### 4. 导航映射（从菜单树 + 顶部项派生）

面板内每个字母在**同一面板内唯一**。默认字母如下（用户可改）：

**左侧菜单页面（导航面板 `g`）**

| 字母 | 目标 | 路由来源 |
|---|---|---|
| D | 缺陷 | `/project/defect` |
| C | 用例 | `/project/case` |
| P | 测试计划 | `/project/plan` |
| J | 交付物 | 交付物独立页（实现时按菜单项 path 取） |
| Y | 仪表盘 | 项目仪表盘 |
| R | 报告 | `/project/report` |
| W | 项目文档 | `/project/document`（面板显示「项目文档」） |
| O | 项目设置 | `/project/option` |
| S | 团队设置 | team 分组菜单项 |
| X | 项目管理 | 项目列表页 |

**顶部工具栏（同一导航面板 `g`）**

| 字母 | 目标 | 类型 |
|---|---|---|
| N | 通知 | 跳转 `/notice/index` |
| H | 帮助文档 | 打开系统使用手册（`system-doc`，面板显示「帮助文档」） |
| I | 国际化 ▸ | 下拉 → 二级面板列语言 |
| U | 个人中心 ▸ | 下拉 → 二级面板（个人中心 / 键盘设置 / 退出） |
| M | 主题 ▸ | 下拉 → 二级面板（浅色 / 暗色） |
| B | 官网 | 打开官网 |
| K | 源码(Git) | 打开源码仓库 |

- **派生策略**：导航项以菜单树为准（按 `meta.titleI18nKey` 取标题、`path` 取路由）；默认字母按上表匹配，匹配不到的菜单项自动分配未占用字母；用户覆盖优先。
- 仅渲染当前可见 / 有权限的菜单项（无权限的不进面板）。

**下拉二级面板默认字母**

| 入口 | 二级项 | 默认字母 |
|---|---|---|
| 国际化 L | 简体中文 / 繁體中文 / English / Русский / 日本語 / 한국어 / العربية | 按序 1–7（或首字母） |
| 个人中心 U | 个人中心 / 键盘设置 / 退出登录 | P / K / Q |
| 主题 M | 浅色 / 暗色 | L / D |

### 5. 缺陷页动作映射

缺陷页支持**双通道**触发同一套动作字母（键位可用户在「键盘设置」中修改）：

| 通道 | 交互 |
|---|---|
| 动作引导键 `空格` | 打开居中命令面板，再按字母执行 |
| 按住 `Cmd/Ctrl` | `page-action-hints` 在控件右下角显示浮层徽标；保持修饰键再按字母直接执行（无需先按空格） |

**默认字母与动作**

| 键 | 动作 | 挂接说明 |
|---|---|---|
| L | 切换项目 | 打开左上角 `ProjectLabel` 项目选择浮层 |
| E | 新建菜单 | 展开新建/导入/导出工具栏下拉（`addMenu`） |
| S | 查询 | 聚焦查询区并进入查询键盘导航 |
| J | 切换 Tab | 聚焦页签条；`←/→` 切换 Tab，末项 `→` 到添加按钮 |
| I | 统计模版 | 跳转统计模版配置页 |
| G | 统计模块导航 | **仅统计区存在模块时**注册并显示徽标；`←/→` 选择模块，`Enter`/空格触发主点击，`Delete` 移除 |
| O | 切换视图 | 表格 / Excel 等视图切换 |
| B | 上一页 | 分页上一页 |
| P | 下一页 | 分页下一页 |

**动态行徽标（按住 `Cmd/Ctrl`）**

- 表格 / Excel 视图：为当前视口内可见行在序号列旁显示动态字母（`1`–`9` 优先，字母池补位，与工具栏字母去重）。
- 再按对应字母：打开该行缺陷详情。

**统计区 `G` 显隐**

- 统计区**无模块**时：不注册 `statisticNav`、不显示 `G` 徽标、不响应 `G`；统计区与 Tab 工具栏仍保留 10px 间距（`defectStatisticWrapVisible`）。

- 缺陷页通过 `registerPage('defect', [...])` 提供动作回调；`activated/deactivated` 配对注销。
- **触发条件**：仅在缺陷列表**一级界面**、无任何弹框/抽屉/下拉浮层时，空格或 `Cmd/Ctrl` 页面动作才生效（见 Decision 3、9）。

### 6. 配置与持久化

```
localStorage key: cat2bug.shortcuts.v1
{
  "enabled": true,
  "leaders": { "nav": "g", "action": "Space" },
  "overrides": {            // 仅存用户改动的差异
    "nav.defect": "d",
    "action.defect.new": "n"
  }
}
```

- 默认映射来自代码（随菜单派生），**仅持久化用户改过的项**，升级不丢、体积小。
- 绑定 ID 命名：`nav.<menuKey>`、`nav.top.<itemKey>`、`action.<pageKey>.<actionKey>`（如 `action.defect.query`、`action.statistic-template.preview`）、`nav.top.<dropdownKey>.<subKey>`。
- 「键盘设置」页：分组展示（导航 / 缺陷页 / 下拉）、行内编辑捕获新键、**同面板内重复即时标红**、单项与全局「恢复默认」。

### 7. 命令面板交互细节

- 居中浮层（`position: fixed`，遮罩半透明），随 `themeMode` 适配明暗。
- 每项卡片：图标 + 标题 + 右下角字母角标（复用 `ShortcutBadge`）；下拉项标 `▸`。
- 进入面板后：匹配唯一字母即执行 / 进子面板。
- **模糊搜索（首期纳入）**：面板顶部含搜索输入框，键入文本按项标题做模糊匹配并实时过滤；上下方向键 + 回车选中过滤结果；字母直达与模糊搜索并存——纯字母仍即时触发，进入搜索框后遵循输入态规则（此时字母键作为搜索内容，不触发直达）。
- 无障碍：面板 `role="dialog"`，`aria-label`；角标 `aria-hidden`，标题含 "(快捷键 D)" 供读屏。

### 8. 实现轨道（单变更内排期）

```
A 引擎 + 插件 + 焦点守卫 + 状态机（无 UI 也能 console 验证）
B 命令面板组件（导航面板 + 角标 + Esc/超时/遮罩）
C 导航映射派生（菜单树 + 顶部项）+ 跳转/主题/打开动作
D 下拉二级面板（国际化 / 个人中心）
E 缺陷页动作面板（registerPage 接入）
F 键盘设置页 + localStorage + 冲突检测 + 恢复默认
G i18n 7 语言 + 「个人中心」入口 + 路由
H 缺陷表单/抽屉键盘集成（见 Decision 9）
I 统计模版页快捷键（见 Decision 10）
J 缺陷工具弹框 + 处理缺陷抽屉徽标（见 Decision 11–12）
K 统计模块配置弹框 + 报时提醒表格（见 Decision 13–14）
```

### 10. 统计模版页快捷键（`StatisticTemplate.vue`）

统计模版页注册 `registerPage('statistic-template', …)`，复用 `page-action-hints` 与缺陷页相同的 `Cmd/Ctrl` 浮层徽标样式（`assets/styles/page-kbd-hints.scss`）。

**区域入口（按住 `Cmd/Ctrl` 显示徽标，或空格面板后按字母）**

| 键 | 区域 |
|---|---|
| P | 预览区 |
| G | 个人模版 |
| H | 团队模版 |

**区域内导航**

| 区域 | 方向键 | 确认 / 删除 |
|---|---|---|
| 预览区 | 仅 `←/→` | `Delete` / `Backspace` 从预览移除 |
| 个人 / 团队模版 | `↑↓←→` 网格导航（`statistic-grid-kbd.js`） | 空格 / `Enter` 添加到预览区 |

**跨区域上下传递（边界行 + 水平 x 对齐）**

```
预览区  ←——↑——  个人模版  ←——↑——  团队模版
        ——↓——→          ——↓——→
```

- 团队模版**首行** `↑` → 个人模版**末行**、中心 x 最接近的模块
- 个人模版**首行** `↑` → 预览区 x 最接近的模块
- 预览区 `↓` → 个人模版**首行** x 对齐
- 个人模版**末行** `↓` → 团队模版**首行** x 对齐
- 目标区域无模块时跨区失败，焦点保持不动

**Esc 行为**

- 处于区域导航中：先 `exitTemplateKbdNav` 退出导航
- 未在区域导航：返回上一页（`$router.back()`）
- 不使用 `⌘+B` 返回

**键盘设置**：独立分组「统计模版页」，绑定 ID 前缀 `action.statistic-template.*`。

### 11. 缺陷工具弹框键盘集成（`DefectTools/*Dialog.vue`）

缺陷列表工具栏打开的**小型操作弹框**（指派、修复、驳回、通过、打开、关闭）与新建/编辑抽屉共享同一键盘模型，但作用域为 `el-dialog` 而非 `el-drawer`。

**设计思路：** 抽取三层 mixin，避免六个弹框重复实现：

```
defect-tool-dialog-kbd.js
  ├─ dialog-form-shortcuts.js    # Cmd/Ctrl+Enter / Esc
  ├─ defect-tool-dialog-close.js # 未保存关闭确认（序列化 form 快照对比）
  └─ form-field-hints.js         # Cmd/Ctrl 字段字母徽标
```

| 交互 | 行为 |
|---|---|
| 弹框 `@opened` | 记录 `toolDialogCloseBaseline`，`requestAnimationFrame` 后聚焦第一个 Tab 停靠点 |
| `Cmd/Ctrl + Enter` | `shortcutSave` → `onSubmit` |
| `Esc` / 取消 / `before-close` | `requestCloseToolDialog`；脏数据时 `$modal.confirm` |
| 按住 `Cmd/Ctrl` | 表单字段字母徽标（视口分配，与抽屉规则一致） |

**关闭确认：** `defect-tool-dialog-close-state.js` 对 `form` 做稳定序列化（排序、去噪），仅比较用户可编辑字段；与统计弹框的 `statistic-dialog-close-state.js` 对称。

**遮挡层：** `hasBlockingUiLayer` 对 `excludeDefectToolDialog: true` 放行，使报时提醒等统计弹框打开时，缺陷页一级动作仍被正确抑制，但工具弹框内部键盘不受影响。

### 12. 处理缺陷抽屉快捷键徽标（`HandleDefect.vue`）

处理缺陷是**只读详情 + 右上角工具按钮 + 风琴自定义属性**，与新建/编辑抽屉的「表单字段字母」场景不同，单独 mixin `handle-defect-kbd-hints.js`。

**双区徽标：**

| 区域 | 分配策略 | 触发 |
|---|---|---|
| 右上角工具按钮 | 固定首选字母（G 指派、F 修复、J 驳回、P 通过、O 打开、B 关闭、E 编辑、D 删除、U 恢复） | `Cmd/Ctrl+字母` 等效点击按钮 |
| 风琴组标题 | 视口内可见组：`1`–`9`、`0` 优先，用尽后字母池 | `Cmd/Ctrl+数字/字母` 展开对应风箱 |

**共用基础设施：** `panel-kbd-hints.js` 提供视口矩形检测、`assignDigitThenLetterKeys`、`assignToolbarHintKeys`、`resolveDrawerScrollContainer`；与统计模版页、报时提醒表格复用同一套「数字优先 + 字母兜底」分配器。

**其它：**

- 仅当 `HandleDefect` 为栈顶抽屉（`isTopDrawerVm`）时激活，避免多层抽屉错乱
- `Cmd/Ctrl+↑/↓` 滚动 `.defect-edit-body`（约 40% 视口高度），滚动后 `requestAnimationFrame` 刷新徽标
- 松开修饰键或窗口 `blur`（非原生文件选择会话）立即清除徽标

### 13. 统计模块配置弹框（`PersonalRemindTimer`、`MyLife` 等）

统计卡片点击打开的**配置型 `el-dialog`** 走 `statistic-dialog-kbd.js`（结构与缺陷工具弹框对称）：

```
statistic-dialog-kbd.js
  ├─ dialog-form-shortcuts.js
  ├─ statistic-dialog-close.js   # 未保存确认（组件自定义 getStatisticDialogCloseSnapshot）
  └─ form-field-hints.js
```

| 弹框 | 特殊键盘逻辑 |
|---|---|
| 通用 | `Cmd/Ctrl+Enter` 保存、`Esc` 关闭确认、打开后聚焦首控件 |
| 报时提醒 | 额外混入 `remind-timer-table-kbd.js`（表格行级快捷键，见 Decision 14） |

**关闭快照：** 各组件实现 `getStatisticDialogCloseSnapshot` / `serializeStatisticDialogCloseSnapshot`（如报时提醒序列化 `timers` 数组），与工具弹框 form 序列化解耦。

### 14. 报时提醒表格与时间控件键盘

报时提醒弹框内是**行式表格编辑**，字段徽标不足以覆盖「跳行 / 增删行」，因此在 `form-field-hints` 之上叠加 `remind-timer-table-kbd.js`，并重写 `$_buildFieldHints`：

| 按键 | 行为 |
|---|---|
| `Cmd/Ctrl + 1`–`9` | 聚焦对应行第一个 `[data-remind-stop]` 控件（开关列） |
| `Cmd/Ctrl + +` / `=` | 新增一行 |
| `Del` / `Backspace` | 删除当前行（名称等可编辑 `input` 内保留删字） |

**时间选择器（Element UI 特例）：** 时间面板 Popper 挂 `body`，与输入框焦点生命周期脱节，需组件内包装：

1. **失焦关面板：** `onRemindTimePickerBlur` + `dropdown-blur-close.js` / `$_closeRemindTimerPickerPanels` 遍历 `.el-date-editor` 查 `pickerVisible`
2. **确定后回焦：** 监听 `picker.$on('pick', …)`，仅 `visible === false`（点确定）时调度回焦；Spinner 滚动时 `visible === true` 不触发
3. **回车回焦：** 包装 `handleKeydown`，Enter 关闭面板后同样回焦（Element 默认会 `blur()` 输入框）
4. **静默回焦：** `_remindSilentFocus` / `_remindSuppressBlur` 包装 `handleFocus`/`blur`，避免程序化 `focus()` 再次打开面板或被 `blur()` 抢走焦点
5. **↑/↓ 重开下拉：** 面板关闭时 `ArrowUp`/`ArrowDown` 先 `handleFocus` 打开面板，再转发给 `picker.handleKeydown`

**文本/图标按钮焦点环：** `.remind-sound-preview`、`.remind-delete-btn`、`.remind-add-btn` 在 `:focus` 时使用 `--cat2bug-field-focus-color` 外描边，弥补 `el-button--text` 默认焦点不明显。

**行内 Tab：** 不使用 `←/→` 在行内控件间切换（已移除），用户通过 Tab / 字段徽标 / 数字跳行定位。

### 9. 缺陷表单与组合组件键盘集成

在缺陷新建/编辑抽屉（`AddDefect` 等）内，全局引导键 `g`/空格在输入态会被守卫；表单另有一套**直接组合键**与 **Tab 顺序** 规则，与命令面板互补。

#### 9.1 抽屉级组合键（`mixins/dialog-form-shortcuts.js` + `utils/defect-drawer-shortcuts.js`）

| 组合键 | 动作 |
|---|---|
| `Cmd/Ctrl + Enter` | 保存（`submitForm` / `shortcutSave`） |
| `Esc` | 关闭抽屉（`requestCloseDefectFormDrawer`）；有未保存修改时弹出确认 |

抽屉设置 `:close-on-press-escape="false"`，由全局捕获监听统一处理 `Esc`，避免与 Element 默认行为冲突。下拉/日期等浮层仍打开时，`Esc` 先交给浮层（`hasBlockingUiLayer({ excludeDefectFormDrawer: true })`）。

**标题栏徽标（视觉提示，非引导键面板）**

| 按钮 | 未按 `Cmd/Ctrl` | 按住 `Cmd/Ctrl` |
|---|---|---|
| 关闭 | 无徽标 | 无徽标 |
| 保存缺陷 | 无徽标 | 仅显示 `↵`（表示 `Cmd/Ctrl+Enter`） |

松开 `Cmd/Ctrl` 后 MUST 立即隐藏字段徽标与保存按钮 `↵` 提示。

**实现要点：**

- 单例栈 + DOM 解析栈顶 `AddDefect` / `EditDefectDialog` 抽屉，避免多实例注册错乱。
- `visible` 监听带 `immediate: true`；抽屉打开时 `registerDefectDrawerShortcuts`。

#### 9.2 字段快捷聚焦（`mixins/form-field-hints.js`）

| 交互 | 行为 |
|---|---|
| 按住 `Cmd/Ctrl` | 当前**可视区域内**各可输入字段标签浮现字母/数字徽标；保存按钮浮现 `↵`（关闭按钮无徽标） |
| 按住 `Cmd/Ctrl`（缺陷列表一级界面） | 工具栏/分页等控件浮现动作字母（L/E/S/J/I/G/O/B/P），与 Space 动作面板映射一致；有抽屉/浮层时不显示 |
| 按住 `Cmd/Ctrl`（统计模版页） | 预览/个人/团队三区右下角浮现 P/G/H 徽标 |
| 保持 `Cmd/Ctrl` + 字母/数字 | 焦点跳到对应字段，依赖全站统一 `:focus` / `:focus-within` 焦点环（`cat2bug.scss`）；**可连续跳转**（跳转时仅隐藏徽标，保留映射） |
| 保持 `Cmd/Ctrl` + `↑` / `↓` | 滚动表单属性区（约 40% 视口高度），徽标随滚动刷新 |
| 松开修饰键 | 徽标与映射全部清除 |

**字母池与系统保留键：**

| 类别 | 键位 | 说明 |
|---|---|---|
| 永不分配给字段 | `A` `C` `V` `X` `Z` | 系统全选/复制/粘贴/剪切/撤销；映射与按键拦截均跳过 |
| 浏览器/OS 不可拦截 | `N` `T` `W` `Q` `R` `L` `H` | 新窗口/标签/刷新/地址栏/隐藏应用等 |
| 优先分配（按字段顺序） | `D` `F` `G` `S` `E` `M` `B` `J` `K` `P` `U` `Y` `I` 等 | 约 14 个常用字母 |
| 固定占用 | `O` | 「保存本次选项」复选框（`getFixedFieldHints`，无标签徽标注入） |
| 用尽后兜底 | `1`–`9` `0` | 数字键；仅当映射命中时 `preventDefault` |

**视口分配（属性过多场景）：**

- 扫描容器默认 `getFieldHintContainer()`（新建缺陷为 `el-form` 根节点）；视口矩形取最近可滚动祖先（如 `el-drawer__body`）的 `getBoundingClientRect()`。
- 表单项垂直可见高度 ≥ 8px 才参与分配；滚出视口的字段不占键位。
- 按住 `Cmd/Ctrl` 期间监听滚动容器 `scroll` 事件，以 `requestAnimationFrame` 防抖重建映射并刷新徽标。
- 字母跳转后若目标触发 `scrollIntoView`，下一帧按新视口重新分配，便于连续操作。

`pickControl` 优先聚焦组合组件**外框**（成员/交付物/上传）或开关内部 `input`，避免双层焦点环；跳转前调用 `dismissComboPopoverIfLeaving` 关闭已展开的下拉浮层。

#### 9.3 全局 Tab 方向（`plugins/tab-direction.js`）

| 按键 | Tab 方向 |
|---|---|
| `Tab`（默认） | 正向：下一个可聚焦元素 |
| `↑` 按住 + `Tab` | 反向：上一个可聚焦元素 |
| `↓` 按住 + `Tab` | 显式正向（与默认 Tab 等价） |

在对话框/抽屉作用域内仅在当前模态容器内循环可聚焦列表。命令面板打开时不介入。

#### 9.4 组合选择与上传：单 Tab 停靠点 + 组件内按键

| 组件 | Tab | 组件内按键 | 焦点环 |
|---|---|---|---|
| `SelectProjectMember` | 外框 `.select-project-member-input` 单停靠点 | 下拉展开：`↑/↓` 选人，`←/→` 切角色 Tab，`Enter`/空格选中，`Esc` 关闭；外框直接输入字符展开并搜索 | 外框 `:focus-within` 金色边框；`Cmd+字母` 闪动时内部搜索框不重复描边 |
| `SelectModule` | 外框 `.select-module-input` 单停靠点 | 与成员选择器对齐（外框聚焦转发内部 input） | 同上 |
| `el-switch` | 仅 `.el-switch__input` 一个停靠点（`plugins/switch-keyboard.js`） | `←` 关、`→` 开；空格由原生 checkbox 切换 | `:focus-within .el-switch__core` 金色边框 |
| `FileUpload` / `ImageUpload` | 外框 `.cat2bug-upload-focus-target` 单停靠点（`utils/upload-focus-tab.js` + 全局插件） | `Enter`/空格触发选取或加号上传；Tab 在捕获阶段一次跳出上传区 | 外框 `:focus` / `:focus-within` 金色边框 |
| `el-input-number` | 与普通 input 相同 Tab 行为 | — | 抽屉内 `:focus-within`：暗色 `#ffce3a`、浅色 `#1890ff`（与普通 input 一致） |

上传/开关通过 `MutationObserver` 持续将内部 `button`、`.el-upload`、列表项等设为 `tabindex="-1"`，防止按两次 Tab 才能离开。

#### 9.5 原生文件选择框与键盘冲突（`utils/native-file-picker.js`）

打开系统文件选择框时，页面内其它键盘监听可能干扰选文件（如 `vue-excel-editor` 拦截方向键、`upload-focus-tab` 的 `focusin` 抢焦点）。

| 机制 | 行为 |
|---|---|
| 原生选文件会话 | 点击上传区时 `beginNativeFilePickerSession()`，关闭 input 时 `closeNativeFilePickerSession()` |
| Excel 表格 | 会话期间 `suspendExcelEditorKeyboard()`，暂停表格方向键处理 |
| 上传 focusin | 会话期间 `onUploadFocusIn` 不抢焦点，允许在系统文件框内用方向键选文件 |
| 字段徽标 | 松开 `Cmd/Ctrl` 立即隐藏；`Cmd+字母` 跳转后若仍按住修饰键则刷新徽标 |

#### 9.6 相关文件

```
src/plugins/tab-direction.js
src/plugins/switch-keyboard.js
src/plugins/upload-focus-tab.js
src/utils/upload-focus-tab.js
src/mixins/dialog-form-shortcuts.js
src/mixins/form-field-hints.js
src/mixins/page-action-hints.js              # Cmd/Ctrl 页面动作浮层徽标
src/mixins/defect-tool-dialog-kbd.js         # 缺陷工具弹框键盘
src/mixins/defect-tool-dialog-close.js
src/mixins/handle-defect-kbd-hints.js      # 处理缺陷抽屉徽标
src/mixins/statistic-dialog-kbd.js           # 统计配置弹框键盘
src/mixins/statistic-dialog-close.js
src/mixins/remind-timer-table-kbd.js         # 报时提醒表格行快捷键
src/mixins/layout-nav-hints.js               # ⇧⌘ 布局级导航浮层
src/utils/layout-nav-hints.js
src/utils/defect-drawer-shortcuts.js         # 抽屉 Cmd+Enter / Esc 单例监听
src/utils/defect-tool-dialog-close-state.js
src/utils/statistic-dialog-close-state.js
src/utils/defect-row-kbd-hints.js            # 缺陷列表行动态徽标分配
src/utils/statistic-item-kbd.js              # 统计模块 Enter/空格主点击映射
src/utils/statistic-grid-kbd.js              # 统计模版网格与跨区 x 对齐
src/utils/panel-kbd-hints.js                 # 面板/抽屉视口徽标分配
src/utils/remind-timer-table-kbd.js
src/utils/dropdown-blur-close.js             # 失焦关下拉（含时间面板）
src/utils/native-file-picker.js
src/utils/combo-focus-tab.js
src/assets/styles/page-kbd-hints.scss        # 页面级浮层徽标全局样式
src/assets/styles/cat2bug.scss               # 全站焦点环、MessageBox 底部间距
src/components/Project/SelectProjectMember/index.vue
src/components/Project/ProjectLabel/index.vue
src/components/Module/SelectModule/index.vue
src/components/FileUpload/index.vue
src/components/ImageUpload/index.vue
src/components/Defect/AddDefect.vue
src/components/Defect/EditDefectDialog.vue
src/components/Defect/HandleDefect.vue
src/components/Defect/DefectTools/*Dialog.vue
src/components/Cat2BugStatistic/Statistic/PersonalRemindTimer.vue
src/views/system/defect/index.vue            # 缺陷页动作 + 统计区 G 导航
src/views/system/defect/StatisticTemplate.vue
e2e/form-tab-order.spec.cjs
e2e/select-project-member-tags.spec.cjs
e2e/debug-drawer-cmd-esc.spec.cjs            # 抽屉 Esc 关闭
e2e/debug-image-filepicker-arrows.spec.cjs
e2e/remind-timer-time-picker-blur.spec.cjs   # 报时时间控件失焦/回焦
```

## Risks / Mitigations

| 风险 | 缓解 |
|---|---|
| 序列键在非输入区误触发 | 严格焦点守卫 + 引导态超时 + 仅布局内生效 |
| 菜单动态变化导致默认字母冲突 | 派生时去重分配 + 面板内唯一性校验 + 设置页标红 |
| 引导键 `空格` 与页面滚动/按钮/下拉冲突 | 缺陷页仅在一级列表且无遮挡层时打开动作面板；输入态、按钮焦点、任意 dialog/drawer/浮层打开时均不接管；可在设置页改引导键 |
| 子面板语言项过多字母不够 | 二级面板用数字 1–9 兜底 |
| keep-alive 页面注册泄漏 | 用 `activated/deactivated` 配对 `registerPage/unregisterPage` |
| 与已加载的 `v-hotkey` 局部绑定冲突 | 引擎只监听 document 顶层；局部 `v-hotkey` 仍各自生效，文档约定新功能统一走引擎 |
| 表单字段多导致快捷键不够用 | 仅对当前视口内字段分配键位，滚动后刷新；数字键 `1`–`0` 作兜底 |
| 字段快捷键误拦复制粘贴 | `A/C/V/X/Z` 永不进入分配池；按键处理双重跳过，保留系统编辑组合 |
| Element 时间面板挂 body 导致失焦/回焦异常 | 组件内包装 `handleFocus`/`blur`/`handleKeydown` + `pick` 监听；`dropdown-blur-close` 遍历 `pickerVisible` |
| 多层 dialog/drawer 快捷键串台 | `isTopDrawerVm`、单例栈、`hasBlockingUiLayer` 排除当前弹框类型 |
| 报时表格 `Del` 与输入框删字冲突 | `shouldRemindTimerDeleteRow` 在可编辑名称/数字框内不删行 |

## Open Questions

- （无，已全部确认）

## 已确认决策（原 Open Questions）

- **主题走二级面板**：`M` 打开二级面板，列「浅色 / 暗色」，再按字母切换；未来新增主题可在该二级面板扩展。
- **命令面板提供模糊搜索**：首期纳入，字母直达与模糊搜索并存（见 Decision 7）。
- **文档命名区分**：`H` 在面板显示为「帮助文档」（顶部 `system-doc`，产品使用手册），`W` 显示为「项目文档」（左侧菜单 `/project/document`，项目内业务文档），避免两个「文档」歧义。
