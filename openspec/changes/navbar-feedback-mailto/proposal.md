## Why

用户想在产品内快速向官网客服邮箱 `service@cat2bug.com` 提建议，目前顶栏只有官网/源码/文档等外链，没有「写信」入口。现在用轻量 `mailto` 方案即可上线，无需后端发信与防刷。

## What Changes

- 顶栏右侧新增「提建议」邮件图标按钮（复用现有 `email` 图标风格，与官网/文档同类入口一致）。
- 点击后打开系统默认邮件客户端，收件人为 `service@cat2bug.com`；预填建议类主题，正文可附带版本/环境等只读上下文（便于客服定位）。
- 不引入站内发信 API、不新增 SMTP/通知通道；无本机邮件客户端时行为依赖浏览器，本次不做弹窗撰稿（方案 B）或服务端投递（方案 C）。
- 补充 i18n 文案；若顶栏键盘导航清单需登记新锚点，一并纳入。

## Capabilities

### New Capabilities

- `navbar-feedback-mailto`：顶栏「提建议」入口经 `mailto` 联系 `service@cat2bug.com` 的行为与可见性约定。

### Modified Capabilities

- （无）不修改既有 capability 的需求条文；视觉上与 `admin-visual-system` / `action-focus-ring` 对齐，但不改其要求。

## Impact

- 前端：`cat2bug-platform-ui/src/layout/components/Navbar.vue`（及可选小型 `Cat2Bug` 邮件入口组件）、i18n、`layout-nav` 相关键盘提示（若登记新 `data-layout-nav`）。
- 后端 / 数据库 / 邮件服务：无。
- 文档：实现后在 `docs/08-版本记录` 当月一行；长期 UI 约定若补充「顶栏外联入口」可引用本变更，不另立 ADR。
