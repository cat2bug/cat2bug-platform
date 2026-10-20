# 操作列自动宽 live rect 反馈放大卡在 450

- 日期：2026-10-20
- 涉及：前端（`Cat2BugTable`）

## 现象

用例等短操作列（仅「修改」「删除」）右侧大片空白，列宽常驻约 **450px**；缺陷多按钮页不显眼。

## 根因

1. CSS `--cat2bug-operate-col-applied-width` **默认占位 450px**，首帧已顶满上限。
2. `syncOperateColumnWidths` 在固有宽之外又取 `tools.scrollWidth` / `getBoundingClientRect().width` 取 max；容器已被 450 撑开时，live 宽 ≈ 单元格宽，回灌后结果永远贴 `operateColumnMaxWidth`。

表头测宽注释早已禁止用被撑开的 `.cell` rect，行内 live 宽遗漏了同一原则。

## 解决办法

变更 `fix-operate-column-auto-width`：

- 测宽只保留 `measureOperateRowInnerWidth` + 表头固有宽；删除 live 宽回灌。
- 初始 applied 占位改为 **88px**（与 `operateColumnMinWidth` 一致）。
- 行内测宽为 0 时回退 `max(表头, minWidth)`，不得抬到 max。

## 以后怎么避免

- 「根据测量结果改尺寸」时，禁止把**已被当前尺寸撑开**的 DOM 宽再当内容宽。
- 占位宽度取下限或保守短值，不要用上限当默认 applied。
