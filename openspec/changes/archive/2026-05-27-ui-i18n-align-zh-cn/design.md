## Context

Cat2Bug UI 使用 `vue-i18n`，消息来自扁平 JSON（`i18n-zh-CN.json` 等 7 文件），与 Element UI locale 通过 `Object.assign` 合并。当前约 **1077** 个键；`en_US` 等比 zh-CN 少 **16** 键、多 **1** 个废弃键 `feishu.hook-required`。zh-CN 近期增加了飞书 kebab-case 键（`feishu.app-id` 等），代码仍主要引用 camelCase（`feishu.appId`）。扫描发现 **P0** 断裂：`feishu.hook-required`、`feishu.please-enter-mobile` 在代码中使用但 zh-CN 缺失；`plan.defect-re-open-count` 与 zh-CN 的 `plan.defect-restart-count` 不一致；`project.api.copy-failed` 全语言缺失；`member.base-info` 应改为已有 `team.base-info`。

后端 `scripts/sync-i18n-messages.py` 已提供 properties 同步模式，本设计在其思路上为 JSON 实现轻量版本。

## Goals / Non-Goals

**Goals:**

- 七个 locale JSON 与 zh-CN **键集合一致**（顺序一致便于 diff）。
- 所有静态 `$t('key')` / `i18n.t('key')` 在 zh-CN 中存在；切换任一 UI 语言时不显示裸 key。
- 提供可重复执行的 `sync-i18n-ui.py` 与文档，新增键流程为：先改 zh-CN → 跑脚本 → 补 `LOCALE_OVERRIDES` 译文（可选）。
- 修复已知的错误键引用与 P0 缺失键。

**Non-Goals:**

- 不翻译质量全面审校（非中文语言允许 en/zh-CN 回退占位，后续人工改）。
- 不在此变更删除 200+ 未使用键（避免大范围 diff）；可记录为 follow-up。
- 不合并前后端 i18n 为单一数据源。
- 不重构飞书模块为单一命名风格（camelCase vs kebab-case 双轨保留，但两套键均写入 zh-CN 并同步到其它语言）。

## Decisions

### 1. 基准文件与 locale 映射

**选择**：`cat2bug-platform-ui/src/utils/i18n/i18n-zh-CN.json` 为唯一基准；目标文件与 `i18n.js` 中 `messages` 键一致：

| 文件 | vue-i18n locale |
|------|-----------------|
| i18n-en-US.json | en_US |
| i18n-zh-TW.json | zh_TW |
| i18n-ar.json | ar |
| i18n-ja-JP.json | ja_JP |
| i18n-ko-KR.json | ko_KR |
| i18n-ru.json | ru |

**理由**：与产品默认语言及 explore 结论一致。

### 2. 同步脚本行为（仿 `sync-i18n-messages.py`）

**选择**：`scripts/sync-i18n-ui.py`：

1. 读取 zh-CN 键顺序；
2. 对每个目标 locale：保留已有译文，缺键用 `LOCALE_OVERRIDES[file][key]` → `en_US` 值 → zh-CN 值；
3. 应用 `KEY_ALIASES`（旧键 → 新键）从旧 locale 迁移值后删除旧键；
4. 输出 JSON，键顺序与 zh-CN 相同，UTF-8，`ensure_ascii=False`。

**初始 `KEY_ALIASES`（示例）**：

| 旧键 | 新键（zh-CN 权威） |
|------|-------------------|
| `feishu.hook-required` | `feishu.please-enter-hook`（或同时保留两键同值，见下） |

**选择（飞书校验）**：zh-CN **同时保留** `feishu.hook-required` 与 `feishu.please-enter-hook`（文案可相同或略有区分），因 `feishu.vue` 表单 rules 与 `$message` 各用其一；其它语言同步两键，避免只迁移别名导致 rules 仍缺键。

### 3. 代码修复策略

| 问题 | 处理 |
|------|------|
| `plan.defect-re-open-count` | 改为 `plan.defect-restart-count`（与 zh-CN 一致） |
| `member.base-info` | 改为 `team.base-info` |
| `project.api.copy-failed` | 在 zh-CN 新增文案，脚本扩散到其它语言 |
| `feishu.please-enter-mobile` | 从 `enterprise-weishu.please-enter-mobile` 类推，写入 zh-CN |

**理由**：优先改代码匹配已有 zh-CN，减少新键扩散。

### 4. 校验脚本

**选择**：`scripts/check-i18n-ui-keys.py` 或合并进 sync 的 `--check` 模式：rg 提取 `src` 下静态 key，报告不在 zh-CN 的 key；动态拼接（`week` + n、`defect.log.media-*` + suf）用白名单或后缀规则排除。

### 5. kebab 飞书键

**选择**：zh-CN 已有 `feishu.app-id` 等但代码未引用 — **同步到所有语言**，不删 zh-CN 键（为将来 UI 切换预留），不在此变更改 Vue。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 大批量 JSON 重排导致 git diff 噪音 | 脚本固定键顺序；按 locale 分 commit 可选 |
| en 回退中文占位 | 接受；`LOCALE_OVERRIDES` 逐步补全 |
| 别名迁移覆盖人工译文 | 仅当旧键存在且新键为空时迁移 |
| 动态 `$t` 误报 | 维护 `DYNAMIC_KEY_PREFIXES` 白名单 |

## Migration Plan

1. 先补 zh-CN 与代码修复（P0）。
2. 运行 `sync-i18n-ui.py` 更新 6 个语言文件。
3. 运行 `--check` 确认零缺失。
4. 手工抽测：英文界面飞书通知设置、API Key 复制失败提示、计划 Markdown 缺陷重启次数字段。
5. 回滚：还原 JSON 与少量 Vue 改动即可。

## Open Questions

- `system-introduction` 是否在导航中暴露？若暴露须在 sync 时从 zh-CN 扩散；若未使用可暂不加入基准（当前 zh-CN 有，建议同步）。
- 是否在 CI 中强制 `i18n:check`？建议先本地 npm script，CI 作为 follow-up。
