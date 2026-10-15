# 暗色主题下臆造 --bg-color-overlay 回退成大块白色

- 日期：2026-10-15
- 涉及：前端

## 现象

暗色主题下，MCP 接入弹窗「客户端接入示例」右侧详情区是整块刺眼的白色，和弹窗深色底严重冲突。左侧列表、`<pre>` 代码块颜色正常。

## 根因

自绘容器写了：

```css
background: var(--bg-color-overlay, var(--el-bg-color));
```

项目 token 里**没有** `--bg-color-overlay`（只有 `--bg-color-base|page|block`，以及容器类 `--card-bg` / `--dialog-bg`）。CSS 变量无效时用 fallback；`--el-bg-color` 又**没有**在 `html.dark` 里映射到项目 token，仍是 Element Plus 浅色默认白底。于是暗色下整片区域变成白色。

同类坑：文字侧曾用未映射的 `--el-text-color-*`（见 [[暗色主题下el-text-color变量仍是浅色值导致文字看不清]]）。背景侧同样不能「猜一个像 Element 的名字」再拿未映射的 `--el-*` 当兜底。

## 解决办法

改用真实存在的项目 token，例如：

- 分区底：`--bg-color-block` 或 `--dialog-bg` / `--card-bg`
- 列表底：`--bg-color-base`
- 代码块：`--bg-color-base` + `--text-color-primary` + `--border-color-light`

不要写不存在的 `--bg-color-overlay`，也不要用未在 `theme-dark.scss` 映射过的 `--el-bg-color` 做 fallback。

## 以后怎么避免

- 写背景前先查 [[主题与配色]]「变量体系」表，只用表里有的名字。
- `var(--x, var(--el-...))` 的 fallback 也必须是已映射或项目 token；未映射的 `--el-*` 在暗色下常等于浅色默认。
- 自绘面板（左右分栏、卡片、说明区）必须显式底色 + 字色，并在暗色主题下肉眼看一遍有无「大块白/大块黑」断层。
- 规则见 `.cursor/rules/ui-dark-theme-contrast.mdc`。

相关：[[主题与配色]]、[[暗色主题下el-text-color变量仍是浅色值导致文字看不清]]
