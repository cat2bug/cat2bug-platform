package com.cat2bug.im.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:31
 * @Version: 1.0.0
 * 飞书自定义机器人消息（Webhook）
 * 参考：https://open.feishu.cn/document/client-docs/bot-v3/add-custom-bot
 */
@Data
@NoArgsConstructor
public class FeishuMessage extends IMMessage<FeishuMessage.Content> {
    /**
     * 消息类型：text / post / interactive / image / share_chat
     */
    private String msg_type = "text";

    /** Webhook 地址（仅内部使用，不序列化到请求体） */
    @JsonIgnore
    private String hook;

    /** 签名密钥（仅内部使用，不序列化到请求体） */
    @JsonIgnore
    private String secret;

    public FeishuMessage(String text) {
        this.setContent(new Content(text));
    }

    @Data
    @NoArgsConstructor
    public static class Content {
        private String text;

        public Content(String text) {
            this.text = text;
        }
    }
}
