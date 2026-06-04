## ADDED Requirements

### Requirement: 登录页专用视觉 token

系统 SHALL 在登录页根容器（`.logo-page`）上提供专用 CSS 变量，用于背景、卡片、强调色与文案层级。

在 `html.dark` 下，页面背景 MUST 接近 mockup 深色（`#0D0D0D` 量级），卡片背景 MUST 接近 `#1A1A1A`，主强调色 MUST 为黄色（`#FFC107` 量级）。

在浅色主题下，同一组变量 MUST 赋值为可读对比度，不得依赖未定义的变量回退到不可读组合。

系统 MUST NOT 在进入 `/login` 时强制修改全局 `themeMode` 或强制添加 `html.dark`。

#### Scenario: 用户系统主题为 light 时仍可登录

- **WHEN** 用户 `themeMode` 为 `light` 并打开 `/login`
- **THEN** 页面使用浅色 `--login-*` token 正常显示，且表单可阅读

#### Scenario: 用户系统主题为 dark 时对齐 mockup 底色

- **WHEN** 用户 `themeMode` 为 `dark` 并打开 `/login`
- **THEN** 页面背景明显深于全局 `--bg-color-base`（`#1e1e20`），接近 mockup

### Requirement: 左栏品牌双色与装饰线

左栏品牌区 MUST 展示 Logo 与「Cat2Bug-Platform」文案，其中：

- `Cat2` 部分为主文字色（白/近白）
- `Bug` 部分为 `--login-accent` 黄色
- `-Platform` 部分为 muted 灰色

Logo 下方 MUST 有一条短横线（黄色，宽约 40–56px，高 2–3px）。

#### Scenario: 暗色下品牌双色可见

- **WHEN** 在 dark 主题打开登录页左栏
- **THEN** 可见 Cat2 白、Bug 黄、-Platform 灰与黄色装饰线

### Requirement: 左栏 Hero 与特性列表 i18n

左栏 MUST 包含：

1. 主标题（`login.hero.title`）
2. 一段描述（`login.hero.desc`）
3. 三条特性，每条含图标、标题、说明（`login.features.*`）

上述文案 MUST 通过 `$t()` 读取，不得硬编码单一语言。

系统 MUST 在 `zh_CN`、`zh_TW`、`en_US`、`ru`、`ja_JP`、`ko_KR`、`ar` 七个语言文件中提供对应翻译。

#### Scenario: 切换英文显示 mockup 文案

- **WHEN** 用户在登录页将语言切换为 `en_US`
- **THEN** Hero 标题为 “Simplify Bugs, Strengthen Software.” 量级英文，三条 Feature 为英文标题与描述

#### Scenario: 切换中文显示本地化 slogan

- **WHEN** 用户将语言切换为 `zh_CN`
- **THEN** Hero 与三条 Feature 显示中文翻译，而非英文硬编码

### Requirement: 左栏响应式

视口宽度小于 980px 时，左栏 MAY 隐藏（与现有行为一致），仅展示登录表单区域。

#### Scenario: 窄屏隐藏左栏

- **WHEN** 视口宽度 ≤ 980px
- **THEN** `.login-introduce` 不显示，登录表单仍可用
