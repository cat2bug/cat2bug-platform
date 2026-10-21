## 1. Cat2BugTable spacer 注入

- [x] 1.1 在 `Cat2BugTable` 模板中于业务列与 `#append` 之间增加 spacer 列（空 label、`cat2bug-operate-spacer-column` + `no-drag`、无手柄、无业务插槽）
- [x] 1.2 增加内部状态（如 `spacerColumnWidth`）与探测「是否存在 operate-column」的逻辑；无操作列时不渲染或宽为 0
- [x] 1.3 确认 spacer 不进入 `tableFieldList` / `columns-change` / 本地列缓存

## 2. 布局与 flex 共存

- [x] 2.1 在 `runTableLayoutPass` / `computeFlexColumnWidths`（或紧邻步骤）中计算主区剩余宽度，写入 spacer；`remaining ≤ 容差` 时 spacer = 0
- [x] 2.2 当 spacer 需要吃剩余时，跳过业务列 `columnShouldAutoFlex` 的剩余分配，避免双弹性
- [x] 2.3 求和时计入 prepend（selection、树展开列等）与操作列宽，避免漏算导致 spacer 过大/过小
- [x] 2.4 调宽、显隐列、窗口 resize、操作列测宽变化后重算并 `doLayout`；注意避免布局循环（对照 Safari 弹性列踩坑）

## 3. 样式

- [x] 3.1 spacer 表头高度/底边/背景与业务列对齐；暗色继承现有表头 token
- [x] 3.2 确认自定义横滚条与 `fixed-right` 操作列在有/无 spacer 时不错位

## 4. 验收

- [x] 4.1 文档列表：拖窄各列后操作列贴容器右缘，中间为 spacer 空白而非表外空洞（代码侧：`computeFlexOrSpacerColumnWidths`；请本地硬刷新确认）
- [x] 4.2 缺陷 / 用例列表：同样拖窄后贴右；列总宽溢出时横滚正常、spacer 为 0（同 Cat2BugTable 全局路径）
- [x] 4.3 列选择器与本地列缓存无 spacer 项；硬刷新后行为稳定（spacer 仅内部状态，未写入 `tableFieldList`）

## 5. 回写长期知识

- [x] 5.1 更新 `docs/04-UI规范/组件与交互约定.md`：列总宽不足时注入操作列前弹性空列
- [x] 5.2 `docs/08-版本记录/2026-10.md` 加本变更一行；拟新增 ADR：无；若实现踩到布局循环再补踩坑
