# 新版Chrome中section里的h1不再自动缩小

- 日期：2026-10-14
- 涉及：前端

## 现象

仪表盘各卡片标题（「测试用例」「缺陷状态变化30天走势图」「活动」「成员处理缺陷排行」等）突然变成 32px：统计卡片标题被截断，走势图标题压到右上角的「30天 / 12个月」按钮上。前端代码并没有改过。

## 根因

这些组件的 `<h1>` 没写 `font-size`，以前靠浏览器内置规则：`article / aside / nav / section` 里的 `h1` 缩小为 1.5em（24px）、上下外边距 0.83em。页面主体外层正好是 `<section class="app-main">`。Chrome 136 起删除了这条内置规则，`h1` 回到默认的 2em（32px）。

## 解决办法

在 `cat2bug-platform-ui/src/assets/styles/index.scss` 里补回这条规则，并用 `:where()` 把优先级降到 0，组件自己写的字号依然优先：

```scss
:where(article, aside, nav, section) :where(h1) {
  margin-block: 0.83em;
  font-size: 1.5em;
}
```

## 以后怎么避免

- 新组件的标题要显式写字号，不要依赖浏览器内置样式。
- 看到「代码没动、样式突然变了」时，先查浏览器版本是否改了 UA 默认样式。
