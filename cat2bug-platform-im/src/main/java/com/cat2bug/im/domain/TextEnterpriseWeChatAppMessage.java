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
     ** markdown 类型消息
     */
    public final static String MARKDOWN_MSG_TYPE = "markdown";
    private Markdown markdown;

    public TextEnterpriseWeChatAppMessage(String text) {
        super.setMsgtype(MARKDOWN_MSG_TYPE);
        this.markdown = new Markdown();
        this.markdown.setContent(text);
    }

    public static class Markdown {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
