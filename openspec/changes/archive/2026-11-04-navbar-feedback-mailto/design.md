## Context

顶栏 `Navbar.vue` 右侧已有主题、官网（`Cat2Bug/Site`）、源码、通知、文档等入口；官网组件用 `window.open('https://www.cat2bug.com')`。探索结论选定方案 A：`mailto:service@cat2bug.com`，零后端。已有图标 `email.svg`。不推翻任何 ADR。

## Goals / Non-Goals

**Goals:**

- 顶栏增加「提建议」图标，一键调起本机邮件客户端，收件人固定 `service@cat2bug.com`。
- 预填主题与只读环境信息，降低客服沟通成本。
- 交互与样式对齐现有 `right-menu-item`（tooltip、hover、焦点环规范）。

**Non-Goals:**

- 站内弹窗撰稿（方案 B）、服务端 SMTP/工单 API（方案 C）。
- 修改官网页面；不要求用户资料里必须有邮箱。
- 为无邮件客户端场景做完整产品化兜底（可后续再加「复制邮箱」）。

## Decisions

### D1：实现形态 — 轻量组件或 Navbar 内联

- **选择**：新增小型组件（如 `Cat2Bug/Feedback` 或 `Cat2Bug/SuggestMail`），在 `Navbar` 中与 `cat2-bug-site` 并列引用；`data-layout-nav="feedback"`（或 `suggest`）便于键盘提示锚点。
- **理由**：与 Site/Git/Doc 一致，逻辑（拼 mailto）可单测/单测友好；避免 Navbar 继续膨胀。
- **备选**：仅在 Navbar 内联 `@click` — 更短，但复用与测试较差。

### D2：打开方式 — `location.href` / `window.open` / `<a href>`

- **选择**：使用 `window.location.href = mailtoUrl` 或隐藏 `<a href="mailto:..." target="_self">` 点击；优先可编码的 `mailto:` 字符串。
- **理由**：部分浏览器对 `window.open(mailto)` 拦截不一致；`location.href` 对 mailto 更稳。
- **备选**：`window.open(mailto)` — 与 Site 一致但可能被拦截。

### D3：mailto 内容

- **To**：`service@cat2bug.com`（常量，单处定义）。
- **Subject**：i18n，例如「Cat2Bug 产品建议」。
- **Body**：若干行上下文 + 空行留给用户。建议字段（有则写、无则跳过）：应用版本（若前端可得）、`navigator.userAgent` 精简或浏览器语言、当前 i18n locale、登录昵称（非隐私敏感展示名）、当前项目名/ID（若 store 有）。
- **编码**：`encodeURIComponent` 拼 query；控制总长度，避免极端长 UA 撑爆部分客户端限制（可截断 UA）。

### D4：可见性

- **选择**：放在桌面 `device!=='mobile'` 工具组内，文档图标附近（官网/文档旁），与 Site/Doc 同显隐。
- **理由**：规格要求与 peer 工具一致；移动端后续若要入口可再进头像菜单。

### D5：i18n

- 新增键如 `feedback.suggest`（tooltip）、`feedback.mail-subject`、`feedback.mail-body-prompt`（正文引导句）；各语言包同步（至少 zh-CN / en-US，其余可先跟中/英占位策略与项目惯例一致）。

## Risks / Trade-offs

- [无本机邮件客户端] → 点击可能无响应；接受方案 A 局限，文案不承诺「已发送」。后续可加复制邮箱兜底。
- [mailto 长度限制] → 截断 UA / 精简字段。
- [隐私] → 正文只带昵称与项目名等用户已可见信息，不主动塞 token/密码。
- [键盘导航登记遗漏] → 若新增 `data-layout-nav`，对照 `layout-nav-hints` / 快捷键导航清单补锚点字母（无新字母需求则可只挂锚点不占快捷键）。

## Migration Plan

- 纯前端发布；无数据迁移。
- 回滚：移除 Navbar 入口与组件即可。

## Open Questions

- 无（方案 A 范围已定）。实现时若前端无统一版本号常量，版本行可省略或写 `1.2.0` 构建常量来源（以仓库现有 package/version 注入为准）。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/04-UI规范/组件与交互约定.md`（顶栏动作焦点环、交互约定）
- `docs/07-技术决策/说明.md`（本次不新增 ADR）
- 拟新增 ADR：无
