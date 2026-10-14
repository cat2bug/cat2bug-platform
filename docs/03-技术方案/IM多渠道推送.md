# IM 多渠道推送

缺陷变更、手动发送通知、报告新建三类事件，除写站内通知外，还按成员的「IM 个人配置」推送到钉钉、飞书、企业微信（个人消息 + 群机器人）和邮件。新增事件类型、新增渠道或排查「某人没收到推送」时参考。

## 概述

- 统一入口 `ImNoticeDispatchService#dispatch(ImEvent)`：先**同步**写站内通知并推 WebSocket（与改造前一致，失败直接报错），再把每个外部渠道各作为一个任务提交到线程池 `imPushTaskExecutor`（固定 3 个守护线程，线程名 `im-push-N`）。外部渠道失败只记 WARN，不影响业务接口返回，也不影响其它渠道。
- 三类事件：
  - 缺陷：`DefectImNoticeService`，`moduleKey=defect`，标题如 `[PROCESSING]缺陷:#1 名称`。
  - 通知：`NoticeResource#send` → `sendNotice` → `dispatch`，`moduleKey` 为空（不检查实时开关）。
  - 报告新建：`ReportResource#add` 保存成功后调 `ReportImNoticeService`，标题「报告:<报告标题>」，`moduleKey=report`。
- 正文由 `ImMessageTemplates` 生成 `text / markdown / html` 三种格式：站内通知用 markdown（与旧版逐字一致），钉钉 / 企业微信用 markdown，飞书用 text，邮件用 html（用户输入已转义）。

## 关键流程

1. **选接收人**：
   - 缺陷 / 通知：调用方给出接收人，去重。
   - 报告：项目成员中 `project_lock = 0` 且个人配置 `modules.report.event.new` 为真者。
   - 事件带 `moduleKey` 时，还要求接收人 `modules.<moduleKey>.option.realtime.switch` 为真（与 Spring 线一致）；个人配置读取失败的接收人记 WARN 后跳过。
2. **站内通知**：`platforms.asystem.switch` 为真时写 `sys_notice` 并推送；`panel` 决定是否弹面板。
3. **群消息**：每个事件只发一次，取**第一个有效接收人**的配置（hook / secret）：
   - 钉钉群：文本 `【关键词】` + text，带签名。
   - 飞书群：文本 `【关键词】` + text，带 timestamp / sign。
   - 企业微信群：markdown，并 @ 该接收人的 `wechat.mobile`；只依赖 Webhook，不需要项目应用配置。
4. **个人消息**：需要项目级应用配置（`im_project_config`，按 systemCode 读取 `DingProjectConfig` / `FeishuProjectConfig` / `EnterpriseWeChatProjectConfig`），缺失时 WARN 跳过：
   - 钉钉：机器人单聊 ActionCard，按钮链接追加 `screen=full`；需要 `robotCode`。
   - 飞书：按手机号查用户后发 text。
   - 企业微信：应用消息 markdown；错误码 60020 表示服务器 IP 不在应用可信 IP 列表。
5. **邮件**：`platforms.bmail.switch` 为真；收件人优先 `bmail.receiver`，为空时用 `sys_user.email`；邮件未配置时整个渠道跳过。
6. **access token**：`ImTokenCache` 按「平台 + 应用标识」缓存（企业微信为 `corpId:agentId`），到期前 5 分钟视为过期，取不到有效期时按 7000 秒；平台返回 token 失效类错误码时清缓存并重试一次。
7. **链接**：按 History 路由生成（见 [[项目级History路由与路径键]]）：
   - 缺陷 `{host}/{teamKey}/{projectKey}/defect?projectId=&defectId=`
   - 报告 `{host}/{teamKey}/{projectKey}/report?projectId=&reportId=`
   - 通知 `{host}/notice/index?projectId=`
   - `host` 来自请求体的 `srcHost`（报告为 `SysReport.srcHost`，不落库）；取不到路径键时用 `project` 作路径段。

## 邮件配置

默认关闭，`application.properties` 只放不含凭据的默认值。启用时在运行期（环境变量或安装配置）同时提供：

| 配置项 | 环境变量 | 说明 |
|---|---|---|
| `cat2bug.mail.enabled` | `CAT2BUG_MAIL_ENABLED` | `true` 才启用 |
| `quarkus.mailer.host` | `QUARKUS_MAILER_HOST` | SMTP 主机，必填 |
| `quarkus.mailer.port` | `QUARKUS_MAILER_PORT` | 端口 |
| `quarkus.mailer.from` | `QUARKUS_MAILER_FROM` | 发件地址，必填 |
| `quarkus.mailer.username` / `password` | `QUARKUS_MAILER_USERNAME` / `QUARKUS_MAILER_PASSWORD` | 按需 |
| `quarkus.mailer.tls` | `QUARKUS_MAILER_TLS` | 隐式 TLS（如 465 端口） |
| `quarkus.mailer.start-tls` | `QUARKUS_MAILER_START_TLS` | `REQUIRED` / `OPTIONAL` / `DISABLED`（如 587 端口） |

`enabled` 不为真或 `from` 为空即视为未配置：启动时记一次 INFO「邮件未配置」，之后静默跳过。`host` 虽也参与判断，但 Quarkus Mailer 给它内置了默认值 `localhost`，漏配时不会被判为未配置，而是每次发信连本机 25 端口失败并记 WARN，见 [[Quarkus-Mailer的host默认localhost]]。测试 profile 用 `%test.quarkus.mailer.mock=true`。

## 关键代码位置

- 分发：`cat2bug-platform-admin/src/main/java/com/cat2bug/admin/service/ImNoticeDispatchService.java`、`service/im/ImEvent.java`
- 事件来源：`DefectImNoticeService.java`、`ReportImNoticeService.java`、`resource/NoticeResource.java`、`resource/ReportResource.java`
- 正文与链接：`service/im/ImMessageTemplates.java`、`service/im/ImLinkBuilder.java`
- 平台客户端：`cat2bug-platform-system/src/main/java/com/cat2bug/system/service/im/`（`DingClient`、`FeishuClient`、`WeComClient`、`ImMailSender`、`ImTokenCache`、`AbstractImClient`）
- 线程池：`cat2bug-platform-system/src/main/java/com/cat2bug/system/config/ImPushAsyncConfig.java`
- 配置页测试接口：`cat2bug-platform-system/src/main/java/com/cat2bug/system/service/ImConfigTestService.java`（复用同一组客户端）
- Native 反射：`cat2bug-platform-admin/src/main/java/com/cat2bug/admin/nativeimage/ImReflectionConfig.java`

## 注意事项

- 日志只带流水号 `sn`、渠道、projectId、memberId 和异常消息；不得输出 token、secret、邮件密码、完整手机号（用 `AbstractImClient.maskMobile`）。
- 邮件账号密码只能来自运行期配置，不得写进代码、`application.properties`、compose 文件或文档示例。
- 新增事件类型：构造 `ImEvent` 调 `dispatch`，需要实时开关时传 `moduleKey`，并在 `ImMessageTemplates` 加模版。
- 群消息取第一个接收人的配置：同一项目内各成员群配置不一致时，只有第一个人的群会收到。
- 平台基础 URL 是客户端的可注入字段，测试用 JDK `HttpServer` 桩（`ImHttpStub`）替代真实平台。

## 相关规格

- `openspec/changes/restore-im-external-push/`（归档后在 `openspec/changes/archive/` 下）

返回 [[03-技术方案/说明|03-技术方案]]
