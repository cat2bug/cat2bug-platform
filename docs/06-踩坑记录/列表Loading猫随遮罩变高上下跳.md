# 列表 Loading 猫随遮罩变高上下跳

- 日期：2026-10-27
- 涉及：后台

## 现象

交付物等列表页：奔跑猫先出现在偏上位置，表格区域撑开后又跳到上下居中。

## 根因

1. `Cat2BugTable` 把 `v-loading` 挂在内层 `el-table` 上。空数据时表格只有表头高度，遮罩很矮，猫在矮框里「居中」看起来贴顶。
2. 随后列表区定高 / 数据行出现，遮罩变高；`cat2bug-loading-enhance` 的浮动层按遮罩 `getBoundingClientRect` 重新垂直居中，猫跟着下移。

## 解决办法

- `v-loading` 改挂 `.cat2-bug-table-wrap`，并让 wrap 在列表宿主定高时 `height: 100%`，遮罩从一开始就覆盖整块列表区。
- loading 且已有 `tableMaxHeight` 时给 wrap 写 `minHeight`（计划/用例等 `.xxx-table-x-scroll` 故意不 `flex:1`，否则空表时 wrap 仍随表很矮）。
- enhance：遮罩高度连续稳定两帧后再显示并锁定视口纵坐标；EP leave 时立刻拆浮动层。

## 以后怎么避免

列表类 Loading 挂在「已定高的列表容器」上，不要挂在内容高度随数据变化的 `el-table` 根上；有分页、x-scroll 不能撑满时，loading 期间用 `tableMaxHeight` 撑开遮罩。
