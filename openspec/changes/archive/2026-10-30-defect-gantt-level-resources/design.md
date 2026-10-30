## Context

`defect-gantt-auto-level` 已落地图层勾选拖动推挤（`pushOverlapsAfterPinned`）。`levelDefectPlans`、候选过滤与管理员判定仍在 `defect-gantt.js`。产品要补齐 OmniPlan 式一键平整，并与拖动避让分工命名。

约束：自研壳；写走 `updateDefect`；权限角色键与现网一致；不推翻 ADR。

## Goals / Non-Goals

**Goals:**

- 工具条「自动排列」：预览 → 确认/取消；算法用 `levelDefectPlans`（ASAP、消同学道空档、FS、visualOrder）。
- 候选 = 未关闭 ∧ 已排期 ∧ 可改；管理员全量，成员仅自己处理；空气不挡。
- 预览拉项目可编辑未关闭已排期全集 + FS 边，不依赖甘特分页。
- 图层勾选文案改为「拖动避让」；行为保持现有拖推。

**Non-Goals:**

- 改人/交付物/工期；日历；空气硬挡；批量平移推挤；服务端求解器；自动开勾选。

## Decisions

1. **复用 `levelDefectPlans`，恢复预览 UX**  
   按钮 → 分页拉候选全集 + meta links → 过滤 → `levelDefectPlans`（传入列表 `visualOrder`）→ 预览态（幽灵/提议条 + 摘要）→ 确认有限并发 `updateDefect`。取消清内存。  
   **备选**：无预览直接写——否决，动条过多。

2. **入口分离**  
   按钮在甘特工具条（与图层旁）；勾选留在图层菜单并改名。互不强制。预览中禁用普通拖改或提示先结束预览。

3. **权限**  
   同拖动避让：`project.create-by` / `project.admin` 或 `handleBy` 含当前用户；入口要 `system:defect:edit`。

4. **i18n**  
   「自动排列」留给按钮；勾选用「拖动避让」+ tip 说明只后推。

## Risks / Trade-offs

- [与空气重叠负荷仍粉] → 文案诚实。  
- [部分写回失败] → 汇总 ok/fail。  
- [大量 update] → 并发上限约 6。  
- [命名迁移] → 旧 `layers.autoLevel` 键不变，仅文案变。

## Migration Plan

- 无库表迁移。回滚：藏按钮、勾选文案可回退。  
- 合入后回写 `docs/04`、`docs/08`。

## Open Questions

- 无（命名与双入口已在 explore 拍板）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`
- `docs/08-版本记录/2026-10.md`
- 无新 ADR
