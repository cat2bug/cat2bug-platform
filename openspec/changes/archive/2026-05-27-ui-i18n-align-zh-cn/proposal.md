## Why

Vue 前端 7 个 `i18n-*.json` 与代码中 `$t()` 引用未与简体中文基准（`i18n-zh-CN.json`）保持同步：其它语言缺 12–16 个键、存在已废弃的 `feishu.hook-required` 等多余键，且部分页面引用了 zh-CN 中不存在的键（如 `project.api.copy-failed`、`plan.defect-re-open-count`），切换语言后会直接显示 key 名。后端已有 `sync-i18n-messages.py`，前端缺少对等维护流程，需在规范中补齐键对齐、脚本化同步与代码引用修复。

## What Changes

- 新增 `scripts/sync-i18n-ui.py`（或等价工具）：以 `i18n-zh-CN.json` 为基准，同步 `en_US`、`zh_TW`、`ar`、`ja_JP`、`ko_KR`、`ru` 的键集合与顺序；缺失键回退 `en_US` 再回退 zh-CN 文案；支持 `KEY_ALIASES` 迁移（如 `feishu.hook-required` → `feishu.please-enter-hook`）。
- 在 `i18n-zh-CN.json` 补全代码已引用但缺失的键；将错误键名修正为与 zh-CN 一致（或补别名）。
- 批量补全各语言文件相对 zh-CN 的缺口（飞书 kebab 键、`system-introduction` 等）；移除或迁移各语言中相对基准多余的键。
- 修复 Vue/JS 中的错误 i18n 键引用（`member.base-info`、`plan.defect-re-open-count` 等）。
- 在 `cat2bug-platform-ui` 或项目 README 中补充前端 i18n 维护说明（改 zh-CN → 跑同步脚本）。
- 可选校验：`npm run` 脚本扫描 `$t()` 引用是否均存在于 zh-CN（CI 或 pre-commit 可选）。
- 不在本变更中统一飞书 camelCase / kebab-case 双套键名（仅保证两套键在 zh-CN 与各语言均存在且代码不报错）；全面重命名飞书键可另开变更。

## Capabilities

### New Capabilities

- `ui-i18n-sync`: 前端 i18n JSON 以 zh-CN 为基准的键同步脚本、别名迁移与维护约定。
- `ui-i18n-runtime-keys`: 代码引用的 i18n 键在 zh-CN 及全部 UI 语言中可解析，无裸 key 展示。

### Modified Capabilities

- （无）不修改 `openspec/specs/` 下既有业务域规格。

## Impact

- **前端**：`cat2bug-platform-ui/src/utils/i18n/*.json`、`i18n.js`（仅文档引用，一般不改编译逻辑）。
- **脚本**：`scripts/sync-i18n-ui.py`；可选 `cat2bug-platform-ui/package.json` 增加 `i18n:sync` / `i18n:check`。
- **代码修复**：`feishu.vue`、`api/index.vue`、`Cat2BugMarkdown/index.vue`、`browser/index.vue` 等少量文件。
- **文档**：`cat2bug-platform-admin/.../i18n/README.md` 或 `cat2bug-platform-ui` 内 README 增加前端段落。
- **无后端 API / 数据库变更**。
