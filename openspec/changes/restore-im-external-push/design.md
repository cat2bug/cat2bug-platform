## Context

- 现状（Quarkus）：
  - 触发点：`DefectResource` → `DefectImNoticeService#sendDefectNotice`；`NoticeResource#send` → `ImNoticeDispatchService#sendNotice`；报告新建**没有**触发。
  - `ImNoticeDispatchService` 只写 `sys_notice` 并经 WebSocket 推站内通知 / 面板弹窗。
  - `ImConfigTestService`（system 模块）已实现三家平台的取 token、手机号查用户、钉钉 / 飞书签名和 OkHttp 调用，但只服务于配置页测试按钮；token 用 `ConcurrentHashMap` 缓存且**永不过期**。
  - 配置对象已在 `cat2bug-platform-common` 的 `domain/im/*`（`IMUserConfig`、`IMConfig`、各 `*PlatformConfig`、各 `*ProjectConfig`）。
  - 没有任何邮件依赖与配置。
- Spring 参照（本地 tag `spring-final-1.1.0`，模块 `cat2bug-platform-im`）：`IMService#sendMessage` 按渠道挑选 `IIMService` 实现，工厂类 `*MessageFactoryImpl` 用 `IMessageTemplate` 的 `toText / toMarkdown / toHtml` 生成正文，固定 3 线程池异步发送。
- 约束：
  - Native 交付（ADR-004：功能优先，不以体积裁功能）；新依赖必须 Native 可用。
  - 禁止硬编码密钥；邮件凭据只能来自运行期配置。
  - 多实例部署时缓存层由 `AppCacheStore`（local / redis）承担，但它**没有 TTL 能力**。

## Goals / Non-Goals

**Goals:**

- 缺陷、通知、报告三种事件的外部推送与 Spring 行为对等（渠道、开关、正文格式、链接）。
- 测试消息与正式推送共用一套平台客户端，修复 token 永不过期。
- 外部调用失败不影响业务请求、不影响其它渠道。
- 邮件可通过环境变量 / 安装配置启用，未配置时安静跳过。

**Non-Goals:**

- 不改前端配置页、不改数据库表。
- 不在安装向导中加邮件配置表单（后续单独变更）。
- 不做消息发送记录表、失败重投队列（Spring 也没有）。
- 不接入飞书 / 钉钉的消息回调（`receiveMessage` 在 Spring 中也是空实现）。

## Decisions

### D1 分层：平台客户端 + 渠道发送器 + 分发服务

- `cat2bug-platform-system/.../service/im/`：
  - `DingClient`、`FeishuClient`、`WeComClient`：从 `ImConfigTestService` 抽出的 HTTP 调用（取 token、手机号查用户、发个人消息、发群 Webhook、签名）。
  - `ImTokenCache`：按 `(平台, 应用标识)` 缓存 token，记录到期时间。
  - `ImMailSender`：封装 Quarkus Mailer。
- `cat2bug-platform-admin/.../service/`：
  - `ImNoticeDispatchService`：扩展为唯一分发入口 `dispatch(ImEvent)`，先同步写站内通知（保持现有行为），再把外部渠道任务交给专用线程池。
  - `ImMessageTemplates`：缺陷、通知、报告三种事件的 `text / markdown / html` 渲染。
- `ImConfigTestService` 保留对外方法签名，内部改调上述客户端。
- **备选**：照搬 Spring 的「工厂 + 服务 + 按 Bean 名查找」三层结构。否决：Quarkus 里按名字查 Bean 需要额外 CDI 限定，层次多但没有带来扩展价值；渠道固定为 5 个，直接分发更清楚。

### D2 渠道选择规则（对齐 Spring，钉钉群除外）

对每个事件，用 `ImUserConfigService.selectImUserConfigByProjectAndMember(projectId, memberId, defaultOption)` 取每个接收人的配置：

| 渠道 | 条件 | 粒度 |
|---|---|---|
| 站内 + 面板 | `platforms.system.configSwitch` | 每接收人 |
| 邮件 | `platforms.mail.configSwitch` 且用户有邮箱且邮件已配置 | 每接收人 |
| 钉钉个人 | `ding.singleSwitch` 且 `ding.mobile` 非空且项目有钉钉应用配置 | 每接收人 |
| 飞书个人 | `feishu.singleSwitch` 且 `feishu.mobile` 非空且项目有飞书 appId / appSecret | 每接收人 |
| 企业微信个人 | `wechat.singleSwitch` 且 `wechat.mobile` 非空且项目有企业微信应用配置 | 每接收人 |
| 钉钉 / 飞书 / 企业微信群 | 对应平台 `groupSwitch` 且 `hook` 非空 | **每事件一次**，取第一个接收人的配置 |

- 群消息取第一个接收人的配置，与 Spring 飞书 / 企业微信一致；钉钉群在 Spring 中按接收人重复发送，这里改为每事件一次（有意偏离，见 proposal）。
- 企业微信群 Webhook 不再要求项目有企业微信应用配置（Spring 工厂会因缺项目配置而跳过群消息，属缺陷）。

### D3 正文格式（对齐 Spring 工厂）

- 钉钉：群用 `text`，有关键词时前缀 `【关键词】`；个人用企业内部机器人 `sampleActionCard`，正文 Markdown，按钮「点击查看详情」，链接为 `src + "&screen=full"`。
- 飞书：群用 `text`，有关键词时前缀 `【关键词】`；个人用 `text`。
- 企业微信：群和个人都用 `markdown`；群可 `@手机号`。
- 邮件：HTML，主题为事件标题。
- 链接 `src`：沿用现有拼法（缺陷由 `DefectImNoticeService#buildLink` 生成；通知为 `{src}/#/notice/index?projectId=`；报告为 `{srcHost}/#/project/report?reportId=`）。实现时核对前端当前路由，若已改为 History 路由则按现行路由生成并在 tasks 中注明。

### D4 异步与失败隔离

- 专用线程池 `imPushTaskExecutor`（固定 3 线程、守护线程，写法参照 `GitWebhookAsyncConfig`）。
- 每个「渠道 × 接收人」（群消息为「渠道 × 事件」）是一个独立任务，异常只记 `WARN` 日志（带事件流水号、渠道、projectId，不带 token、secret、手机号全文）。
- **备选**：`@Blocking` + Mutiny / Vert.x 事件总线。否决：消息量小、HTTP 客户端是同步 OkHttp，固定线程池最简单，也与现有 Git Webhook 做法一致。

### D5 token 缓存：进程内 + 到期时间 + 失败刷新

- 进程内 `ConcurrentHashMap<key, (token, expiresAt)>`；到期前 5 分钟视为过期。
- 钉钉、飞书、企业微信的 token 接口都返回有效期（约 2 小时），按返回值记录；取不到时按 7000 秒。
- 业务调用返回「token 无效 / 过期」类错误码时，清除缓存、重新取 token 并**重试一次**。
- **备选**：放进 `AppCacheStore`（Redis）跨实例共享。否决：`AppCacheStore` 无 TTL；三家平台都允许多实例各自持有 token，进程内缓存足够。

### D6 邮件：Quarkus Mailer + 运行期配置

- 依赖 `io.quarkus:quarkus-mailer`（Vert.x Mail，官方支持 Native）。
- 配置项（全部运行期）：`quarkus.mailer.host / port / username / password / from / tls / start-tls`（Quarkus 3.15 中 `ssl` 已弃用，改用 `tls`），以及 `cat2bug.mail.enabled`（默认 `false`；`host` 与 `from` 都非空时才生效。注意 `quarkus.mailer.host` 内置默认值 `localhost`，实际起拦截作用的是 `enabled` 与 `from`，漏配 host 表现为发信 WARN）。环境变量示例写入 `deploy/docker/docker-compose.yml` 注释与部署文档。
- 未启用时邮件渠道直接跳过，启动时 `INFO` 记录一次「邮件未配置」。
- **备选**：Jakarta Mail（Angus）直接用。否决：Native 需要大量反射配置，Quarkus Mailer 已处理好。

### D7 报告新建触发

- 在报告新建成功后调用分发入口；接收人为项目内未锁定成员中，个人配置 `modules.report.event.new == true` 的人（与 Spring 一致；读取失败按「不发」处理）。
- 发送人为当前登录用户。

## Risks / Trade-offs

- [外部平台接口不可在 CI 中真实调用] → 平台客户端可注入基础 URL，单元测试用本地 HTTP 桩（`MockWebServer` 或 Vert.x 临时服务器）验证请求体、签名与 token 刷新；真实平台在验收时用测试账号手动验证。
- [Native 下 OkHttp / Mailer 反射或资源缺失] → 实现后跑 `deploy/build-native.sh` 与 `smoke-native-docker.sh`，并在冒烟中加入「邮件未配置时发送通知不报错」检查。
- [群消息取第一个接收人的配置，接收人配置不一致时可能漏发或用错 hook] → 与 Spring 行为一致，文档注明；不在本次改变配置模型。
- [邮件凭据泄露] → 只从运行期配置读取；日志、异常信息不输出密码；文档示例使用占位符。
- [历史公开仓库里的旧 SMTP 密码] → 本变更不引用；在变更摘要中提示轮换，不在代码或文档中复述。
- [Native 体积增加] → 重建后在 `docs/08-版本记录` 记录 UPX 前后体积。

## Migration Plan

- 无数据迁移。升级后已有的 IM 配置立即生效；邮件需要运维显式配置后才启用。
- 回滚：回退本变更提交即可，配置表结构未变。

## Open Questions

- 前端缺陷 / 通知 / 报告链接目前是 Hash 路由还是 History 路由（项目级路径键已上线），实现时以前端现行路由为准。
- 安装向导是否需要邮件配置表单：本次不做，后续视需求另开变更。

## 相关长期文档

- `docs/02-系统架构/整体架构.md`（需更正「im 能力已合并」的说法）
- `docs/02-系统架构/部署.md`（补邮件配置项）
- `docs/05-接口文档/接口清单-系统与监控.md`（`/im/*`、`/system/notice/send`）
- `docs/07-技术决策/ADR-004-Native交付分两道门并接受约38MiB.md`
- `docs/07-技术决策/ADR-010-移除Spring后端并统一模块命名.md`
- 拟新增 ADR：无（D1～D7 都是本功能内部的实现选择；若评审认为「群消息每事件一次」需要长期约束，再补 ADR）
- 拟新增技术方案文档：`docs/03-技术方案/IM多渠道推送.md`
