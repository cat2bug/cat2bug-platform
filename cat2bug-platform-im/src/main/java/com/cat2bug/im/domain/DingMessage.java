package com.cat2bug.im.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

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
    private String appKey;
    @JsonIgnore
    private String appSecret;
    @JsonIgnore
    private String webHook;
    /**
     * 创建企业内部应用机器人后，获取机器人robotCode。详情步骤请参考机器人唯一标识。
     */
    private String robotCode;
    /**
     * 接收机器人消息的用户的userId列表，每次接收人不能超过20个。
     */
    private List<String> userIds;
    /**
     * DING消息类型。
     * 1：应用内DING
     * 2：短信DING
     * 3：电话DING
     */
    private String msgKey="sampleActionCard";

    /**
     * 消息模板参数，消息模板参数，详情参考企业机器人发送消息的消息类型
     */
    private String msgParam;

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
