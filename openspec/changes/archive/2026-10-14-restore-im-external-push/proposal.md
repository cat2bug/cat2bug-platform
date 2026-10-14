## Why

Quarkus 恢复为默认后端时，IM 只做到「配置 + 测试消息 + 站内通知」。Spring `IMService` 会把缺陷变更、通知发送、报告新建推送到**钉钉、飞书、企业微信（群机器人 + 个人消息）和邮件**，Quarkus 没有移植；报告新建甚至连站内通知都没有。Spring 线已删除（`docs/07-技术决策/ADR-010-移除Spring后端并统一模块命名.md`），这个缺口不再有回退：配置了外部 IM 的用户点「测试」能收到，业务通知却永远收不到。按 ADR-004「先功能对等、不以体积裁功能」，需要补齐。

## What Changes

- 新增 IM 多渠道分发：一次通知事件（缺陷 / 通知 / 报告），按接收人的个人 IM 配置（`IMUserConfig.config.platforms`）和项目企业应用配置（`IMProjectConfig`）发往各渠道：
  - 站内通知 + 面板弹窗（现有，行为不变）
  - 邮件（HTML）
  - 钉钉：个人（企业内部机器人，按手机号查 userId）、群机器人（Webhook + 可选加签）
  - 飞书：个人（企业应用，按手机号查 user_id）、群机器人（Webhook + 可选签名）
  - 企业微信：个人（应用消息，按手机号查 userid）、群机器人（Webhook，可选 @手机号）
- 群机器人消息**每次事件只发一条**。Spring 对飞书、企业微信是这样做的，钉钉群却按接收人逐个重复发送；本次统一为每事件一条，属有意偏离。
- 外部推送**异步**执行；某个渠道或某个接收人失败，不影响其它渠道和业务请求本身。
- 缺陷、通知、报告三种事件各提供文本 / Markdown / HTML 三种正文，对齐 Spring `DefectMessageOfNoticeTemplateImpl`、`ReportMessageOfNoticeTemplateImpl`、`DefaultMessageTemplateImpl`。
- 报告新建补上通知触发：接收人为个人配置中开启 `modules.report.event.new` 的项目成员（对齐 Spring `SysReportServiceImpl#sendDefectNotice`）。
- 把 `ImConfigTestService` 里的取 token、手机号查用户、签名、HTTP 调用抽成共用的平台客户端，测试消息和正式推送共用；token 缓存增加**过期与失败后刷新重试**（现有测试缓存永不过期，约 2 小时后测试会失败）。
- 新增**邮件发送配置**：SMTP 主机、端口、账号、密码、发件人、SSL 均从运行期配置（环境变量或安装配置）读取，**不内置任何默认账号**；未配置时跳过邮件渠道并只记一次日志，不报错。
- **安全**：Spring 线 `application.yml` 曾硬编码真实 SMTP 账号与密码，仍留在公开历史中。本变更不沿用该配置；凭据轮换在变更摘要中单独提示。

## Capabilities

### New Capabilities

- `im-external-push`：缺陷、通知、报告事件按项目与个人 IM 配置推送到站内、邮件、钉钉、飞书、企业微信；覆盖渠道选择规则、群消息去重、异步与失败隔离、token 缓存与刷新、邮件配置与未配置时的行为。

### Modified Capabilities

（无：`openspec/specs/` 中没有现成的 IM / 通知规格。）

## Impact

- **后端代码**：
  - `cat2bug-platform-system`：`ImConfigTestService` 改为依赖新的平台客户端；新增钉钉、飞书、企业微信、邮件发送器。
  - `cat2bug-platform-admin`：`ImNoticeDispatchService` 成为多渠道分发入口；`DefectImNoticeService`、`NoticeResource#send` 改走新入口；报告新建处增加触发。
  - `cat2bug-platform-common`：按需补充 `domain/im/*` 配置对象字段（不改表）。
- **依赖**：新增 `io.quarkus:quarkus-mailer`（基于 Vert.x Mail，Native 友好）；OkHttp 已存在。Native 体积会略增，按 ADR-004 功能优先，重建后记录新体积。
- **配置**：新增邮件运行期配置项，写入部署文档和 Docker Compose 环境变量示例；本轮不改安装向导。
- **数据库**：无迁移，沿用 `sys_im_user_config`、`sys_im_project_config`、`sys_notice`、`sys_user.email`。
- **前端**：不改（配置页与测试按钮已存在）。
- **文档**：`docs/02-系统架构/整体架构.md` 现写「im 能力早已合并进四个模块」，不准确，随本变更更正；新增 IM 推送技术方案文档。
