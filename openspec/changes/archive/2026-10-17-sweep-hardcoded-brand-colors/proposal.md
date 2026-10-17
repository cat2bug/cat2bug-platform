## Why

`unify-admin-ui` 只改了主题 token。业务页里写死的 `#409eff` 和 `#FFC107` 仍会盖过 token，暗色悬停和链接会回到旧蓝或琥珀黄。

## What Changes

- 把 `cat2bug-platform-ui/src` 里作为品牌色的 `#409eff`、`#FFC107`、`#ffce3a`、`#FFD54F` 改成 `var(--cat2bug-primary, #1890ff)` 或 `#1890ff` / `#69b1ff`。
- 图表、设置默认主题等不能用 CSS 变量的地方，字面量改为 `#1890ff`。
- 不改像素验证码插画、Markdown 引用块警告色、项目预设图标里的琥珀块。

## Capabilities

### New Capabilities

- `brand-color-literals`: 业务样式不再写死旧品牌蓝和琥珀黄。

### Modified Capabilities

（无）

## Impact

- 前端样式与少量 JS 颜色常量。
- 不改接口、表结构和 `Cat2BugTable` 行为。
