# Loading 雪碧用 background-position 主线程卡死

- 日期：2026-10-27
- 涉及：前端 / 缺陷列表 / 加载态

## 现象

外网打开缺陷列表（数据量大）时，品牌奔跑猫 loading 停在某一帧「卡住不跑」；本地小数据往往不明显。页面其它操作也会卡顿。

## 根因

`Cat2BugLoading` 曾用 CSS `background-position` + `steps()` 抽雪碧帧。`background-position` 动画要主线程参与 paint，不能稳定跑在合成线程。缺陷页 `listDefect` 返回后 Vue 同步渲染大表 / Excel 视图，主线程连续占满，帧动画一起停。

这和「接口没异步」不是一回事：请求可以是异步的，但**响应落地后的同步渲染**仍会堵主线程。

## 解决办法

- 条带宽度 = 帧数 × 单帧宽，用 `transform: translate3d` + `steps(帧数)` 抽帧，并 `will-change: transform` / 父级 `translateZ(0)` 促合成层。
- 脚影等附属动效同样只改 `transform` / `opacity`。

## 以后怎么避免

- 加载指示、骨架屏动效优先 `transform` / `opacity`；不要用 `background-position`、`top/left`、`width/height` 做持续动画。
- 大列表仍应分页 / 虚拟滚动，减轻主线程，但 loading 本身不能依赖「主线程空闲才跑」。

相关：[[组件与交互约定]]
