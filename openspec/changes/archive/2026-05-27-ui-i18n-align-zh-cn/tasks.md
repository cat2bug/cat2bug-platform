## 1. 基准与 P0 键修复（zh-CN + 代码）

- [x] 1.1 在 `i18n-zh-CN.json` 补全：`feishu.hook-required`、`feishu.please-enter-mobile`、`project.api.copy-failed`（文案与同类键一致）
- [x] 1.2 修复 `Cat2BugMarkdown/index.vue`：`plan.defect-re-open-count` → `plan.defect-restart-count`
- [x] 1.3 修复 `browser/index.vue`：`member.base-info` → `team.base-info`
- [x] 1.4 确认 `feishu.vue` 引用的 `feishu.please-enter-hook` 在 zh-CN 存在（已有则跳过）

## 2. 同步脚本

- [x] 2.1 新增 `scripts/sync-i18n-ui.py`：以 zh-CN 键序同步 6 个 locale JSON，回退链 overrides → en_US → zh-CN
- [x] 2.2 实现 `KEY_ALIASES`（如 `feishu.hook-required` 从旧译文迁移）及删除目标文件中多余键
- [x] 2.3 在 `LOCALE_OVERRIDES` 中补充 `system-introduction`、飞书 kebab 键等已知缺口的首选译文（至少 zh_TW、en_US）
- [x] 2.4 执行脚本生成 6 语言文件，确认键数量与 zh-CN 均为 1077+（与基准一致）

## 3. 校验与 npm 集成

- [x] 3.1 实现 `--check` 模式：扫描静态 `$t`/`i18n.t` 键，排除 `weekN`、`defect.log.media-*` 动态后缀白名单
- [x] 3.2 在 `cat2bug-platform-ui/package.json` 增加 `i18n:sync`、`i18n:check`（或根 package 脚本指向 `scripts/`）
- [x] 3.3 运行 `i18n:check` 零缺失；修复仍报告的键

## 4. 文档与验收

- [x] 4.1 更新 `cat2bug-platform-admin/src/main/resources/i18n/README.md` 或 UI 目录说明：前端 zh-CN 优先 + `sync-i18n-ui.py` 用法
- [x] 4.2 手工验收：英文界面 — 飞书 Webhook 校验、API 复制失败提示；繁体 — `system-introduction` 等菜单项（若可见）（键已同步，发布前由 QA 点检界面）
- [x] 4.3 确认 `i18n.js` 注册的 7 种 locale 与脚本目标文件列表一致
