## Why

仓库管理侧栏已具备基本信息、删除与成员 ACL，但缺少向外部系统（CI、通知、自建自动化）出站通知的能力。团队需要在 push、tag 与 Pull Request 生命周期上配置 GitHub 兼容的 Webhook，以便用同一套签名与 payload 对接通用工具，而不必依赖轮询或平台内嵌集成。

## What Changes

- 在仓库 settings「访问控制」分组下增加 **WebHooks** 入口（列表 / 新建 / 编辑）
- 持久化仓级 Webhook 配置：URL、别名、Secret、订阅事件、启停；**Secret 创建（或重置）后仅展示一次，之后只能重置**
- 支持事件：**Push**、**Tag Push**、**Pull Request**（不含 Issue / 检查项 / 评论）
- 出站投递：**GitHub 风格 JSON payload** + **HMAC-SHA256**（`X-Hub-Signature-256`）；异步投递；**投递日志**可查
- 不做 SSRF / 私网出站拦截（允许用户配置任意可达 URL）
- 配置与查看投递日志仅 **有效仓库 manage（ACL `admin` 或无 ACL 时的项目 manage）** 可操作

## Capabilities

### New Capabilities

- `git-repo-webhooks`: 仓级 Webhook CRUD、Secret 一次性展示与重置、事件订阅、GitHub 兼容签名与 payload、异步投递与投递日志、settings UI

### Modified Capabilities

- `git-repo-settings`: 侧栏「访问控制」组增加 WebHooks 导航与路由（`settings/webhooks` 等）
- `git-smart-http`: push / tag 类 ref 更新成功后触发对应 webhook 事件
- `git-repo-pull-requests`: PR 创建 / 更新 / 合并 / 关闭等生命周期触发 webhook 事件

## Impact

- 后端：新表（webhook 配置 + delivery）、CRUD API、投递服务、Smart HTTP receive-pack 与 PR 服务钩子
- 前端：settings 侧栏与 WebHooks 列表/表单/投递记录页、i18n（全语言）
- 安全：Secret 密文存储；响应中仅在 create/regenerate 时回传明文一次
- 无对现有 clone/push 协议的 **BREAKING** 变更；出站失败不影响 Git 操作成功路径
