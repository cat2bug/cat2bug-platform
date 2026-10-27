## Context

工作台 v3 行布局（`tabs.work.rows` / 自定义页签 rows）中，列结构在 `row-layout.js` 已规范化为 `widgets[]`（兼容旧字段 `widget`），展示层 `WorkbenchRows` 对多组件已做上下堆叠；manager 预设右侧使用 `stack: [...]`。编辑器 `WorkbenchRowEditor` 的 `placeWidgetInCol` 仍整列替换为单元素数组，拖拽把手绑定整列并 `swapColWidgets`，空槽才显示选择器——导致「只能加一个组件」。

约束：不改持久化 API 与 version（仍为 3）；不推翻 ADR-021；变更范围限前端布局编辑与规格对齐。

## Goals / Non-Goals

**Goals:**

- 编辑态一列可追加多个组件，上下自动排列，数量不设上限。
- 组件级拖拽：列内改序、跨列移动；目录/选择器放入默认追加。
- 同列 `type` 唯一：选择器排除已有；拖入重复则提示且不落。
- 明确行高：列共用行高；`content` 取各列堆叠高度（组件之和 + 间距）的最大值。

**Non-Goals:**

- 不改后端表结构或 workbench 保存接口。
- 不做列内嵌套子栅格、不做组件跨行拖。
- 不强制迁移历史配置（已是 `widgets[]` 或单 `widget` 均可读）。
- 不引入新 DnD 依赖库（优先 HTML5 drag，与现编辑器一致）。

## Decisions

1. **数据真相：`col.widgets` 有序数组**  
   继续用现有 `normalizeCol` / `setColWidgetList`；`widget` 仅同步为首项以兼容。放置改为 append；移除单实例后更新列表。  
   *备选：新建 `stack` 嵌套结构 → 否决，与现预设/归一化重复。*

2. **拖拽载荷改为 instance 级**  
   `dragstart` 携带 `instanceId` + 源 `colId`；落点：同列插入索引改序；跨列从源删除再插入目标索引（默认追加末尾或落在两组件之间）。去掉「整列 swap」主路径。  
   *备选：保留整列拖 + 另做列内排序 → 手势冲突，否决。*

3. **同列 type 唯一、跨列允许同 type**  
   选择器按当前列已有 `type` 过滤；catalog 拖入 / 跨列移入若目标已有同 `type` → `$message.warning` 并 abort。页面级仍允许多实例（不同列），与既有 `instanceId` 语义一致。

4. **行高 `content` = max(列堆叠高)**  
   列 flex 拉伸共用行高；每列自然高 = Σ(组件内容高) + (n-1)×间距；行高取各列自然高最大值。`row`/`fixed` 行为保持规格既有描述，多组件时溢出在列内滚动或裁切（与现列容器一致）。

5. **空列与添加入口**  
   非空列保留「添加」控件（过滤后的选择器）及可继续接受 catalog drop；单组件有独立删除，删光后列为空槽。

## Risks / Trade-offs

- [列内组件过多导致页面很长] → 不设上限是产品选择；`fixed`/`row` 溢出靠列内滚动缓解。  
- [HTML5 drag 插入位置手感差] → 落点高亮插入线；MVP 可先「跨列一律 append 末尾 + 列内仅上下互换相邻」再迭代精确插缝。  
- [用户习惯整列对调] → 可用两次跨列移动近似；不在本变更恢复整列 swap。

## Migration Plan

- 部署前端即可；读路径已兼容单 `widget` 与 `widgets[]`。  
- 回滚：回退前端包；已保存的多组件布局在旧编辑器中仍可展示（若旧包展示支持），旧编辑器再编辑可能压回单组件——属预期。

## Open Questions

- 无（content 取 max、同列禁 type 已确认）。

## 相关长期文档

- `docs/07-技术决策/ADR-021-工作台固定页与自定义私有共享页签.md`
- `docs/01-产品需求/产品定位与核心功能.md`（项目工作台条目）
- 拟新增 ADR：无
