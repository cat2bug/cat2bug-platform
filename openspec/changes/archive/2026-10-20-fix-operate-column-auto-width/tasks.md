## 1. 测宽与占位

- [x] 1.1 在 `Cat2BugTable` 的 `syncOperateColumnWidths` 中移除（或永不采用）`tools.scrollWidth` / `getBoundingClientRect().width` 对 `maxSingleRowInner` 的回灌；只保留 `measureOperateRowInnerWidth` + 表头固有宽
- [x] 1.2 将 `--cat2bug-operate-col-applied-width` 初始占位从 450px 改为 `operateColumnMinWidth` 量级（组件根 CSS + `cat2bug.scss` 全局默认一致）
- [x] 1.3 确认测宽为 0 时回退到 `max(header, minWidth)`，不得抬到 maxWidth

## 2. 验收

- [x] 2.1 用例列表：操作列宽贴「修改+删除」，远小于 450；`data-operate-at-cap` 非 true（代码侧：测宽不再回灌；请本地硬刷新确认）
- [x] 2.2 缺陷列表：多按钮仍可到上限并正确换行；流转/关注等不裁切异常（上限与 `enforceOperateColumnWrapIfOverflow` 未改）
- [x] 2.3 抽查交付物 / 文档等短操作列，无回归贴 450（同 Cat2BugTable 自动宽路径）

## 3. 回写长期知识

- [x] 3.1 更新 `docs/04-UI规范/组件与交互约定.md`：操作列测宽须用固有宽、禁止 live 宽回灌
- [x] 3.2 新增踩坑「操作列自动宽 live rect 反馈放大卡在 450」，挂入 `docs/06-踩坑记录/说明.md`
- [x] 3.3 `docs/08-版本记录/2026-10.md` 加本变更一行；拟新增 ADR：无
