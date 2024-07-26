package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-26 21:15
 * @Version: 1.0.0
 */
@Data
public class TextEnterpriseWeChatAppMessage extends EnterpriseWeChatAppMessage {
    /**
     ** 文本类型消息
     */
    public final static String TEXT_MSG_TYPE = "text";
    private Text text;

    public TextEnterpriseWeChatAppMessage(String text) {
        super.setMsgtype(TEXT_MSG_TYPE);
        this.text = new Text();
        this.text.setContent(text);
    }

    public static class Text {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
