## Context

`WorkbenchWidgetCatalog` 已按业务域手风琴展示，并用 `CatalogLiveThumb` 等比缩放真实组件预览。「已添加」为提示标记。组件变多后，仅靠分组仍需多次展开翻找。

约束：保持 `catalog:<type>` 拖拽契约；不改注册表持久化字段；搜索为纯前端过滤。

## Goals / Non-Goals

**Goals:**

- 标题下提供搜索输入，按本地化名称（及 type id）子串匹配（大小写不敏感）。
- 搜索态：隐藏无命中分组；命中分组全部展开以便一次扫完结果。
- 清空后恢复「同时只开一组」；无结果有空态文案。

**Non-Goals:**

- 不做拼音/模糊语义搜索、远程搜索。
- 不按描述、权限文案等多字段复杂检索。
- 不改预览实现或「已添加」语义。

## Decisions

### 1. 过滤发生在目录组件内

在 `WorkbenchWidgetCatalog` 用 `query` + `computed` 过滤 `listCatalogGroups()` 结果；不必改 `widget-registry`。匹配：`$t(titleKey)` 与 `id`，`includes` + `toLowerCase`。

备选：注册表增加 `keywords`——否决，本期名称足够。

### 2. 搜索态放开手风琴单开

有非空关键词时，所有命中组视为展开（`v-if` 为真）；折叠按钮可保留但清空前以展示结果优先。清空 `query` 后 `openGroups` 回到单开（优先含已用项的组，否则第一组）。

备选：搜索时仍单开——否决，跨组命中不便。

### 3. 空态与 i18n

无命中显示一行「无匹配组件」类文案（zh-CN / en-US）。搜索框 placeholder 同步 i18n。

## Risks / Trade-offs

- [搜索展开多组会同时挂载多个 live preview] → 与「只开一组」相比请求更多；组件库体量有限可接受；若后续卡顿再对预览做懒挂载。
- [语言切换后同一 query 结果变化] → 按当前 `$t` 过滤，符合「按名称」预期。

## Migration Plan

- 纯前端；无数据迁移。回滚：去掉搜索框与过滤逻辑即可。

## Open Questions

无。

## 相关长期文档

- `docs/01-产品需求/产品定位与核心功能.md`
- `docs/08-版本记录/2026-10.md`
