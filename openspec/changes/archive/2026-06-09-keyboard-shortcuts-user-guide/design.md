## Context

Cat2Bug 用户文档采用 Markdown 存放于 `readme/production/`，通过 Vue 文档页 `views/doc/index.vue` 加载并在左侧树展示。现有用户指南章节遵循统一格式：

- 标题含应用内路由链接，如 `# 登录 [/login](/login)`
- 「概述」→ 功能说明 → 表格/步骤 → 「常见问题」
- 截图路径：`readme/images/user-guide/...`

快捷键功能采用 **三层交互模型**（见已归档 `keyboard-shortcuts/design.md`）：

```
┌─────────────────────────────────────────────────────────────┐
│  ① 引导键 + 字母     g → 导航面板    空格 → 当前页动作面板   │
│  ② 按住 ⌘/Ctrl       页面级控件右下角字母徽标               │
│  ③ 按住 ⇧⌘/Shift+Ctrl  布局级：侧栏、顶栏、折叠按钮         │
└─────────────────────────────────────────────────────────────┘
```

文档须用**用户语言**解释上述模型，避免复制 OpenSpec 技术规格原文。

## Goals / Non-Goals

**Goals:**

- 新用户阅读一章即可理解：何时用 `g`、何时用空格、何时按住 ⌘/Ctrl
- 提供**默认键位速查表**（导航、缺陷页、登录/注册、通知、系统文档页、统计模版）
- 说明键盘设置入口、自定义键位、冲突检测、localStorage 仅存本浏览器
- 说明输入框/弹框/抽屉打开时的限制（为何不响应）
- 注册到文档树与 README，在 `/system/doc` 可浏览

**Non-Goals:**

- 不写开发者 API（`registerPage`、`ShortcutService` 等）
- 不做多语言用户文档（en/ja 等）
- 不在此变更修改默认键位或应用内 i18n
- 不替代「键盘设置」页的内联说明（文档与之互补）

## Decisions

### 1. 文件路径与文档树位置

**选择**：`readme/production/advanced/keyboard-shortcuts.md`

**文档树**：**高级操作** 与 **AI 指南** 为同级顶级节点：

```javascript
{
  label: 'AI 指南',
  children: [{ label: 'MCP 接入', path: 'ai-guide/mcp.md', ... }]
},
{
  label: '高级操作',
  children: [
    {
      label: '键盘快捷键',
      path: 'advanced/keyboard-shortcuts.md',
      icon: 'el-icon-setting'
    }
  ]
}
```

**理由**：高级操作（键盘、后续效率类能力）与 AI 能力分域，互不嵌套。

### 2. 文档结构（Hub + 各页面详情）

**总览** `keyboard-shortcuts.md`：

| H2 节 | 内容要点 |
|-------|----------|
| 概述 | 三层模型；链到 `/member/keyboard` |
| 通用操作 | g、空格、⌘、⇧⌘、Esc、守卫条件 |
| 页面导航（g） | 侧栏 + 顶栏默认字母表 |
| 各页面快捷键索引 | 表格链到各详情文档 `#键盘快捷键` 锚点 |
| 自定义快捷键 | 键盘设置页 |
| 常见问题 | 全局 FAQ |

**各页面详情**（在对应 Markdown 末尾或「常见问题」前增加 `## 键盘快捷键`）：

| 用户指南文档 | 键位范围 |
|-------------|----------|
| `user-login.md` | 登录表单 ⌘ 徽标、D-pad |
| `user-register.md` | 注册表单 ⌘ 徽标 |
| `notification/notification-list.md` | 通知列表空格/⌘ 动作 |
| `notification/send-options.md` | 通知设置/发送弹框 |
| `current-project/defect.md` | 缺陷列表 E/U/R/I/B/P、行徽标 |
| `defect/defect-statistics.md` | 统计模版 P/G/H |
| `table-mode/table-mode-intro.md` | 处理抽屉、工具弹框通用 |
| `table-mode/defect-create.md` | 新建抽屉表单 |
| `table-mode/defect-edit.md` | 处理缺陷抽屉（与 intro 互补） |
| `table-mode/defect-*.md`（指派/修复等） | 链到 intro 工具弹框说明 |
| `excel-mode/excel-mode-intro.md` | Excel 列表行徽标、编辑态说明 |

每页小节开头 MUST 链回总览：`通用说明见 [键盘快捷键](相对路径至 advanced/keyboard-shortcuts.md)`。

### 3. 默认键位数据来源

**选择**：撰写时对照以下文件，表格列「默认字母」须与代码一致：

| 范围 | 来源 |
|------|------|
| 引导键 | `keymap.js` → `DEFAULT_LEADERS` |
| 侧栏菜单 | `DEFAULT_MENU_LETTERS` |
| 顶栏 | `TOP_ITEMS` |
| 缺陷页动作 | `DEFECT_ACTION_DEFAULTS` + `index.vue` `registerDefectShortcuts` |
| 统计模版 | `STATISTIC_TEMPLATE_ACTION_DEFAULTS` |
| 登录/注册 | `LOGIN_ACTION_DEFAULTS` / `REGISTER_ACTION_DEFAULTS` |
| 通知 | `NOTICE_ACTION_DEFAULTS` |
| 系统文档页 | `DOC_ACTION_DEFAULTS` |
| 布局级 | `LAYOUT_*` 常量 + `keyboard-layout-nav-hints` 规格 |

**注意**：早期 proposal 中部分缺陷页字母（如 L/S/J/O）已与实现对齐为 E/U/R/I 等，**以 keymap 为准**。

### 4. 与 keyboard-* 规格的关系

文档各节对应 OpenSpec 主规格，用于实施时核对完整性，不在用户文档中引用 spec 文件名：

| 文档节 | 规格 |
|--------|------|
| 通用 / 引导键 | `keyboard-shortcut-engine`, `keyboard-command-palette` |
| g 导航 | `keyboard-navigation-map` |
| ⇧⌘ 布局 | `keyboard-layout-nav-hints` |
| 缺陷列表 | `keyboard-defect-actions` |
| 缺陷表单 | `keyboard-form-integration` |
| 工具弹框 | `keyboard-tool-dialog-actions` |
| 处理抽屉 | `keyboard-handle-defect-actions` |
| 统计弹框 | `keyboard-statistic-dialog-actions` |
| 统计模版页 | `keyboard-statistic-template-actions` |
| 通知弹框 | `keyboard-notice-dialog-actions` |
| 系统文档页 | `keyboard-doc-page-actions` |
| 登录 D-pad | `keyboard-login-mouse-control` |
| 设置页 | `keyboard-settings-page` |

### 5. 交叉引用

**选择**：在 `user-profile.md` 末尾或「个人设置」小节增加一句：

> 如需自定义键盘快捷键，请参阅 [键盘快捷键](../../advanced/keyboard-shortcuts.md)。

**理由**：个人中心是用户自然寻找设置的位置。

### 6. 截图策略

**选择**：tasks 中标记为可选；若提供，建议 3 张：

1. `g` 或空格打开的命令面板
2. 缺陷页按住 ⌘ 的工具栏徽标
3. 键盘设置页 `/member/keyboard`

路径：`readme/images/user-guide/user-management/keyboard-command-palette.png` 等。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 键位随版本变更导致文档过时 | 文档注明「默认键位以键盘设置页为准」；改 keymap 时同步更新本章 |
| 单页过长难浏览 | 文首加「目录锚点」或简短速查表；H2 分节清晰 |
| 动态字母（表格行、表单字段）无法穷举 | 说明「按住 ⌘ 显示当前可见项字母」，不给固定表 |
| doc 树 icon 不存在 | 选用已有 icon 名（如 `guide`）或 Element 图标类 |

## Migration Plan

1. 撰写 `keyboard-shortcuts.md`
2. 更新 `doc/index.vue` 与 `README.md`
3. 可选更新 `user-profile.md`
4. 本地打开 `/system/doc` 验证树节点与 Markdown 渲染
5. 手工对照键盘设置页默认键位走查 5–10 个常用操作

## Open Questions

- 文档树图标用 `keyboard` 还是复用现有 `guide`/`setting`？实施时以 `index.vue` 已有 icon 为准
- 是否在「缺陷介绍」`defect.md` 增加指向快捷键章节的链接？建议 follow-up，避免本变更范围扩散
