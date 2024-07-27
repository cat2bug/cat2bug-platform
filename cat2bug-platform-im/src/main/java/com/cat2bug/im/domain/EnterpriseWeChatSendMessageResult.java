package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-27 13:22
 * @Version: 1.0.0
 * 企业微信发送消息的返回体
 */
@Data
public class EnterpriseWeChatSendMessageResult extends EnterpriseWeChatResult {
    /** 不合法的userid，不区分大小写，统一转为小写 */
    private String invaliduser;
    /** 不合法的partyid */
    private String invalidparty;
    /** 不合法的标签id */
    private String invalidtag;
    /** 没有基础接口许可(包含已过期)的userid */
    private String unlicenseduser;
    /** 消息id，用于撤回应用消息 */
    private String msgid;
    /** 仅消息类型为“按钮交互型”，“投票选择型”和“多项选择型”的模板卡片消息返回，应用可使用response_code调用更新模版卡片消息接口，72小时内有效，且只能使用一次 */
    private String response_code;
}
