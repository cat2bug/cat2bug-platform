package com.cat2bug.im.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-08 00:15
 * @Version: 1.0.0
 * 钉钉消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DingMessage extends IMMessage<String> {
    private String msgtype;
    private Text text;
    @JsonIgnore
    private String webHook;

    public DingMessage(String text) {
        Text t = new DingMessage.Text();
        t.setContent(text);
        this.text = t;
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
