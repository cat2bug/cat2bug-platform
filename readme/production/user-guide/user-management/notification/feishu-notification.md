# 飞书通知

## 概述

飞书通知允许用户通过飞书接收系统通知，支持企业个人推送和机器人群发两种方式。

## 企业个人推送

### 功能说明

- 将发送给当前用户的通知直接发送到个人飞书账号
- 适合接收个人相关的通知
- 需要企业飞书应用支持

### 前置条件

- 企业已开通飞书企业应用
- 用户已加入企业飞书组织
- 需要管理员在"项目设置"->"第三方应用"->"飞书"中进行配置

### 用户配置步骤

如果需要将通知信息单独发送给指定成员，需要配置到Cat2Bug-Platform个人通知配置中。

1. 进入Cat2Bug-Platform系统，点击右上角的通知图标，进入通知页面后选择右侧配置按钮，选择【接收平台】->【飞书】页面，开启"单发配置"开关。

2. 输入飞书中的用户手机号，点击"保存"按钮。

![飞书单发配置](../../../images/user-guide/user-management/notification/feishu/feishu_member_config.png)

3. 点击"单发测试"按钮，系统提示"测试消息发送成功"，并且在飞书客户端可以收到Cat2Bug-Platform发来的测试消息，表示配置成功。

::: tip 提示

配置企业个人推送还需要项目管理员配置飞书企业应用，详情请参考[用户指南->当前项目->项目设置->第三方应用->飞书](../../current-project/project-setting/project-third-party/feishu.md)

:::

## 机器人群发

### 功能说明

- 将发送给当前用户的通知发送到飞书群组
- 适合团队协作场景
- 通过飞书机器人向群组发送通知

### 前置条件

- 需要在飞书群中添加自定义机器人
- 需要配置机器人的 Webhook 地址

### 配置步骤

#### 创建飞书客户端群组机器人

1. 打开飞书客户端，点击群组右上角的设置按钮。

![创建机器人1](../../../images/user-guide/user-management/notification/feishu/feishu_group_1.png)

2. 选择群机器人选项进入“机器人管理”页面。

![创建机器人2](../../../images/user-guide/user-management/notification/feishu/feishu_group_2.png)

3. 在“机器人管理”页面中，点击“添加机器人”选项。

![创建机器人3](../../../images/user-guide/user-management/notification/feishu/feishu_group_3.png)

4. 在弹出的添加机器人选项框中选择“自定义机器人”。

![创建机器人4](../../../images/user-guide/user-management/notification/feishu/feishu_group_4.png)

5. 在“自定义机器人”中输入机器人名称后，点击“添加”按钮。

![创建机器人5](../../../images/user-guide/user-management/notification/feishu/feishu_group_5.png)

6. 勾选“签名校验”或“自定义关键词”（建议二选一即可）后，备份下“Webhook”和“签名校验”的值后，点击“完成”按钮。

![创建机器人6](../../../images/user-guide/user-management/notification/feishu/feishu_group_6.png)

#### 用户配置

1. 在Cat2Bug-Platform系统中，点击右上角的通知图标，进入通知页面后选择右侧配置按钮，选择【接收平台】->【飞书】配置页面，启动"群发配置"开关。

2. 将刚刚备份的“Webhook”和“签名校验”输入到配置项中，点击"保存"按钮。

![飞书群发配置](../../../images/user-guide/user-management/notification/feishu/feishu_group_config.png)

3. 点击"群发测试"按钮，系统提示"测试消息发送成功"，并且在飞书群组中可以收到机器人发来的测试消息，表示配置成功。

## 最佳实践

- 根据团队使用习惯选择飞书通知方式
- 个人通知使用企业个人推送
- 团队协作使用机器人群发，确保团队成员都能收到

## 常见问题

**Q: 为什么收不到飞书通知？**  
A: 请检查以下几点：
- 确认通知设置中已开启"飞书"
- 如果是单人发送，检查项目管理员是否已在"项目设置"->"第三方应用"->"飞书"中配置飞书企业应用
- 确认您已加入飞书组织
- 检查飞书开发者平台是否开启相应权限

**Q: 可以自定义飞书通知的内容吗？**  
A: 飞书通知的内容由系统自动生成，用户无法自定义。

**Q: 飞书通知会有延迟吗？**  
A: 飞书通知通常是实时推送的，但可能会因网络状况有轻微延迟。

**Q: 如何关闭飞书通知？**  
A: 在通知设置的接收平台中，取消"单发配置"或"群发配置"即可。
