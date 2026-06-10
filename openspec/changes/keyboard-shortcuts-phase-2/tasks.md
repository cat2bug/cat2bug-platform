# keyboard-shortcuts-phase-2 任务清单

## 0. 保留键治理（优先）

- [x] 0.1 新增 `src/plugins/shortcut/reserved-keys.js`（`PAGE_ACTION_RESERVED`、`FIELD_HINT_BLOCKED`、`SETTINGS_NEVER_BIND`、`isReservedForScope`）
- [x] 0.2 重构 `page-action-hints.js` 使用统一常量
- [x] 0.3 重构 `form-field-hints.js` 使用统一常量
- [x] 0.4 重构 `defect-row-kbd-hints.js` 使用统一常量
- [x] 0.5 `shortcut-store.js` 冲突检测：重复 + 保留键拒绝
- [x] 0.6 键盘设置页：不可绑定键说明区 + 保留键校验 i18n（7 语言）
- [x] 0.7 单元测试：`reserved-keys` 与 store 冲突逻辑

## 1. keymap 与设置页

- [x] 1.1 `keymap.js` 增加 `CASE/PLAN/MODULE/REPORT/DOCUMENT/PROJECT_OPTION/TEAM_OPTION/TEAM_MEMBER/PROJECT_MANAGE/PROJECT_CREATE/PROFILE_ACTION_DEFAULTS`
- [x] 1.2 `member/keyboard/index.vue` 增加 Phase 2 分组展示与编辑

## 2a. 列表页一级动作

- [x] 2a.1 测试用例 `case/index.vue`：`registerPage` + `page-action-hints` + 工具栏 hint class
- [x] 2a.2 用例模块树 G 导航（mixin 或 TreeModule 扩展）
- [x] 2a.3 用例表格行动态 1–9
- [x] 2a.4 测试计划 `plan/index.vue`
- [x] 2a.5 交付物 `module/index.vue` + 树行导航
- [x] 2a.6 项目报告 `report/index.vue`
- [x] 2a.7 文档管理 `document/index.vue`
- [x] 2a.8 i18n：`keyboard.act.*` Phase 2 文案

## 2b. 弹框与抽屉

- [x] 2b.1 `AddCase.vue` 表单/关闭 mixin
- [x] 2b.2 CloudCase / 导入 dialog 工具弹框 mixin
- [x] 2b.3 `AddPlanDialog.vue` + 内嵌用例查询 S
- [x] 2b.4 `HandlePlanDialog.vue` Esc / Cmd+Enter（首期）
- [x] 2b.5 `ModuleDialog.vue`
- [x] 2b.6 文档新建/移动 dialog
- [x] 2b.7 `CreateTeamMember` / `InviteTeamMember`

## 2c. 设置枢纽与个人中心

- [x] 2c.1 `project/option/index.vue` 卡片 1–9
- [x] 2c.2 项目设置主要子页表单 mixin（base-info、api、defect-fields 等）
- [x] 2c.3 `team/option/index.vue` 卡片 1–9
- [x] 2c.4 `team/option/member/index.vue`
- [x] 2c.5 `user/profile/index.vue` + userInfo + resetPwd
- [x] 2c.6 `project/index.vue` 项目管理列表（`project-manage`）
- [x] 2c.7 `project/add.vue` 创建项目表单（`project-create`）

## 3. 文档（可并行 keyboard-shortcuts-user-guide）

- [x] 3.1 更新 `readme/production/advanced/keyboard-shortcuts.md` 总览
- [x] 3.2 各业务页 `## 键盘快捷键` 小节
- [x] 3.3 增加「不可绑定键 / 浏览器冲突」说明章节

## 4. 验收

- [x] 4.1 各列表页：空格面板、Cmd 徽标、遮挡层守卫（已实现 registerPage + page-action-hints + hasBlockingUiLayer，待浏览器冒烟）
- [x] 4.2 设置页：保留键 T/N/W 等拒绝保存（SETTINGS_NEVER_BIND + validateOverride）
- [ ] 4.3 Safari + Chrome（macOS）、Chrome（Windows）冒烟：⌘/Ctrl+W/T/N 不被应用拦截（需人工浏览器验证）
- [x] 4.4 `npm run lint`（ui）：项目 eslint 全局 ignore src；已 spot-check 变更文件，无新增语法错误
- [x] 4.5 归档前同步 `openspec/specs/keyboard-*/`（10 个 Phase 2 capability + settings-page 更新）
