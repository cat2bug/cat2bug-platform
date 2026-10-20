# EP small 表压过表格轨 token

- 日期：2026-10-20
- 涉及：前端

## 现象

用例类型等仍裸用 `el-table` 的业务列表，单行明显矮于缺陷 / 用例（`Cat2BugTable`）列表，虽已设 `--cat2bug-table-cell-padding-y: 16px`。

## 根因

全站 `el-config-provider` 默认 `size=small`，表格带 `.el-table--small`。Element Plus 的 `.el-table--small .el-table__cell` 与全局 `.el-table .el-table__cell` **选择器权重相当**，级联顺序下 EP 紧凑 padding 常赢。`Cat2BugTable` 用更深的 `:deep(tbody > .el-table__row > …)` 才不被压掉。

## 解决办法

变更 `fix-table-rail-vs-ep-small`：

- 全局规则显式写上 `.el-table--small`（必要时 `!important`），保证表格轨 token 生效。
- 键盘说明表、对话框枚举编辑表等加 `cat2bug-table-compact` 保持紧凑。
- 用例类型迁 `Cat2BugTable`。

## 以后怎么避免

- 改表格轨密度时，验收必须包含 **裸 `el-table` + 全局 small** 一页，不能只看 `Cat2BugTable`。
- 新增业务列表优先 `Cat2BugTable`；临时裸表也依赖上述全局覆盖。
