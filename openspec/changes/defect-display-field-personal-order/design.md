## Context

缺陷列表表格「显示字段」popover（`table.vue`）与 Excel 同名面板目前只能勾选显隐。列默认序来自团队 `listColumnLayout` → `orderedEnabledFieldKeys`（`sortOrder`）。`Cat2BugTable` 表头已可 Sortable 拖列并 `saveShowColumns`，但 `mergeDefectTableColumnPrefs` **故意忽略**缓存数组顺序，刷新后回到团队序。文档 `docs/04` 写明「不用个人拖列覆盖字段管理顺序」。本变更按产品决定改为：**个人列序权威，团队序仅默认**；面板可拖且与表格/Excel 同步；团队属性管理页不动。

## Goals / Non-Goals

**Goals:**

- 显示字段列表（全部可配置字段，含未勾选）可拖排序，松手同步表格列序并持久化到用户隔离 localStorage。
- 表头拖序与面板拖序同一缓存；刷新后个人序仍在。
- Excel 共用同一用户键与同一合并结果；Excel 面板亦可拖。
- 团队新增字段：按团队默认序插入到「个人序中仍存在的、团队序上紧邻前驱」之后；若无前驱则插到个人序开头相对位置或末尾规则见 Decisions。
- 团队删除/停用字段：从个人序剔除。
- 更新 `docs/04` 口径。

**Non-Goals:**

- 不改团队「缺陷属性」管理页的拖序与 `sortOrder` API。
- 不改新建/编辑表单、详情风箱字段序（仍只读团队 layout）。
- 不把个人列序同步到服务端 / user-config。
- 不改甘特/看板布局。
- 不新增 ADR（列表个人偏好，非架构分叉）。

## Decisions

1. **合并算法（改 `mergeDefectTableColumnPrefs`）**  
   - 输入：`defaults`（团队序列定义）、缓存条目 `{ key, visible, fixed, width? }[]`（或旧 string[]）。  
   - 输出列序：先按缓存中仍存在于 defaults 的 key 顺序排列；defaults 中有、缓存没有的 **新 key**：在团队 `orderedEnabledFieldKeys` 中找该 key 的前一个仍出现在「已输出个人序」中的字段，插到其后；若无这样的前驱，插到结果末尾（保持稳定、可预期）。  
   - `visible` / `fixed` / `width`：仍按 key 从缓存合并到 defaults 行。  
   - 备选否决：继续以 defaults 为序（现状，不满足需求）；整表完全忽略团队新字段直到用户重置（易丢新列）。

2. **显示字段面板拖拽**  
   - 对 `.defect-column-picker` 内每一项加拖柄，`Sortablejs`（与 `Cat2BugTable` / 页签一致），`filter`/handle 避免与 checkbox 点击冲突。  
   - `onEnd`：按 DOM 重排 live columns → `setColumns`（或等价全量 setColumns）→ 既有 `saveShowColumns` / `columns-change` 链路刷新勾选列表。  
   - 备选否决：vuedraggable（Excel PanelSetting 遗留）；仅会话不落盘。

3. **表格 ↔ Excel 共用缓存键**  
   - 一律 `getDefectTableColumnCacheKey()`（含 `defect-table:u{userId}`）。Excel 去掉裸 `defect-tabledefect-table-field-list` 写入；读时若仅有旧键则迁移到用户键（与 table 既有迁移一致）。  
   - Excel 数据列序由 `excelDataColumnKeysFromMerged` / `reorderColsByKeyOrder` 吃合并结果；面板拖完写回同一 field-list。

4. **`preserve-column-order`**  
   - 父组件传入的 merged 列已含个人序；保持 `preserve-column-order`，避免 Cat2BugTable 内部再按另一套缓存重排冲突。拖列后仍 `setColumns` + save，下次 `resolveDefectMergedColumns` 读回个人序。

5. **表单/详情**  
   - 明确继续 `loadDefectColumnLayout` / 团队序，**不读**个人 field-list 顺序，避免列表偏好污染录入体验。

## Risks / Trade-offs

- [Risk] 旧缓存只有显隐、顺序碰巧等于写入时 DOM 序 → 合并后突然「记住」一次无意顺序 → Mitigation：行为符合「缓存数组即序」；无缓存时仍用团队默认。  
- [Risk] Excel 旧无用户前缀键与 table 键分裂 → Mitigation：读两边、写只写用户键并迁移。  
- [Risk] 拖柄与 checkbox/键盘导航冲突 → Mitigation：专用 handle + 既有 `useColumnPickerPopoverKbd` 回归。  
- [Trade-off] 个人序与团队默认长期分叉，新人仍看团队序；管理员改团队序不会覆盖已有个人序（仅影响新字段落点）——符合产品决定。

## Migration Plan

- 纯前端；无 DB 迁移。  
- 上线后：有 field-list 对象数组缓存的用户按数组序恢复；仅有旧 string[] 的用户以团队序为列序、string[] 为可见集（与今类似，但对象化后首次拖序即写入完整序）。  
- 回滚：恢复 merge「以 defaults 为序」并去掉面板 Sortable；用户缓存多出来的顺序字段可忽略。

## Open Questions

- 无（新字段落点规则已在 Decisions §1 写死）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（缺陷列表 / 显示字段 / 列序口径，本变更须改写）
- `docs/07-技术决策/ADR-023-用例类型评审模板与缺陷属性归属团队.md`（团队属性权威；本变更不推翻，仅列表个人展示序）
- `docs/06-踩坑记录/缺陷属性团队配置后其他账号仍像默认.md`（缓存键/用户隔离相关，实现时对照）
- `docs/08-版本记录/2026-10.md`（归档前加一行）
- 拟新增 ADR：无
