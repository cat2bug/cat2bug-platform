# Select 下拉嵌 MemberNameplate 须撑高 option

- 日期：2026-11-04
- 涉及：前端

## 现象

`el-select` 选项里放 `MemberNameplate`（头像 + 昵称 + 邮箱/手机）时，选项文字叠在一起，看起来像和背后表格「重叠」。

## 根因

Element Plus 默认 `el-select-dropdown__item` 行高偏矮，装不下铭牌两行；且下拉挂到 `body`，scoped 样式打不上。

## 解决办法

给 `popper-class` 写**非 scoped** 样式：`item` 设 `height: auto; min-height: 60px;`，并整理铭牌内边距与次行字号。团队转让、项目转让下拉已按此处理。

## 以后怎么避免

凡 `el-option` 内嵌 `MemberNameplate`（或其它多行内容），一并加对应 `popper-class` 撑高样式，不要只依赖组件默认行高。
