# 暗色主题下 custom-class 未生效导致统计卡仍是白底

- 日期：2026-10-18
- 涉及：前端

## 现象

暗色主题下，测试计划执行抽屉顶部的指标卡（倒计时、用例通过率、缺陷发现率等）整块白底，文字仍是浅色主题的深灰，和周围深色界面脱节。

## 根因

两处叠在一起：

1. 卡片样式写死 `background-color: #fff`、`color: #303133`、`border: #ebeef5`，没有用项目 token。
2. 暗色覆盖写在 `html.dark .handle-plan-dialog` 下，但抽屉用的是 Element UI 的 `custom-class`。Element Plus 的 Drawer / Dialog 没有这个 prop，它只会变成普通 HTML 属性，不会进入 `class`。选择器匹配不到，覆盖不生效。

## 解决办法

- 自绘卡片直接用 token：底 `--card-bg`，字 `--text-color-primary`，边 `--border-color-light`。浅色值与原来的 `#fff` / `#303133` / `#ebeef5` 相同，暗色随 `html.dark` 切换。
- 需要挂到抽屉根节点的类名用 `class`，不要用 `custom-class`。

## 以后怎么避免

- 新样式禁止写死单一主题的白底和深色字，见 [[主题与配色]]。
- Element Plus 弹层加类名用 `class`。`custom-class` 只在旧 Element UI 里是 class。

相关：[[主题与配色]]、[[暗色主题下臆造bg-color-overlay回退成大块白色]]
