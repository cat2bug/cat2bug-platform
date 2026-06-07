## 1. 撰写用户指南正文（总览 Hub）

- [x] 1.1 新建 `readme/production/advanced/keyboard-shortcuts.md`，标题含 `[键盘设置](/member/keyboard)` 链接
- [x] 1.2 撰写「概述」：三层模型（引导键 / ⌘ 页面级 / ⇧⌘ 布局级）、Mac ⌘ 与 Windows Ctrl 说明
- [x] 1.3 撰写「通用操作」：`g`、空格、⌘/Ctrl、⇧⌘/Shift+Ctrl、Esc、下拉 ↑↓ Enter；输入框/弹框/抽屉时不触发的说明
- [x] 1.4 撰写「页面导航（g 面板）」默认字母速查表（对照 `keymap.js` 的 `DEFAULT_MENU_LETTERS` 与 `TOP_ITEMS`）
- [x] 1.5 重构总览：移除与各页重复的详表，改为「各页面快捷键索引」链到详情文档
- [x] 1.6 撰写「自定义快捷键」：入口、总开关、改键、冲突检测、恢复默认、localStorage 仅存本浏览器
- [x] 1.7 撰写「常见问题」：空格无反应、⌘ 无徽标、Excel 单元格编辑、与浏览器快捷键冲突
- [x] 1.8 保留系统文档页键位说明（无独立用户指南页）

## 2. 注册文档树与索引

- [x] 2.1 在 `doc/index.vue` 注册与 **AI 指南** 平级的 **高级操作** →「键盘快捷键」，`path` 为 `advanced/keyboard-shortcuts.md`
- [x] 2.2 在 `readme/production/README.md` 增加与 **AI 指南** 平级的 **高级操作** 索引
- [x] 2.3 （可选）在 `user-guide/user-management/user-profile.md` 增加指向快捷键章节的交叉引用

## 3. 各页面详情文档（本页快捷键小节）

- [x] 3.1 `user-login.md` — 登录表单 ⌘ 徽标、D-pad
- [x] 3.2 `user-register.md` — 注册表单 ⌘ 徽标
- [x] 3.3 `notification/notification-list.md` — 列表动作 S/J/G/E/B/P
- [x] 3.4 `notification/send-options.md` — 通知设置 / 发送弹框
- [x] 3.5 `current-project/defect.md` — 列表 E/U/R/I/B/P、行徽标
- [x] 3.6 `defect/defect-statistics.md` — 统计模版 P/G/H
- [x] 3.7 `table-mode/table-mode-intro.md` — 处理抽屉、工具弹框通用
- [x] 3.8 `table-mode/defect-create.md` — 新建抽屉表单
- [x] 3.9 `table-mode/defect-edit.md` — 处理缺陷抽屉（链到 intro / create）
- [x] 3.10 `excel-mode/excel-mode-intro.md` — Excel 行徽标、编辑态说明
- [x] 3.11 Table 工作流页（assign/repair/reject/pass/close/reopen/comment）— 链到 table-mode-intro 工具弹框

## 4. 配图（可选）

- [ ] 4.1 （可选）命令面板截图 → `readme/images/user-guide/user-management/keyboard-command-palette.png`
- [ ] 4.2 （可选）缺陷页 ⌘ 徽标截图 → `keyboard-page-hints.png`
- [ ] 4.3 （可选）键盘设置页截图 → `keyboard-settings.png`
- [ ] 4.4 （可选）在 Markdown 中引用上述图片

## 5. 验收

- [ ] 5.1 本地 `/system/doc` 打开「键盘快捷键」及各页详情，Markdown 渲染与 `#键盘快捷键` 锚点可用
- [x] 5.2 对照 `keymap.js`，各页默认字母表无遗漏或错误
- [ ] 5.3 手工走查：`g` + D、缺陷页空格、⌘ 徽标、索引链到 login/defect 详情节
- [x] 5.4 总览索引与各页「见 keyboard-shortcuts.md」相对链接路径正确
