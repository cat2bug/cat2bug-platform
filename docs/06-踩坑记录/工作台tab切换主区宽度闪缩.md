# 工作台 tab 切换主区宽度闪缩

- 日期：2026-10-27
- 涉及：后台

## 现象

工作台页签切换时，整页先变窄再变宽，闪一下。

## 根因

主滚动在 `.main-container`（`overflow-y: auto`）。切换 tab 时 `WorkbenchRows` 整页重排，内容高度短时内越过/退回视口，纵向滚动条显隐会改变可用宽度；宽度一变又触发栅格列回流，看起来像「缩一下再宽一下」。全局不敢开 `scrollbar-gutter`（无滚动条时右侧仍占槽，与左侧 20px 不对称）。

## 解决办法

仅对工作台：`.main-container:has(.workbench-page) { overflow-y: scroll; }`（常显纵向滚动条占位）。**不要用 `scrollbar-gutter: stable`**：部分 Chromium 会把 gutter 轨道和真实滚动条画成并排双条。工作台根与行容器加 `max-width: 100%` / `overflow-x: hidden`，避免切换瞬间横向溢出。文档层 `html/body/#app` 保持 `overflow: hidden`，只让 `.main-container` 滚。

## 以后怎么避免

主区靠纵向滚动、且内容高度会随状态大幅变化的页面，用常显 `overflow-y: scroll` 或列表页那套内部滚动稳定宽度；慎用 `scrollbar-gutter`（易双条）。`overflow-x`/`overflow-y` 不要一轴 `hidden`、一轴 `visible`（visible 会被算成 auto）。
