# 第三方应用 [/project/ding](/project/ding)

## 概述

第三方应用供**项目管理员**集成钉钉、飞书、企业微信，将缺陷、报告等事件推送到 IM 平台。各平台配置页均为**左侧表单 + 右侧说明**，请按对应页面中的介绍与选项填写。

![项目设置页面](../../../images/user-guide/current-project/project-setting/01-project-setting-overview.png)

## 配置入口

**项目设置 → 第三方应用**，选择：

| 平台 | 文档 | 项目配置页 |
|------|------|------------|
| 钉钉 | [钉钉](project-third-party/ding.md) | [/project/ding](/project/ding) |
| 飞书 | [飞书](project-third-party/feishu.md) | [/project/feishu](/project/feishu) |
| 企业微信 | [企业微信](project-third-party/enterprise-wechat.md) | [/project/enterprise-wechat](/project/enterprise-wechat) |

详细步骤、表单项与截图见各平台文档（内容与系统配置页右侧说明一致）。

## 配置分工

1. **项目管理员**：在上述配置页保存企业应用凭证（钉钉 Client ID / Secret / Robot Code，飞书应用 ID / 密钥，企业微信 企业ID / 应用ID / 应用凭证）。
2. **成员**：在右上角 **通知 → 配置 → 接收平台** 中开启单发或群发，填写手机号、UserId、Webhook 等，并执行测试。

| 平台 | 项目级（管理员） | 个人/群（成员通知设置） |
|------|------------------|-------------------------|
| 钉钉 | 企业应用 + 机器人 | [钉钉通知](../../user-management/notification/dingtalk-notification.md) |
| 飞书 | 企业自建应用 | [飞书通知](../../user-management/notification/feishu-notification.md) |
| 企业微信 | 自建应用 + 可信 IP | [企业微信通知](../../user-management/notification/wecom-notification.md) |

## 权限说明

仅**项目管理员**可修改第三方应用的项目级配置。

## 常见问题

**Q: 三个平台可以同时用吗？**  
A: 可以。分别完成项目配置后，成员可按需在各自通知设置中启用。

**Q: 项目已保存但仍收不到消息？**  
A: 检查成员是否开启对应平台开关并完成单发/群发项；钉钉/企业微信单发还需成员账号或手机号正确；企业微信需配置**可信 IP**。

**Q: 只想发到群、不配企业应用？**  
A: 钉钉、飞书、企业微信均支持成员仅配置**群机器人 Webhook**（见各平台用户通知文档中的「群发」章节）。
