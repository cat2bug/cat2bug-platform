## Context

`Cat2BugTable` 对 append 操作列（`cat2bug-operate-column` + `.cat2bug-operate-tools`）做自动宽：`operateColumnAutoWidth` 默认开，范围 `operateColumnMinWidth`(88)～`operateColumnMaxWidth`(450)。CSS 变量 `--cat2bug-operate-col-applied-width` **默认 450px**，`syncOperateColumnWidths` 测完再写入真实宽。

测宽路径里除 `measureOperateRowInnerWidth`（克隆节点测固有宽）外，还有一段用 `tools.scrollWidth` / `getBoundingClientRect().width` 的 **live 宽**。表头测宽注释已禁止用被撑开的 `.cell` rect，行内 live 宽仍存在，易把「占位 450 后的容器宽」回灌成「内容宽」，结果永远贴上限。用例列表仅「修改 + 删除」时空白最明显。

长期约定见 `docs/04-UI规范/组件与交互约定.md`（操作列 88～450）。与 `unify-list-density` / `unify-operate-action-colors` 正交。

## Goals / Non-Goals

**Goals:**

- 短操作列（如用例：修改+删除）按内容收窄，视觉贴按钮，不再常驻 ~450px 空白。
- 测宽只使用内容固有宽（克隆 / 可见子节点累加），禁止 live 容器宽反馈放大。
- 未测出有效内容前占位 ≤ 下限附近（如 88），测完再设真实宽；上限 450 与超宽换行不变。
- 缺陷多按钮操作列仍可接近 450 并正确 `data-operate-at-cap`。

**Non-Goals:**

- 不改操作列 CRUD 色、行高/密度、表头手柄规则。
- 不给业务页手写固定操作列 `width` 作为主方案（仍走组件自动宽）。
- 不改 `operateColumnMaxWidth` / `MinWidth` 的公开默认数值（88 / 450）。
- 不做跨浏览器像素级像素审计以外的大重构。

## Decisions

1. **去掉（或永不参与决策的）live 宽回灌**  
   - 在 `syncOperateColumnWidths` 中删除对 `tools.scrollWidth` / `getBoundingClientRect().width` 取 max 并入 `maxSingleRowInner` 的逻辑；只保留 `measureOperateRowInnerWidth`（及表头 `measureOperateHeaderContentWidth`）。  
   - 备选：保留 live 宽但仅当 `liveW <= natural + 容差` 时采纳 → 更复杂，收益小，否决。

2. **默认占位从 450 改为下限**  
   - 组件根与全局 CSS 中 `--cat2bug-operate-col-applied-width` 初始改为 `operateColumnMinWidth`（88px）或与之同量级；`applyOperateColumnDomWidth` 写入后仍可为 88～450 任意值。  
   - 备选：保持 450 占位只修测宽 → 首帧仍闪一下宽列，否决。

3. **固有宽测量失败时的回退**  
   - 行内测宽为 0 且表头有「操作」文案：至少取 `max(headerContent, minWidth)`。  
   - 仍为 0：保持 minWidth，不抬到 max。

4. **文档**  
   - 组件约定补一句：「操作列测宽必须用内容固有宽，禁止用已被列宽撑开的 DOM 宽回灌。」  
   - 拟新增踩坑（非 ADR）：「操作列自动宽 live rect 反馈放大卡在 450」。  
   - 拟新增 ADR：无（实现细节，不上升为架构决策）。

## Risks / Trade-offs

- [首帧略窄再撑开] → 占位 88 比 450 闪动更可接受；布局 pass 已在 rAF 内二次 apply。  
- [克隆测宽漏掉复杂 DefectTools 子节点] → 继续用现有 `measureOperateFlexRowNaturalWidth` / focus-list 分支；验收对缺陷列表做回归。  
- [Safari 弹性列抖动类问题复发] → 不重新对根做无容差 ResizeObserver；操作列宽变化仍走既有 layout pass。见 `docs/06-踩坑记录/Safari下表格弹性列循环重排行跳动.md`。

## Migration Plan

- 纯前端；无需数据迁移。  
- 回滚：还原 `Cat2BugTable` 测宽与 CSS 默认占位即可。

## Open Questions

- 无（占位取 88 已定；若实现时首帧裁切严重可改为 ~120，仍远小于 450）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（操作列自动宽 88～450、测宽原则拟补）
- `docs/06-踩坑记录/Safari下表格弹性列循环重排行跳动.md`（表格测宽/布局循环，勿复发）
- `docs/08-版本记录/2026-10.md`（归档前加一行）
- 拟新增踩坑：操作列自动宽 live rect 反馈放大卡在 450
- 拟新增 ADR：无
