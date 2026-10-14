## 1. 准备

- [x] 1.1 确认工作区干净，记录当前 HEAD；对照 tag `spring-final-1.1.0` 列出要移植的 Spring 类（`IMService`、各 `*MessageServiceImpl`、各 `*MessageFactoryImpl`、三个 `*MessageOfNoticeTemplateImpl`）
  - HEAD `1dbb658a`；移植对象：`IMService`、`Ding/Feishu/FeishuApp/EnterpriseWeChat/Mail/Notice/PanelNotice` 的 `*MessageServiceImpl` 与 `*MessageFactoryImpl`、`DefaultMessageTemplateImpl`、system 模块 `DefectMessageOfNoticeTemplateImpl`、`ReportMessageOfNoticeTemplateImpl`
- [x] 1.2 根 `pom.xml` 依赖管理与 `cat2bug-platform-system` 加入 `io.quarkus:quarkus-mailer`；如需 HTTP 桩测试，加入测试依赖（优先复用已有依赖）；`mvn -pl cat2bug-platform-system -am -DskipTests install` 通过
  - 根 `pom.xml` 不导入 Quarkus BOM，版本由 system 模块自带的 `quarkus-bom` 管理，只改 system `pom.xml`；HTTP 桩用 JDK 自带 `com.sun.net.httpserver`，不新增测试依赖；并行开发期间用 `package` 代替 `install` 验证
- [x] 1.3 核对前端缺陷 / 通知 / 报告详情的现行路由（Hash 或 History、项目路径键），确定三种事件的链接格式
  - 前端为 History 路由（`createWebHistory`），旧 `/#/...` 链接会落到首页。链接改为：缺陷 `{host}/{teamKey}/{projectKey}/defect?projectId=&defectId=`，报告 `{host}/{teamKey}/{projectKey}/report?projectId=&reportId=`（取不到路径键时退回 `/project/defect`、`/project/report` 旧路径，由前端重定向）；通知 `{host}/notice/index?projectId=`

## 2. 平台客户端与 token 缓存

- [x] 2.1 新增 `ImTokenCache`：按「平台 + 应用标识」缓存 token 与到期时间，到期前 5 分钟视为过期，支持主动失效
- [x] 2.2 从 `ImConfigTestService` 抽出 `DingClient`、`FeishuClient`、`WeComClient`（取 token、手机号查用户、个人消息、群 Webhook、签名），基础 URL 可注入；token 无效类错误码清缓存并重试一次
- [x] 2.3 `ImConfigTestService` 改为调用三个客户端，对外方法签名不变；配置页六个测试接口行为不变
  - 签名与成功路径不变；平台返回错误码（如群关键词不匹配、手机号查不到人）时，测试接口现在返回「测试消息发送失败: …」，原先会静默返回成功
- [x] 2.4 客户端单元测试：用本地 HTTP 桩验证钉钉 / 飞书签名、请求体、token 过期重取、token 失效重试一次（`ImClientsTest`，桩为 JDK `HttpServer`）

## 3. 邮件

- [x] 3.1 新增 `ImMailSender`（Quarkus Mailer）：`cat2bug.mail.enabled` 为假或 `host` / `from` 为空时跳过；启动时记录一次「邮件未配置」
  - Quarkus Mailer 的 `host` 自带默认值 `localhost`，实际开关是 `cat2bug.mail.enabled` 与 `from`
- [x] 3.2 `application.properties` 只放不含凭据的默认值（`cat2bug.mail.enabled=false`、`quarkus.mailer.mock=false` 等）；测试 profile 用 Mailer mock
  - Quarkus 3.15 里隐式 TLS 用 `quarkus.mailer.tls`（`ssl` 已弃用），STARTTLS 用 `quarkus.mailer.start-tls`
- [x] 3.3 单元测试：未配置时跳过不抛异常；已配置（mock）时收件人、主题、HTML 正文正确（`ImMailSenderTest`，用记录型 `Mailer` 替身）

## 4. 消息正文模版

- [x] 4.1 新增 `ImMessageTemplates`：移植 Spring 缺陷、报告、默认（通知）三种模版的 `text / markdown / html`
  - 缺陷字段沿用 Quarkus 现有站内通知（编号、名称、状态、处理人、描述），未移植 Spring `DefectNoticeFieldSupport` 的「按项目字段布局列出全部字段」；文本 / HTML 额外带上缺陷编号名称、报告标题（Spring 的文本正文里没有，飞书 / 钉钉群收到的消息会看不出是哪个缺陷）
- [x] 4.2 `DefectImNoticeService` 现有 Markdown 正文改为调用该模版，保证站内通知内容不变（对比改动前后输出）
  - 结构逐字一致（`defectMarkdownKeepsPreviousSiteNoticeFormat`）；其中的链接按 1.3 改为 History 路由
- [x] 4.3 模版单元测试：同一缺陷三种格式都包含编号与名称；HTML 对用户输入做转义（`ImMessageTemplatesTest`）

## 5. 多渠道分发

- [x] 5.1 新增 `imPushTaskExecutor`（固定 3 线程、守护线程，参照 `GitWebhookAsyncConfig`）（`ImPushAsyncConfig`，线程名 `im-push-N`）
- [x] 5.2 `ImNoticeDispatchService` 扩展为 `dispatch(ImEvent)`：同步写站内通知与面板推送（行为不变），再按 design D2 规则提交外部渠道任务；群消息每事件一次，取第一个接收人配置
  - 与 Spring 一致：事件带 `moduleKey` 时要求接收人 `modules.<module>.option.realtime.switch` 为真；个人配置读取失败的接收人记 WARN 后跳过
- [x] 5.3 钉钉 / 飞书 / 企业微信个人消息与群消息、邮件五个渠道的发送任务；日志带流水号、渠道、projectId，不输出 token、secret、完整手机号
  - 邮件收件人优先 `mail.receiver`，为空时用 `sys_user.email`；钉钉个人卡片链接追加 `screen=full`；项目缺应用配置时 WARN 跳过对应个人渠道
- [x] 5.4 `DefectImNoticeService`、`NoticeResource#send` 改走 `dispatch`（`NoticeResource` 未改，仍调 `sendNotice`，后者内部走 `dispatch`）
- [x] 5.5 报告新建成功后触发：接收人为项目未锁定成员中 `modules.report.event.new` 为真者，标题「报告:<报告标题>」（`ReportImNoticeService`，`SysReport.srcHost` 不落库）
- [x] 5.6 分发单元测试：渠道开关组合、群消息只发一次、企业微信群不依赖应用配置、某渠道抛异常不影响其它渠道、报告接收人过滤（`ImNoticeDispatchServiceTest` 8 个、`ReportImNoticeServiceTest` 1 个、集成测试 `QuarkusReportImNoticeTest`）

## 6. 验证

- [x] 6.1 `unset QUARKUS_CONFIG_LOCATIONS CAT2BUG_INSTALL_CONFIG_PATH && mvn install` 全量测试通过
  - 为避免写 `~/.m2`，实际执行 `mvn -o verify -Dquarkus.http.test-port=0`：191 个测试，失败 0，跳过 2，BUILD SUCCESS
- [x] 6.2 JVM 单 JAR 冒烟：未配置邮件时修改缺陷、发送通知、新建报告均成功，站内通知到达，日志无异常堆栈
  - fast-jar（`quarkus-app/quarkus-run.jar`）在临时目录以 2032 端口启动，经安装向导装新 H2 库后自重启；新建缺陷、指派、发送通知、新建报告均返回 200，三组站内通知都到达；邮件「未配置」INFO 仅启动时一次，钉钉个人因项目无应用配置 WARN 跳过，无异常堆栈；用后已关闭
- [ ] 6.3 用测试账号手动验证：钉钉 / 飞书 / 企业微信群与个人各收到一条缺陷通知；配置测试 SMTP 后收到邮件（无测试账号的渠道在 tasks 中注明未验证）
  - 未验证：需用户提供测试账号（钉钉 / 飞书 / 企业微信应用与群机器人、测试 SMTP）。自动化部分已用本地 HTTP 桩与 Mailer 替身覆盖
- [ ] 6.4 `deploy/build-native.sh` 重建，记录 UPX 前后体积；`smoke-native-docker.sh arm64` 与 `FRESH=true` 通过
  - 未执行：合并后由主 agent 执行（`ImReflectionConfig` 已为项目应用配置类注册反射）

## 7. 文档与回写长期知识

- [ ] 7.1 `docs/02-系统架构/整体架构.md` 更正「im 能力已合并」的说法；`docs/02-系统架构/部署.md` 补邮件配置项；`deploy/docker/docker-compose.yml` 加邮件环境变量注释示例（占位符）
- [ ] 7.2 新增 `docs/03-技术方案/IM多渠道推送.md`（渠道规则、群消息去重、token 缓存、邮件配置），加入总览与目录说明
- [ ] 7.3 可复用的坑写入 `docs/06-踩坑记录/`（没有则注明无）；`docs/08-版本记录/2026-10.md` 记录本变更与 Native 新体积；`check_links.py` 坏链为 0
