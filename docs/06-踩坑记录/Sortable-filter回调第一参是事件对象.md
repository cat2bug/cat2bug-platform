# Sortable filter 回调第一参是事件对象

- 日期：2026-10-26
- 涉及：后台（前端工作台 / 缺陷页签拖序）

## 现象

工作台自定义页签已接 Sortable.js，同组拖序与 `PUT .../tabs/sort` 都写好了，页面上却完全拖不动。

## 根因

Sortable 的 `filter` 若传函数，签名是 `(evt, target, sortable)`。代码写成单参并把该参数交给「是否可拖」判断（读 `el.id`），实际收到的是**指针事件**，没有页签 DOM 的 `id`，于是一律判不可拖，`filter` 恒为 true，拖动全部被取消。

## 解决办法

按官方签名写：`filter(evt, target) { ... isDraggable(target) }`。需要排除「更多」菜单时，用 `evt.target.closest('.xxx-tab-menu')` 再 return true。

## 连带现象：一拖就选中文字

`forceFallback` 下若 `fallbackTolerance > 0`，拖影启动前的几像素移动不会 `preventDefault`，浏览器会开始选字，看起来像「拖不动」。工作台页签标题还是可选中文本，比缺陷页更明显。

处理：页签 `user-select: none`、nav 上拦 `selectstart`、`fallbackTolerance: 0`；菜单用 `@pointerdown.stop`（Sortable 默认听 pointer，只 stop mousedown 不够）。`preventOnFilter` 保持 `false`，否则固定「工作台」项会被拦住点选。

## 以后怎么避免

- 接 Sortable 时对照文档核对 `filter` / `onMove` / `onEnd` 参数，不要凭直觉当成「被拖元素」。
- 拖不动时先在 `filter` 里临时打日志看第一参是不是 `Event`。
- 页签/标签类横向拖序：默认禁选字 + `fallbackTolerance: 0`。
