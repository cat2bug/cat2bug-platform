## ADDED Requirements

### Requirement: 通知事件按接收人配置分发到各渠道
系统 SHALL 在缺陷变更、通知发送、报告新建三种事件发生时，按每个接收人在该项目下的个人 IM 配置决定渠道：`system.configSwitch` 开启发站内通知与面板弹窗；`mail.configSwitch` 开启且用户有邮箱时发邮件；钉钉、飞书、企业微信的 `singleSwitch` 开启且 `mobile` 非空、且项目有对应企业应用配置时发个人消息。

#### Scenario: 只开站内通知
- **WHEN** 接收人只开启 `system.configSwitch`，其它渠道开关均关闭
- **THEN** 系统只写入站内通知并推送 WebSocket，不调用任何外部平台

#### Scenario: 开启钉钉个人消息
- **WHEN** 接收人开启钉钉 `singleSwitch` 且填写了手机号，项目已配置钉钉 `appKey`、`appSecret`、`robotCode`
- **THEN** 系统按手机号查询钉钉 userId，并通过企业内部机器人向该用户发送一条消息，消息包含事件标题、Markdown 正文和「点击查看详情」链接

#### Scenario: 项目缺少企业应用配置
- **WHEN** 接收人开启飞书 `singleSwitch` 且填写了手机号，但项目没有飞书 `appId` / `appSecret`
- **THEN** 系统跳过该接收人的飞书个人消息，记录一条警告日志，其它渠道照常发送

### Requirement: 群机器人消息每个事件只发一条
系统 SHALL 对钉钉、飞书、企业微信的群机器人消息，在一次事件中各最多发送一条，使用第一个接收人的个人配置中的 `groupSwitch`、`hook`、`secret`、`key`。

#### Scenario: 多个接收人都开启钉钉群
- **WHEN** 一次缺陷变更有 3 个接收人，第一个接收人开启钉钉 `groupSwitch` 并配置了 hook
- **THEN** 钉钉群 Webhook 只被调用 1 次

#### Scenario: 群机器人加签与关键词
- **WHEN** 群配置填写了 `secret` 和 `key`
- **THEN** 钉钉请求 URL 带 `timestamp` 与 HmacSHA256 签名，飞书请求体带 `timestamp` 与 `sign`，消息正文以 `【key】` 开头

#### Scenario: 企业微信群不依赖企业应用配置
- **WHEN** 第一个接收人开启企业微信 `groupSwitch` 并配置 hook，项目没有企业微信应用配置
- **THEN** 系统仍向该 Webhook 发送一条 Markdown 消息

### Requirement: 外部推送异步执行且失败隔离
系统 SHALL 在业务请求内同步完成站内通知写入，外部渠道推送在后台线程执行；单个渠道或单个接收人发送失败 MUST NOT 影响业务请求的响应、其它渠道或其它接收人。

#### Scenario: 钉钉接口超时
- **WHEN** 缺陷状态变更触发通知，钉钉接口超时
- **THEN** 缺陷接口照常返回成功，站内通知与其它渠道照常送达，日志记录钉钉发送失败且不包含 token、secret

### Requirement: 平台 token 缓存会过期并在失效时刷新
系统 SHALL 按「平台 + 应用标识」缓存访问 token，并在到期前 5 分钟视为过期重新获取；业务调用返回 token 无效类错误时 MUST 清除缓存、重新获取 token 并重试一次。配置页测试消息与正式推送 SHALL 共用该缓存。

#### Scenario: token 过期后再次发送
- **WHEN** 缓存中的企业微信 token 已超过有效期
- **THEN** 系统先重新获取 token 再发送消息，发送成功

#### Scenario: token 被平台提前作废
- **WHEN** 飞书发送消息接口返回 token 无效错误
- **THEN** 系统重新获取 token 并重试一次；重试仍失败则记录警告日志并放弃

### Requirement: 邮件渠道由运行期配置启用
系统 SHALL 只从运行期配置读取 SMTP 主机、端口、账号、密码、发件人与 SSL 选项，代码与默认配置 MUST NOT 内置任何邮件账号或密码。未启用或主机、发件人为空时，邮件渠道 SHALL 被跳过且不报错。

#### Scenario: 未配置邮件
- **WHEN** 运行期未配置邮件，接收人开启了 `mail.configSwitch`
- **THEN** 系统不发送邮件、不抛异常，其它渠道照常发送

#### Scenario: 已配置邮件
- **WHEN** 运行期已启用邮件并配置 SMTP，接收人开启了 `mail.configSwitch` 且有邮箱
- **THEN** 系统向该邮箱发送一封 HTML 邮件，主题为事件标题

### Requirement: 报告新建通知项目成员
系统 SHALL 在报告新建成功后，向项目内未锁定、且个人配置 `modules.report.event.new` 为真的成员发送通知，标题为「报告:<报告标题>」，链接指向该报告。

#### Scenario: 成员开启报告新建通知
- **WHEN** 项目成员 A 开启报告新建通知、成员 B 未开启，有人新建报告
- **THEN** 只有 A 收到通知

#### Scenario: 读取个人配置失败
- **WHEN** 某成员的 IM 配置无法解析
- **THEN** 该成员不接收此次报告通知，其它成员照常接收

### Requirement: 三种事件提供文本、Markdown、HTML 正文
系统 SHALL 为缺陷、通知、报告事件分别生成文本、Markdown、HTML 三种正文：钉钉群与飞书使用文本，钉钉个人与企业微信使用 Markdown，邮件使用 HTML；正文内容与 Spring 线对应模版一致（缺陷编号、名称、状态、处理人等字段）。

#### Scenario: 同一缺陷事件的多种格式
- **WHEN** 一次缺陷变更同时发往飞书群、企业微信个人和邮件
- **THEN** 飞书收到纯文本、企业微信收到 Markdown、邮件收到 HTML，三者包含相同的缺陷编号与名称
