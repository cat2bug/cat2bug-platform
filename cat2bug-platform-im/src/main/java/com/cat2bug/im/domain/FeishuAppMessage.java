package com.cat2bug.im.domain;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 飞书企业应用单发消息
 */
@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
public class FeishuAppMessage extends IMMessage<String> {
    @JsonIgnore
    private String appId;

    @JsonIgnore
    private String appSecret;

    @JsonIgnore
    private Long projectId;

    @JsonIgnore
    private Long receiveMemberId;

    @JsonIgnore
    private String src;

    @JsonIgnore
    private String group;

    @JsonIgnore
    private String title;

    private String receive_id;

    private String msg_type = "text";

    public FeishuAppMessage(String receiveId, String text) {
        this.receive_id = receiveId;
        this.setContent(JSON.toJSONString(new Content(text)));
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
