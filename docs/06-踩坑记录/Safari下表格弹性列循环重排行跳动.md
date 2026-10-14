# Safari 下表格弹性列循环重排、行跳动

记录 `Cat2BugTable` 在 Safari 中报告、文档列表不停抖动的原因。

- 日期：2026-07-05
- 涉及：前端

## 现象

报告、文档列表在 Safari 中表格行不停上下跳动、列宽来回变化；Chrome 正常。

## 根因

`cat2bug-platform-ui/src/components/Cat2BugTable/index.vue` 让标题列「填满剩余宽度」：

- 用 `ResizeObserver` 监听表格根元素，宽度一变就重新算弹性列宽 → 设宽又触发 resize，形成循环；
- 剩余宽度用 `layout.bodyWidth` 减去各列 `realWidth`，Safari 中这些值会有 1~2px 的小数/舍入抖动，每次计算结果都不同；
- 行高同步每次都重写，进一步触发重排。

Safari 的布局时序与舍入让循环无法收敛。

## 解决办法

提交 `3e00afad`：

- 去掉对根元素的 `ResizeObserver`；
- 改用 `.el-table__body-wrapper` 的 `clientWidth` 作为容器宽度，固定列宽按绑定宽度/最小宽度累加，而不是读运行时 `realWidth`；
- 新增容差 `FLEX_COLUMN_WIDTH_TOLERANCE_PX = 2`，变化不超过 2px 不再更新；
- 记录上次同步的行高、加布局重入标记，避免重复同步。

## 以后怎么避免

- 「根据测量结果修改尺寸」的逻辑必须有容差或收敛条件，不能尺寸变了就无条件重算。
- 表格/布局类改动需在 Safari 上验证一次。
