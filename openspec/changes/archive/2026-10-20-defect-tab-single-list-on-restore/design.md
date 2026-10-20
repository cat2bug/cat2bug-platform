## Context

缺陷页（`views/system/defect/index.vue`）本地只缓存当前页签 id（`defect-tab` / `defect-tab:<projectId>`）。刷新流程大致为：

1. `created`：`applyDefectTabCacheEarly` 读 tabId，只同步 `activeDefectTabName`（无筛选条件）。
2. `init` → `configDefect`：拿到 `config.tabs`（含「我的」等 tab 的 `config` 查询条件）。
3. `selectDefectTabHandle`：把 `tabItem.config` 赋给 `queryParams`，再 `scheduleTabDataLoad` → `handleQuery` → `listDefect`。

问题出在第 3 步：赋值会改动 `queryParams.params.defectStates`、`queryParams.handleBy` 等；模板上 `@change="handleQuery()"` / `@input="handleQuery()"` 以及 `SelectProjectMember` 在父级改 `memberId` 后可能再 `$emit('input')`，与 debounce 的 `scheduleTabDataLoad` 叠加，同一次恢复出现多次列表请求。尤其「我的」带处理人与状态筛选时最明显。

约束（已与产品对齐）：**只缓存 tabId**；筛选条件仍以服务端 tabs 为准；不在 config 返回前用默认条件抢先 list。

## Goals / Non-Goals

**Goals:**

- 刷新恢复缓存 tab（含「我的」）时：`configDefect` 一次 + `listDefect` **一次**（统计面板另计，本变更不强制改统计并发）。
- 用户点击切换页签时：应用条件后同样只触发一次列表加载入口（表单回声不额外 list）。
- 手动改查询条件仍能拉列表，语义不变。
- 保持现有 tabId 本地缓存与「已删除」不落盘策略。

**Non-Goals:**

- 不缓存 tab.config / queryParams 快照。
- 不改为「先 list 再等 config」。
- 不改造其它业务列表页（仅在设计中留下可复用模式说明）。
- 不处理列表密度、操作列颜色、文档新建下拉、评审纪要 MD 壳（探索中其它主题另开变更）。
- 不要求去掉 `handleRefreshQuery` 里的统计刷新（可记为后续优化）。

## Decisions

### D1：用「抑制标志」包住程序化应用 query，而不是改掉所有 @change

- **选择**：在 `selectDefectTabHandle`（及同类 reset/应用 tab 路径）外层设 `_suppressDefectQueryFetch`（或等价命名）；`handleQuery` / 表单回调入口若见标志则直接 return；`finally` 清标志后再调用唯一的 `scheduleTabDataLoad`。
- **理由**：改动面集中在缺陷页父组件；不必立刻改 `SelectProjectMember` 的 emit 语义（其它页可能依赖 input）。
- **备选**：所有列表一律走共享 debounce——也能合并，但手动输入时的手感与现网即时 `@input` 可能不一致；本次以抑制标志为主，debounce 保留作切 tab 合并。

### D2：只缓存 tabId，config 串行后再唯一 list

- **选择**：维持 early 读 tabId → `configDefect` → 应用 config → 单次 list。
- **理由**：无本地筛选快照时，正确条件只能来自服务端 tabs；抢先 list 会变成「错条件 + 再纠正」的双 list，更糟。
- **备选**：缓存 config 快照（已否决）。

### D3：程序化改 `activeDefectTabName` 不额外走 `@tab-change` 拉列表

- **选择**：恢复路径以显式 `selectDefectTabHandle()` 为准；避免「tab-change + 显式调用」双入口。Element Plus 对 v-model 程序赋值默认不带 `tabChange`（`trigger=false`）；实现时勿在 watch `activeDefectTabName` 里再调 `selectDefectTabHandle`（现有 watch 仅落盘，保持）。
- **备选**：去掉 `@tab-change`、只靠 watch——会搅乱落盘与加载职责，不采用。

### D4：验收以 Network 中 `listDefect`（或等价缺陷列表 API）次数为准

- **选择**：硬刷新停在「我的」、或从「全部」点到「我的」，在 config 返回后的短窗口内列表接口只应出现 1 次（忽略被 gen 丢弃的过期响应若仍发出则算失败，目标是根本不发多余请求）。
- **理由**：与用户感知一致；表格内已有 `defectListRequestGen` 只能丢响应，不能阻止多余请求。

## Risks / Trade-offs

- [抑制窗口过长] → Mitigation：仅包住「写 queryParams + 同步子状态」的同步段与紧随的 `$nextTick`（若 SelectProjectMember 异步 emit）；用 try/finally，超时兜底清标志。
- [漏抑制导致仍双请求] → Mitigation：切「我的」用 Network 回归；必要时对 `handleQuery` 打一次性 debug 计数（开发自测，不入库）。
- [抑制误伤手动操作] → Mitigation：标志仅在程序化路径置位；用户点击产生的事件发生在标志清除之后。
- [统计接口仍多] → Mitigation：本变更不纳入；文档说明「单次 list」不含 statistic。

## Migration Plan

- 纯前端行为修复，无数据迁移、无特性开关。
- 回滚：还原缺陷页抑制逻辑即可。

## Open Questions

- 无阻塞问题。若自测发现 `SelectProjectMember` 在异步 `getMemberList` 完成后才 emit，抑制窗口是否需延到该 Promise——实现时按实测补 `$nextTick` 或短 defer，不改产品决策。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/04-UI规范/组件与交互约定.md`（若回写「程序化 hydrate 禁止旁路 fetch」一行）
- `docs/07-技术决策/说明.md`（本次不新增 ADR）
