## Why

缺陷页刷新后已能从本地恢复上次选中的 tab（仅缓存 tabId），但切换/恢复到带筛选条件的页签（尤其「我的」）时，应用 `tab.config` 会改写查询表单；状态多选、处理人等控件回抛 `@change` / `@input`，与 `scheduleTabDataLoad` 叠加，导致同一次恢复发出多次 `listDefect`。用户感知为「读了本地又打了两次列表」。需在仍只缓存 tabId、仍依赖 `configDefect` 取筛选条件的前提下，保证恢复与切 tab 时列表只请求一次。

## What Changes

- 缺陷页：程序化应用 tab 配置（恢复缓存 tab、切换自定义 tab、「全部」/「已删除」reset）期间，抑制查询区控件触发的列表拉取；由统一的 tab 数据加载入口发出**唯一一次** `listDefect`。
- 本地缓存范围不变：只持久化 **tabId**（含按项目键）；不缓存整份 query / tab.config。
- 首屏仍先读 tabId 用于 UI，再等 `configDefect` 拿到 tabs 定义后应用条件并拉列表；不在 config 返回前用错误条件抢先 list。
- 用户手动改查询条件时，行为与现网一致（仍可即时或 debounce 拉列表），不改变产品语义。
- 不改后端接口契约；不强制其它业务页同期改造（可在 design 中记可复用模式，供后续变更引用）。

## Capabilities

### New Capabilities

- `defect-tab-list-fetch`：缺陷页签恢复与切换时的列表请求编排（单次 list、抑制表单回声、只缓存 tabId）。

### Modified Capabilities

- （无）`openspec/specs/` 中无既有缺陷页签级需求规格需改写。

## Impact

- 前端：`cat2bug-platform-ui/src/views/system/defect/index.vue`（及必要时查询子控件的回声路径，如 `SelectProjectMember` 是否在父级赋值时 `$emit('input')`）。
- 后端 / API / DB：无变更。
- 文档：若沉淀可复用约定，可回写 `docs/04-UI规范/组件与交互约定.md` 一行「程序化 hydrate 期间禁止旁路 fetch」；正式 ADR 非必须。
- 探索中其它主题（列表密度、操作列三色、评审纪要 MD 壳、文档新建下拉）**不在本变更范围**，另开 change。
