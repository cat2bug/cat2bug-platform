# Cat2BugTable 写死 cell padding 盖掉表格轨

- 日期：2026-10-19
- 涉及：前端

## 现象

`unify-list-density` 在 `cat2bug.scss` 把 `--cat2bug-table-cell-padding-y` 提到 16px 后，OpenAI 账号等已迁到 `Cat2BugTable` 的列表行高仍像没变（甚至比裸表更扁）。

## 根因

`Cat2BugTable` 的 scoped 样式里有：

```scss
:deep(tbody > .el-table__row > .el-table__cell) {
  padding: 5px 0;
}
```

选择器带组件作用域，比全局 `.el-table .el-table__cell` 更具体，把表格轨 token 盖掉。迁到 Cat2BugTable 后反而锁死在 5px。

## 解决办法

该规则改为使用 `--cat2bug-table-cell-padding-y` / `--cat2bug-table-row-min-height`，不要写死像素。

## 以后怎么避免

- 改全站表格密度时，同时搜 `Cat2BugTable` 内 `padding` / `min-height` 是否写死。
- 组件内覆盖 EP 默认值时优先引用 token，并在缺陷 / 账号等 Cat2BugTable 页验收，不要只看裸 `el-table`。

相关：[[组件与交互约定]]
