## Context

Phase 1 建立了引导键模型与缺陷/通知/系统文档实现范本。Phase 2 将同一套模式扩展到项目域 8 大模块。探索结论见本变更 `proposal.md`；代码参考：

- 列表页范本：`views/notice/index.vue`
- 工具栏 + 行动态徽标：`views/system/defect/index.vue`
- 表单/弹框：`mixins/form-field-hints.js`、`mixins/defect-tool-dialog-kbd.js`

当前保留键分散在 `page-action-hints.js`（`RESERVED_LETTERS`）、`form-field-hints.js`（`FIELD_HINT_KEYS_BLOCKED`）、`defect-row-kbd-hints.js`（`ROW_KBD_RESERVED`），Phase 2 MUST 收敛为单一模块并在设置页校验中复用。

## Goals / Non-Goals

**Goals:**

- 8 大业务模块一级界面动作 + 主要子页/弹框键盘集成
- 浏览器（Chrome / Safari / Firefox / Edge）与 OS（macOS / Windows）冲突键**文档化 + 代码禁用**
- 键盘设置页可配置 Phase 2 动作，冲突检测含保留键
- 与 Phase 1 交互模型 100% 一致

**Non-Goals:**

- 不引入 `Cmd/Ctrl+Shift+字母` 等新修饰档位（除已有 `Shift+Cmd` 布局导航）
- 不改后端；不做跨端同步

## Decisions

### 1. 继续引导键模型（规避浏览器保留组合）

```
导航     g  + 字母  →  居中面板  →  跳转（无修饰键，网页可拦截）
动作     空格 + 字母  →  居中面板  →  执行页面动作
页面徽标  ⌘/Ctrl + 字母  →  控件角标直接执行（单字母，非浏览器保留组合）
布局徽标  ⇧⌘/Shift+Ctrl + 字母  →  侧栏/顶栏（与页面 ⌘ 互斥）
```

**为何不用 `Cmd+字母` 做全局导航：** 见 Decision 2。Phase 2 业务页动作仍用 **单字母 + 按住 Cmd**（与缺陷/通知一致），字母池受保留键约束。

### 2. 浏览器与操作系统保留键（集中记录）

以下组合在 Web 页面中**无法可靠拦截**或拦截后会破坏用户预期；系统 MUST NOT 将其作为可绑定快捷键，并在键盘设置页标注为禁用。

#### 2.1 macOS + Safari / Chrome / Edge / Firefox（⌘ = Command）

| 组合 | 系统行为 | 平台 | 引擎策略 |
|------|----------|------|----------|
| ⌘+W | 关闭标签页 | 全浏览器 | 禁止 ⌘+W 映射；导航「文档」用 `F` 而非 W（见 Phase 1） |
| ⌘+Shift+W | 关闭窗口 | Safari 等 | 禁止 Shift+⌘+W 用于布局/页面 |
| ⌘+T | 新标签页 | 全浏览器 | 单字母 **T** 禁入页面动作/字段池 |
| ⌘+N | 新窗口 | 全浏览器 | 单字母 **N** 禁入页面动作池（报告「新建」用 N 仅在不按 ⌘ 时通过空格面板，⌘+N 仍交给浏览器） |
| ⌘+Shift+N | 无痕窗口 | Chrome | 不映射 |
| ⌘+Q | 退出应用 | macOS | 单字母 **Q** 禁入页面动作池 |
| ⌘+R | 刷新页面 | 全浏览器 | 单字母 **R** 禁入字段池；导出等动作用上下文空格面板 |
| ⌘+Shift+R | 硬刷新 | 全浏览器 | 不映射 |
| ⌘+L | 聚焦地址栏 | Chrome；Safari 同类 | 单字母 **L** 禁入字段池；登录提交等不得文档化为「⌘+L」 |
| ⌘+D | 添加书签 | 全浏览器 | 注意与导航缺陷 D 区分（`g+D` 无冲突） |
| ⌘+F | 页内查找 | 全浏览器 | 字段优先池可用 F，但 ⌘+F 仍由浏览器处理 |
| ⌘+G | 查找下一个 | 全浏览器 | 统计模块导航 G 仅在空格/Cmd 单字母上下文 |
| ⌘+P | 打印 | 全浏览器 | 系统文档打印用空格/Cmd+P 单字母，不拦截 ⌘+P |
| ⌘+S | 保存网页 | 全浏览器 | 不映射 ⌘+S |
| ⌘+A/C/V/X/Z | 编辑 | 全浏览器 | **永不**分配给字段或页面动作 |
| ⌘+Tab | 切换应用 | macOS | OS 级，不映射 |
| ⌘+H | 隐藏应用 | macOS | 单字母 **H** 禁入字段池 |
| ⌘+M | 最小化窗口 | macOS | 单字母 **M** 禁入页面动作池 |
| ⌘+1 … ⌘+9 | 切换标签 | Chrome/Edge | 表单字段映射数字时仅 `preventDefault` **已分配** 的数字 |
| ⌘+Plus/Minus | 缩放 | 全浏览器 | 不映射 |
| ⌘+0 | 重置缩放 | 全浏览器 | 字段兜底数字 0 谨慎使用 |

#### 2.2 Windows / Linux + Chrome / Edge / Firefox（Ctrl）

| 组合 | 系统行为 | 引擎策略 |
|------|----------|----------|
| Ctrl+W | 关闭标签 | 同 ⌘+W |
| Ctrl+T | 新标签 | 单字母 T 禁用 |
| Ctrl+N | 新窗口 | 单字母 N 禁用（页面动作池） |
| Ctrl+Shift+N | 无痕 | 不映射 |
| Ctrl+R / F5 | 刷新 | R 禁入字段池 |
| Ctrl+Shift+R | 硬刷新 | 不映射 |
| Ctrl+L / Alt+D | 地址栏 | L 禁入字段池 |
| Ctrl+F | 查找 | 同 ⌘+F |
| Ctrl+P | 打印 | 同 ⌘+P |
| Ctrl+S | 保存 | 不映射 |
| Ctrl+A/C/V/X/Z | 编辑 | 同 macOS |
| Ctrl+Tab | 切换标签 | 不映射 |
| Alt+F4 | 关闭窗口 | OS 级 |
| Ctrl+1…9 | 切换标签 | 同 ⌘+数字 |

#### 2.3 单字母禁用池（代码常量，按场景）

| 常量 | 字母 | 适用场景 | 原因摘要 |
|------|------|----------|----------|
| `PAGE_ACTION_RESERVED` | A, C, M, N, Q, T, V, X, Z | `page-action-hints`、行动态分配、`registerPage` 默认值校验 | 编辑快捷键 + 浏览器窗口/标签/退出 |
| `FIELD_HINT_BLOCKED` | 上列 + **N, T, W, Q, R, L, H** | `form-field-hints` | 窗口/刷新/地址栏/隐藏应用 |
| `ROW_KBD_RESERVED` | 同 PAGE_ACTION | 表格行动态徽标 | 与工具栏去重且避免编辑键 |
| `SETTINGS_NEVER_BIND` | PAGE ∪ FIELD 并集 | 键盘设置页保存校验 | 用户不可自定义为保留字母 |

> **W** 未进入 PAGE_ACTION 池（交付物/关闭窗口语义），但进入 FIELD_HINT_BLOCKED（⌘+W）。**L** 未进入 PAGE_ACTION（缺陷切换项目 L 通过空格/Cmd 单字母，Safari 地址栏风险在文档中说明「见屏上徽标」）。

#### 2.4 实现：单一数据源

新增 `src/plugins/shortcut/reserved-keys.js`：

```javascript
export const PAGE_ACTION_RESERVED = new Set([...])
export const FIELD_HINT_BLOCKED = new Set([...])
export const ROW_KBD_RESERVED = PAGE_ACTION_RESERVED
export const SETTINGS_NEVER_BIND = new Set([...PAGE, ...FIELD])
export function isReservedForScope(letter, scope) { ... }
```

`page-action-hints.js`、`form-field-hints.js`、`defect-row-kbd-hints.js`、`shortcut-store.js`（冲突检测）MUST 改为从此模块 import，禁止各文件维护独立列表。

### 3. Phase 2 各模块默认动作

#### 3.1 测试用例 `scopeKey: case`

| 键 | 动作 | 方法/锚点 |
|----|------|-----------|
| S | 聚焦查询 | 首个查询 input |
| E | 新建用例 | `handleAdd` / split-button 主按钮 |
| U | 导入 | 下拉项 `.case-hint-import` |
| R | 导出 | 下拉项 `.case-hint-export` |
| A | AI 创建 | `handleCloudCaseAdd` |
| D | 批量删除 | 需已勾选 |
| M | 切换模块树 | `toggleModuleTreeVisible` |
| G | 模块树导航 | 树可见时；↑↓/Enter/Esc（复用 TreeModule 或专用 mixin） |
| B/P | 分页 | `.case-table-pagination` |

**行动态：** 可见行 `caseNum` 列 1–9 打开编辑（`getPageDynamicActionHints`）。

#### 3.2 测试计划 `scopeKey: plan`

| 键 | 动作 |
|----|------|
| S | 聚焦查询 |
| E | `planDialog.openAdd()` |
| B/P | 分页 |

**行跳转：** 1–9 聚焦行；Enter 打开执行（`handlePlanRun`）或默认第一项操作。

**AddPlanDialog：** `keyboard-tool-dialog-actions` + 内嵌用例区 S 查询。

**HandlePlanDrawer：** 独立键盘 scope；Esc 关闭确认；用例项列表方向键（P2 可简化为表单套系 + 滚动）。

#### 3.3 交付物 `scopeKey: module`

| 键 | 动作 |
|----|------|
| S | 聚焦查询 |
| T | 展开/折叠全部 |
| E | 根级新建 |

**树导航模式：** ↑↓ 行移动，Enter 编辑，Right 展开，Left 折叠，Esc 退出。

**ModuleDialog：** 工具弹框套系。

#### 3.4 项目报告 `scopeKey: report`

| 键 | 动作 |
|----|------|
| S | 聚焦查询 |
| E | 新建报告（触发 template-select create；N 为保留键） |
| D | 批量删除 |
| B/P | 分页 |

**ViewReport：** Esc 关闭；只读不设字段池。

#### 3.5 文档管理 `scopeKey: document`

| 键 | 动作 |
|----|------|
| S | 聚焦查询 |
| O | 新建文件夹 |
| I | 新建文件 |
| U | 上级目录 / 返回 |
| B/P | 分页 |

**弹框：** 新建/重命名、移动 → 工具弹框套系。

#### 3.6 项目设置 `scopeKey: project-option`

| 键 | 动作 |
|----|------|
| 1–9 | 按可见卡片顺序激活首个链接 |
| B | 子页 `el-page-header` 返回 |

子路由表单页：S 不适用时省略；表单字段 + ⌘+Enter 保存。

#### 3.7 团队设置 `scopeKey: team-option`

同项目设置卡片模式。成员列表页 `team-member` scope：S 搜索、E 创建、I 邀请、B 返回。

#### 3.8 个人中心 `scopeKey: profile`

| 键 | 动作 |
|----|------|
| J | 切换 Tab（基本信息 / 改密） |
| B | 返回 |

`userInfo` / `resetPwd`：表单套系。

### 4. 注册与生命周期

与 Phase 1 相同：

- `activated` → `registerPage` + mixin 绑定
- `deactivated` / `beforeDestroy` → `unregisterPage`
- 遮挡层守卫：`hasBlockingUiLayer()`
- `keep-alive` 路由必须配对注销

### 5. 键盘设置页扩展

分组新增：

- 测试用例 / 测试计划 / 交付物 / 项目报告 / 文档管理 / 项目设置 / 团队设置 / 个人中心
- **不可绑定键** 折叠说明（引用 `keyboard-reserved-keys` 表格摘要）

保存时：若用户输入字母 ∈ `SETTINGS_NEVER_BIND`，MUST 拒绝并 i18n 提示原因类别（浏览器窗口 / 系统编辑 / 地址栏等）。

### 6. 文件触点（预期）

```
src/plugins/shortcut/reserved-keys.js          # 新增
src/plugins/shortcut/keymap.js                 # *_ACTION_DEFAULTS
src/views/system/case/index.vue
src/components/Case/AddCase.vue
src/views/system/plan/index.vue
src/components/Plan/AddPlanDialog.vue
src/components/Plan/HandlePlanDialog.vue
src/views/system/module/index.vue
src/components/Module/ModuleDialog.vue
src/views/system/report/index.vue
src/views/system/document/index.vue
src/views/system/project/option/index.vue
src/views/system/team/option/index.vue
src/views/system/team/option/member/index.vue
src/views/system/user/profile/index.vue
src/views/member/keyboard/index.vue
```

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 模块树 G 导航与缺陷统计 G 行为不一致 | 共用 tree-nav 工具函数；spec 写清 Enter/Esc |
| HandlePlan 复杂度高 | tasks 拆 2b 后期；首期可仅 Esc + 表单 |
| 卡片 1–9 随权限变化 | 运行时构建 hints，`visible` 过滤 |
| 保留键列表遗漏 | `reserved-keys.js` 单源 + 设置页说明 + 用户指南链接 |
| ⌘+单字母仍与浏览器偶发冲突 | 依赖空格面板兜底；文档强调「以屏上徽标为准」 |

## Open Questions

1. HandlePlan 抽屉是否首期做用例项方向键导航，还是仅工具栏 + 表单？
2. 项目设置子路由是否逐个 spec 还是按「表单页通用规则」批量覆盖？

（默认：2a 只做列表；2b 做弹框；子路由表单按通用规则批量实现，不单独 spec 每个路由。）
