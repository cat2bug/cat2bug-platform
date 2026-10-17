## MODIFIED Requirements

### Requirement: 通知事件按接收人配置分发到各渠道
系统 SHALL 在缺陷变更、通知发送两种事件发生时，按每个接收人在该项目下的个人 IM 配置决定渠道：`system.configSwitch` 开启发站内通知与面板弹窗；`mail.configSwitch` 开启且用户有邮箱时发邮件；钉钉、飞书、企业微信的 `singleSwitch` 开启且 `mobile` 非空、且项目有对应企业应用配置时发个人消息。系统 MUST NOT 因报告新建发送通知。

#### Scenario: 只开站内通知
- **WHEN** 接收人只开启 `system.configSwitch`，其它渠道开关均关闭
- **THEN** 系统只写入站内通知并推送 WebSocket，不调用任何外部平台

#### Scenario: 开启钉钉个人消息
- **WHEN** 接收人开启钉钉 `singleSwitch` 且填写了手机号，项目已配置钉钉 `appKey`、`appSecret`、`robotCode`
- **THEN** 系统按手机号查询钉钉 userId，并通过企业内部机器人向该用户发送一条消息，消息包含事件标题、Markdown 正文和「点击查看详情」链接

#### Scenario: 项目缺少企业应用配置
- **WHEN** 接收人开启飞书 `singleSwitch` 且填写了手机号，但项目没有飞书 `appId` / `appSecret`
- **THEN** 系统跳过该接收人的飞书个人消息，记录一条警告日志，其它渠道照常发送

## REMOVED Requirements

### Requirement: 报告新建通知项目成员
**Reason**: 产品不再提供报告，没有新建报告事件。
**Migration**: 个人配置中的 `modules.report` 不再被读取。不补发历史通知。

### Requirement: 三种事件提供文本、Markdown、HTML 正文
**Reason**: 报告事件已不存在，正文模版只保留缺陷与通知。
**Migration**: 由新增要求「缺陷与通知事件提供文本、Markdown、HTML 正文」取代。

## ADDED Requirements

### Requirement: 缺陷与通知事件提供文本、Markdown、HTML 正文
系统 SHALL 为缺陷、通知事件分别生成文本、Markdown、HTML 三种正文：钉钉群与飞书使用文本，钉钉个人与企业微信使用 Markdown，邮件使用 HTML；正文内容与既有模版一致（缺陷编号、名称、状态、处理人等字段）。系统 MUST NOT 再生成报告事件正文。

#### Scenario: 同一缺陷事件的多种格式
- **WHEN** 一次缺陷变更同时发往飞书群、企业微信个人和邮件
- **THEN** 飞书收到纯文本、企业微信收到 Markdown、邮件收到 HTML，三者包含相同的缺陷编号与名称
