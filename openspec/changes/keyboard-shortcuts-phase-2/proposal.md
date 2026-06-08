## Why

Phase 1（`keyboard-shortcuts`）已完成全局引擎、缺陷域、通知、系统文档、布局导航等能力；Non-Goal 明确将**缺陷页以外的业务页动作**留到后续。用户需要在测试用例、测试计划、交付物、项目报告、文档管理、项目设置、团队设置、个人中心及其子页/弹框上获得与缺陷页一致的键盘效率，并**集中记录** Safari / Chrome / Edge / Firefox 及 macOS / Windows 下不可占用的快捷键，避免实现与文档再次踩坑。

## What Changes

- **业务页一级动作**：为 8 大模块列表/枢纽页接入 `registerPage` + `page-action-hints`（双通道：`空格` 面板 + 按住 `Cmd/Ctrl` 徽标）
- **子页与弹框**：新建/编辑抽屉、`el-dialog` 复用 `form-field-hints`、`keyboard-tool-dialog-actions` 套系（`Cmd/Ctrl+Enter`、`Esc`、字段字母）
- **保留键治理**：新增 `keyboard-reserved-keys` 规格与代码侧单一数据源（`reserved-keys.js`），统一页面动作、表单字段、行动态分配、键盘设置冲突检测
- **默认键位与 i18n**：在 `keymap.js` 增加各域 `*_ACTION_DEFAULTS`，键盘设置页分组展示，7 语言文案
- **用户指南（follow-up）**：实现完成后更新 `keyboard-shortcuts-user-guide` 各业务页 `## 键盘快捷键`（本变更不阻塞文档任务）

## Capabilities

### New Capabilities

- `keyboard-reserved-keys`：浏览器/OS 保留键清单、禁用规则、设置页校验与代码常量
- `keyboard-case-actions`：测试用例列表（含模块树、分页、行跳转）
- `keyboard-case-form-actions`：用例抽屉（AddCase）、AI 创建、导入 dialog
- `keyboard-plan-actions`：测试计划列表、AddPlan 对话框、HandlePlan 执行抽屉
- `keyboard-module-actions`：交付物树表、ModuleDialog
- `keyboard-report-actions`：项目报告列表、ViewReport
- `keyboard-document-actions`：项目文档列表、新建/移动弹框
- `keyboard-project-option-actions`：项目设置卡片枢纽与子路由表单
- `keyboard-team-option-actions`：团队设置卡片枢纽、成员管理与子弹框
- `keyboard-profile-actions`：个人中心 Tab 与基本信息/改密表单

### Modified Capabilities

- `keyboard-settings-page`：新增 Phase 2 动作分组；自定义键位 MUST 拒绝保留字母；展示「不可绑定键」说明区

## Impact

- **前端**：`cat2bug-platform-ui` — `src/plugins/shortcut/`、`src/mixins/`、各 `views/system/{case,plan,module,report,document,project/option,team/option}`、`views/system/user/profile/`、`views/member/keyboard/index.vue`、i18n 7 语言
- **文档（后续）**：`readme/production/advanced/keyboard-shortcuts.md` 与各业务页详情
- **后端**：无
- **依赖**：Phase 1 已归档 `openspec/changes/archive/2026-06-08-keyboard-shortcuts/`；主规格 `openspec/specs/keyboard-*/`

## 产品决策（探索阶段已确认）

1. **模式对齐 Phase 1**：引导键序列（`g` / `空格`）+ `Cmd/Ctrl` 页面徽标；不用单独 `Cmd+字母` 做全站导航
2. **跨页一致字母**：S 查询、E 新建、B/P 翻页、J 切 Tab（有 Tab 时）
3. **文档管理页内**：O 新建文件夹、I 新建文件（避免与导航 `g+F` 语义混淆）
4. **报告新建**：E（与其它页新建一致；N 为浏览器保留单字母）
5. **设置枢纽页**：动态卡片 1–9 跳转（权限过滤后重排）
6. **保留键**：见 `keyboard-reserved-keys`；设置页与运行时分配 MUST 共用同一常量表
7. **分期实现**：2a 列表页 → 2b 弹框/抽屉 → 2c 设置枢纽与子页（见 `tasks.md`）

## Non-Goals

- 仪表盘、项目管理列表、工具浏览器等未在本变更列出的页面
- 后端持久化 / 跨设备同步
- 移动端 / 触屏
- 修改 Phase 1 已交付的缺陷域默认键位（除非保留键治理导致冲突修复）
