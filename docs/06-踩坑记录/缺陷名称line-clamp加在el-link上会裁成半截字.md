# 缺陷名称 line-clamp 加在 el-link 上会裁成半截字

- 日期：2026-11-04
- 涉及：platform-ui（缺陷列表）

## 现象

缺陷列表「缺陷名称」很长或带换行时，最后一行文字被拦腰裁掉（只露半截字），没有出现 `...`。

## 根因

1. `Cat2BugTable` 给单元格设了 `max-height`（约 89px）+ `overflow: hidden`。
2. 名称列把 `-webkit-line-clamp: 3` 写在 `.el-link` 上，但 Element Plus 的 `el-link` 默认是 `inline-flex`，省略号经常不生效。
3. 省略失败后，多出来的字被单元格硬裁切，看起来像「显示一半」。

## 解决办法

把 `line-clamp` 加到 `.el-link__inner`（真正包文字的节点）上；外层 `el-link` 改成 `display: block` + `overflow: hidden`。名称列单元格不要再用通用的 4 行 clamp，留给内层标题做 3 行省略，并给统计行 `flex-shrink: 0`。

见 `cat2bug-platform-ui/src/views/system/defect/list/table.vue`、`src/assets/styles/cat2bug.scss`、`src/components/Cat2BugTable/index.vue`。

## 以后怎么避免

列表里用 `el-link` 做多行省略时，**不要**把 `-webkit-line-clamp` 直接加在 `el-link` 根节点；夹 `.el-link__inner`，或自己包一层纯文本节点再 clamp。
