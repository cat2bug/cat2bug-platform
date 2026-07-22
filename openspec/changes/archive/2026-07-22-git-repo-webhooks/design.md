## Context

仓库 settings 壳（`GitRepoShell` + `SettingsLayout`）已有 general / danger / members。归档的 `git-repo-settings` 将 WebHook 列为非目标。平台已有 Smart HTTP push（JGit receive-pack）与 PR API（创建 / 评论 / merge / close），但没有任何仓级出站 webhook。产品决议：在管理里提供 Gitee/GitHub 风格的 WebHooks 配置；事件覆盖 Push、Tag Push、Pull Request；签名与 payload 对齐 GitHub；需要投递日志；不做 SSRF 出站限制；Secret 仅创建/重置时回传一次。

权限沿用有效 manage（ACL `admin`，或无 ACL 行时的项目 create-by/admin）。

## Goals / Non-Goals

**Goals:**

- 仓级 Webhook CRUD + 启停 + 事件订阅（push / tag / pull_request）
- Secret：创建或重置响应中明文一次；列表/详情永不回传；之后只能重置
- GitHub 兼容：`X-Hub-Signature-256: sha256=<hex>`（HMAC-SHA256 over raw body）；`X-GitHub-Event`（或等价 `X-Cat2Bug-Event` 另议，默认跟 GitHub 事件名）；payload 字段风格对齐 GitHub（`ref`、`before`/`after`、`commits`、`repository`、`sender`、`pull_request`、`action` 等）
- 异步投递；写入 delivery 记录（时间、事件、HTTP 状态、耗时、成功/失败、截断后的响应摘要）
- Settings 侧栏「访问控制」组增加 WebHooks（成员之后）；路由 `.../settings/webhooks`（列表）、新建/编辑与投递记录子页或抽屉
- Flyway：`V1_1_0_11__git_repo_webhooks.sql`（h2 + mysql 同步；以 `./scripts/new-flyway-migration.sh` 实际序号为准）

**Non-Goals:**

- Issue / 检查项 / 评论类事件
- SSRF / 私网 URL 拦截、出站代理配置 UI
- 连续镜像、入站 webhook（接收外部系统回调）
- 保证投递 exactly-once；不保证对方幂等（仅 at-least-once 尽力 + 日志）
- 保护分支 / 部署公钥 / 仓库镜像（其它 settings 项）

## Decisions

### D1. 数据模型

- `sys_git_repository_webhook`：`webhook_id`, `repository_id`, `url`, `alias`, `secret_cipher`（或可逆加密/信封加密字段）, `events`（JSON 数组或位图：`push`,`tag`,`pull_request`）, `active`, 审计字段
- `sys_git_webhook_delivery`：`delivery_id`, `webhook_id`, `event`, `delivery_guid`, `request_headers_summary`, `request_body`（可截断存储）, `response_status`, `response_body_truncated`, `duration_ms`, `success`, `error_message`, `created_at`
- 保留最近 N 条/webhook（如 50）或按天清理；首期实现「按 webhook 列表最近 50」即可

### D2. Secret 生命周期

- 创建：服务端生成或接受用户输入的 secret → 加密入库 → **响应体一次性返回 plaintext**
- 列表/GET：字段为 `secretConfigured: true`，无明文
- 重置：`POST .../webhooks/{id}/secret/regenerate` → 新明文仅本次返回，旧 secret 立即失效
- 签名始终用当前 secret；无 secret 的 webhook 不允许创建（强制配置，便于通用对接）

### D3. 事件映射

| 产品事件 | 触发点 | GitHub 风格 `X-GitHub-Event` / action |
|----------|--------|----------------------------------------|
| Push | receive-pack 成功且存在非 tag 的 ref 更新 | `push` |
| Tag Push | receive-pack 成功且存在 `refs/tags/*` 更新 | `create`/`delete`（tag）或 `push`（带 tag ref）——实现选 **GitHub：tag 推送用 `push` 且 `ref` 为 `refs/tags/...`，纯建删 tag 用 `create`/`delete`**；UI「Tag Push」订阅覆盖这三类 |
| Pull Request | PR create / 源更新（synchronize）/ merge / close | `pull_request` + `action`: `opened`/`synchronize`/`closed`（merged 时 `merged:true`） |

Push 与 Tag 从同一 receive-pack 结果拆分投递，避免重复订阅用户收到两次无关 payload。

### D4. 投递执行

- 触发后入队（Spring `@Async` / 简单线程池即可；首期不做独立 MQ）
- HTTP 客户端：超时（如 10s）、跟随重定向策略保守（可不跟随）
- 失败：记录日志；可选有限次重试（如 3 次指数退避）——写入 design 为「建议实现，tasks 勾选」
- **Git 操作与 PR API 不因投递失败而回滚**

### D5. API 与 UI

- `GET/POST /system/git/repo/{id}/webhooks`
- `GET/PUT/DELETE /system/git/repo/{id}/webhooks/{webhookId}`
- `POST .../webhooks/{webhookId}/secret/regenerate`
- `GET .../webhooks/{webhookId}/deliveries`
- `POST .../webhooks/{webhookId}/test`（可选：发送 ping 事件）
- 权限：全部 `canManage` / ACL admin
- UI：列表 + 表单（URL*、别名、Secret、事件多选、激活）+ 投递记录；创建/重置后模态展示 secret 一次

### D6. Payload 原则

- 优先 **GitHub 文档字段子集**，保证 Jenkins / 通用 receiver 可验签
- `repository` 块用平台 `full_name`（`teamKey/projectKey/slug` 或 clone 路径语义）、`clone_url`、`html_url`（SPA browse URL）
- 不追求 100% GitHub 字段完备；缺省字段可省略，但签名覆盖完整 raw body

### D7. 与 settings 导航

- `SettingsLayout`「访问控制」组增加 WebHooks（成员 → WebHooks）
- `git-repo-settings` 规格增加导航 ADDED；Purpose 文案可在归档时同步

## Risks / Trade-offs

- [任意 URL 出站] → 产品明确不做 SSRF；文档提示仅配置可信端点；运维可用网络层约束
- [Secret 明文只显示一次] → UI 必须强提示复制；丢失则重置
- [异步投递丢任务（进程重启）] → 首期可接受；后续可落库 outbox
- [GitHub 兼容不完全导致个别工具失败] → 提供「测试投递」与 delivery 日志；文档列出已支持事件/字段
- [大 push 的 commits 列表过大] → payload 截断 commits（如最多 20）并设 `truncated:true`，与 GitHub 类似

## Migration Plan

1. Flyway 建表（h2/mysql）
2. 部署后端（新 API + 钩子）；前端 settings 入口
3. 回滚：停用钩子调用 + 忽略表；或 Flyway 不回滚仅停用功能开关（首期无开关则回滚版本）

## Open Questions

- `test`/ping 事件是否首期必做（建议做，成本低）
- delivery body 截断长度具体值（建议请求/响应各 16–64KB）
- Tag 订阅是单一 `tag` 开关还是拆 create/delete/push-tag（UI 用单一「Tag Push」即可，内部映射）
