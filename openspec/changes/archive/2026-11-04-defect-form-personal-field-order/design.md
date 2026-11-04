## Context

新建/编辑表单通过 `resolveOrderedDefectFormFields` 排字段，现状只认团队 `listColumnLayout.orderedEnabledFieldKeys`；`localCache` 参数已废弃。列表侧已有用户隔离 field-list 缓存与 `mergeDefectTableColumnPrefs`（个人 key 序权威）。产品要求录入页顺序与「显示字段」个人序对齐。

## Goals / Non-Goals

**Goals:**

- 有个人显示字段缓存时：表单字段顺序跟该缓存 key 序（经 table key → 表单 field 映射）。
- 无个人缓存：回退团队 `orderedEnabledFieldKeys`（现有行为）。
- 显隐仍只看团队启用；列表 `visible: false` 不隐藏表单字段。
- 打开新建页 / 编辑抽屉时重新解析顺序，避免刚改完显示字段仍用旧序。
- 更新 `docs/04` 与相关踩坑口径。

**Non-Goals:**

- 不改详情页属性序。
- 不改团队缺陷属性管理页 / sortOrder API。
- 不把个人序同步到服务端。
- 不新增 ADR。

## Decisions

1. **复用 merge，不另写一套个人序**  
   - `resolveOrderedDefectFormFields`：若 `hasDefectTableColumnCache(localCache)`，用 `buildDefectTableColumnDefaults` + `mergeDefectTableColumnPrefs` 得到列序，再映射为表单字段；否则走现有 `orderedEnabledFieldKeys` 路径。  
   - 映射复用已有 `tryPushBuiltinFromTableKey` / `tryPushCustomFromTableKey`（`plan-start/end` → `planTime`，跳过编号/状态等）。  
   - 备选否决：只读缓存 raw key、跳过 merge —— 会丢「新团队字段按前驱插入」一致性。

2. **显隐与序解耦**  
   - push 时仍用 `isBuiltinVisible`（团队），忽略 merge 结果里的 `visible`。  
   - 理由：列表可隐藏「描述」等，录入仍需按团队启用展示。

3. **重算时机**  
   - `DefectFormOrderedFields.rebuildOrderedFormFields` 继续吃 `$cache.local`；在 Add/Edit 打开成功路径显式再调一次（或暴露 `refreshFieldOrder`），保证读到最新个人序。  
   - 备选否决：监听 storage 事件（过重）。

4. **旧 string[] 缓存**  
   - merge 对 string[] 仍以团队 defaults 为列序 → 表单等同「无个人序」，符合预期。

## Risks / Trade-offs

- [Risk] 踩坑记录曾要求表单不读个人缓存防串账号 → Mitigation：缓存键已 `u{userId}` 隔离；文档改为「优先个人序，键须用户隔离」。  
- [Risk] 个人序与团队必填字段落点不一致 → Mitigation：`appendMissing*` 仍补齐团队启用字段。  
- [Trade-off] 详情仍团队序、表单跟人 → 短期可接受；产品要对齐再开变更。

## Migration Plan

- 纯前端；无 DB。有 field-list 对象缓存的用户打开表单即见个人序；无缓存仍团队序。  
- 回滚：恢复「忽略 localCache」分支即可。

## Open Questions

- 无。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`
- `docs/06-踩坑记录/缺陷属性团队配置后其他账号仍像默认.md`
- `docs/07-技术决策/ADR-023-用例类型评审模板与缺陷属性归属团队.md`
- `docs/08-版本记录/2026-11.md`（或当月版本记录）
- 拟新增 ADR：无
