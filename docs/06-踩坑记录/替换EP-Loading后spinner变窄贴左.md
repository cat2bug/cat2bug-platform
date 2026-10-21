# 替换 EP Loading 后 spinner 变窄贴左

- 日期：2026-10-20
- 涉及：后台

## 现象

用自定义内容（如 `Cat2BugLoading`）替换 Element Plus `.el-loading-spinner` 内部后，加载指示出现在遮罩左下/偏左，而不是上下左右居中。

## 根因

EP 默认 spinner 依赖 `width: 100%` + `text-align: center` 做水平居中。自定义内容变窄后若只写 `top: 50%; transform: translateY(-50%)` 且 `width: auto`，绝对定位元素会落在静态位置（通常贴左）。

## 解决办法

对增强后的遮罩显式居中：

```scss
.el-loading-mask[data-cat2bug-loading='1'] .el-loading-spinner {
  position: absolute;
  left: 50%;
  top: 50%;
  margin: 0;
  width: max-content;
  transform: translate(-50%, -50%);
}
```

## 以后怎么避免

替换 EP Loading 视觉时，不要沿用「仅靠 width:100% 居中」的假设；自定义块级/窄内容一律用 `left/top 50% + translate(-50%,-50%)`。
