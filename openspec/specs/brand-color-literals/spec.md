# brand-color-literals Specification

## Purpose

业务样式不写死旧品牌蓝与琥珀黄，品牌强调统一走主题 token。

## Requirements

### Requirement: Brand colors are not hardcoded

Admin UI styles MUST NOT hardcode `#409eff` or amber brand colors `#FFC107`, `#ffce3a`, and `#FFD54F`. Brand emphasis MUST use `--cat2bug-primary` (`#1890ff`) or, for dark-theme links and focus, `#69b1ff`.

#### Scenario: Fallback color

- **WHEN** a style uses a CSS variable fallback for the brand color
- **THEN** the fallback MUST be `#1890ff` or `#69b1ff`, not `#409eff` or `#FFC107`

### Requirement: Illustration amber stays

Pixel captcha illustration colors, Markdown blockquote warning chrome, and preset project icon artwork MUST keep their existing amber fills.

#### Scenario: Captcha sun

- **WHEN** the pixel captcha draws the sun
- **THEN** the sun fill MUST remain the existing illustration yellow
