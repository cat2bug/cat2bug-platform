## Why

键盘快捷键功能（`keyboard-shortcuts`）已在应用中落地，但文档体系中缺少总览入口；各业务页也缺少本页键位说明。

需要在文档体系中提供快捷键总览：**高级操作** 与 **AI 指南** 为文档树同级目录，键盘快捷键总览挂在 **高级操作** 下；各业务页详情保留 `## 键盘快捷键` 小节。

## What Changes

- 新增总览 Markdown：`readme/production/advanced/keyboard-shortcuts.md`（**总览 hub**）
- 在 `doc/index.vue` 注册与 **AI 指南** 平级的 **高级操作** 目录，其下挂载「键盘快捷键」
- **各页面详情文档**内增加「键盘快捷键」小节，写入该页专属键位（登录、注册、通知、缺陷列表/抽屉/弹框、统计模版等）
- 总览页通过索引表链到各页面详情，避免长文重复维护
- 在 `readme/production/README.md` 增加与 **AI 指南** 平级的 **高级操作** 索引
- 可选：在 `user-profile.md` 增加指向快捷键章节的交叉引用
- 可选：补充截图至 `readme/images/user-guide/user-management/`（命令面板、⌘ 徽标、键盘设置页）
- **不修改** 快捷键引擎代码、i18n 键、默认键位；文档以 `keymap.js` 与 `openspec/specs/keyboard-*/` 为权威来源

## Capabilities

### New Capabilities

- `user-guide-keyboard-shortcuts`：用户指南中键盘快捷键章节的结构、内容范围、文档树注册与验收标准

### Modified Capabilities

- （无）不修改既有 `keyboard-*` 实现规格；仅新增面向终端用户的文档规格

## Impact

- **文档**：`readme/production/advanced/keyboard-shortcuts.md`（总览 hub）
- **文档树**：`doc/index.vue` — **高级操作**（与 AI 指南平级）→ 键盘快捷键
- **索引**：`readme/production/README.md` — **高级操作**（与 AI 指南平级）
- **可选交叉引用**：`readme/production/user-guide/user-management/user-profile.md`
- **可选图片**：`readme/images/user-guide/user-management/keyboard-*.png`
- **无后端 / 无前端业务逻辑变更**

## 产品决策（探索阶段已确认）

1. **Hub + 分页**：`keyboard-shortcuts.md` 承担总览与导航索引；各业务页面详情文档承载本页键位表
2. **挂载位置**：**高级操作** 为文档树顶级目录，与 **AI 指南** 平级；其下挂载「键盘快捷键」总览
3. **语言**：首期仅简体中文（与现有 `readme/production` 一致）
4. **键位权威来源**：`cat2bug-platform-ui/src/plugins/shortcut/keymap.js` + 用户自定义说明（键盘设置页）
5. **截图**：首期可纯文字上线，截图作为可选 follow-up 任务

## 依赖

- 功能已实现并归档：`openspec/changes/archive/2026-06-08-keyboard-shortcuts/`
- 主规格已同步：`openspec/specs/keyboard-*/spec.md`（14 个能力，作文档验收清单）
