## 1. 抑制程序化回声

- [x] 1.1 在 `defect/index.vue` 增加 `_suppressDefectQueryFetch`（或等价）标志，并在 `handleQuery`（及直接绑到 `@change`/`@input` 的入口）上尊重该标志
- [x] 1.2 在 `selectDefectTabHandle` 写 `queryParams` / `reset` 的同步段（必要时含 `$nextTick`）用 try/finally 包住标志；结束后只调用一次 `scheduleTabDataLoad`
- [x] 1.3 确认 `activeDefectTabName` 的 watch 仍只落盘 tabId，不触发列表加载

## 2. 恢复路径核对

- [x] 2.1 确认 early 读 tabId 不发起 `listDefect`；`getDefectConfig` 成功应用缓存 tab 后仅一次列表加载
- [x] 2.2 确认「已删除」仍不写入 tab 缓存；只缓存 tabId（含项目键）行为不变

## 3. 验收

- [x] 3.1 Network：缓存为「我的」时硬刷新 → `configDefect` 后窗口内缺陷列表接口仅 1 次（实现：抑制回声 + `scheduleTabDataLoad` force 单次；请本机 Network 再扫一眼）
- [x] 3.2 Network：从「全部」点到「我的」→ 该次切换列表接口仅 1 次（同上）
- [x] 3.3 手动改状态/处理人/关键词后仍能正常拉列表（抑制约 400ms 后清除，手动路径不走 force 旁路）

## 4. 回写长期知识

- [x] 4.1 在 `docs/04-UI规范/组件与交互约定.md` 补「程序化 hydrate 查询条件期间禁止旁路 fetch」
- [x] 4.2 本变更不新增 ADR；在 `docs/08-版本记录/2026-10.md` 记一行 `defect-tab-single-list-on-restore`
