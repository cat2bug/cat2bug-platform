# Loading 增强勿在 EP spinner 内 createApp

- 日期：2026-10-26
- 涉及：后台（`cat2bug-platform-ui`）

## 现象

缺陷详情点「分享」后出现奔跑猫「加载中」，弹层内容透出但无法关闭；控制台大量 `Cannot read properties of null (reading 'emitsOptions')` / `vnode` 为 null，页面假死。

## 根因

`cat2bug-loading-enhance.js` 对 Element Plus `v-loading` 遮罩执行 `spinner.innerHTML = ''` 后，在 spinner **内部**再 `createApp().mount()`。Loading 指令自身用 Vue 管理该 DOM，子树被拆掉后开关 loading（分享弹层 `apiLoading`）会在 patch/unmount 时撞上空 vnode。

另：`ShareCard` 请求失败时曾把 `apiLoading` 设回 `true`，失败场景下遮罩永不消失。

## 解决办法

1. 只给原 spinner 加隐藏 class，**不改其子树**；Cat2BugLoading 挂到 `document.body` 的 fixed 浮动层，按 mask 的 `getBoundingClientRect` 对齐。
2. `ShareCard.refresh` 用 `finally` 将 `apiLoading = false`，并在缺少 `defectId` 时直接返回。

## 以后怎么避免

不要在 Element Plus（或其它库）已托管的 DOM 节点上 `innerHTML` / 嵌套 `createApp`。品牌化 loading 用体外挂载或纯 CSS 替换视觉即可。

## 连带：统计卡内容已出猫还停 1～2 秒

body 浮动猫不在遮罩子树里，EP `close()` 后遮罩还要 leave 淡出约 400ms 才卸 DOM；这段时间内容已透出、猫却仍不透明。enhance 须在 mask 出现 `leave` class / `display:none` 时立刻 teardown 浮动层（见 `isMaskHiding`）。
