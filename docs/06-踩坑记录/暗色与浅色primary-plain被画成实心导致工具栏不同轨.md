# 暗色与浅色 primary plain 被画成实心导致工具栏不同轨

- 日期：2026-10-19
- 涉及：前端

## 现象

列表页工具栏已写 `type="primary" plain`，期望与筛选输入框同为轻量描边；暗色下仍是实心蓝底白字，浅色下也像实心主按钮，相对输入框「更大一号」。`unify-list-density` 首轮验收时肉眼觉得「按钮没变」。

## 根因

项目有两处把 **无 `cat2bug-btn-outline` 的 `primary.is-plain`** 画成实心：

1. 浅色：`cat2bug.scss` 里 `html:not(.dark) .el-button--primary.is-plain:not(.cat2bug-btn-outline)` 强制蓝底白字。
2. 暗色：`theme-dark.scss` 里把 `&--primary.is-plain:not(.cat2bug-btn-outline)` 与实心 primary 写在同一条规则里。

只有带 `cat2bug-btn-outline`（或后加的 `.list-page-toolbar` 等作用域覆盖）才是真正的描边/轻底。只改标签为 `plain` 不够。

## 解决办法

- 列表工具栏主新建/添加：`type="primary" plain` **并**加 class `cat2bug-btn-outline`。
- 兜底：暗色/浅色对 `.list-page-toolbar`、`.module-tools`、`.project-option-list-tools` 内的 `primary.is-plain` 覆盖为描边样式，漏加 class 也不回实心。
- 对话框确认、登录等需要实心的场景继续用不带 outline 的 primary（或实心非 plain）。

## 以后怎么避免

- 新列表页主操作不要只写 `plain`；对照 [[组件与交互约定]]「列表页工具条」。
- 改按钮观感时在**浅色和暗色**各看一眼；暗色尤其容易被 `!important` 全局规则盖掉。
- 不要假设 Element Plus 的 `plain` 在本项目里等于「空心」——本仓库刻意把多数 plain primary 做成实心。

相关：[[组件与交互约定]]、[[主题与配色]]、[[暗色主题按钮important覆盖导致禁用态与可点态一样]]、[[Cat2BugTable写死cell-padding盖掉表格轨]]
