package com.cat2bug.ai.vo;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2026-01-03 10:28
 * @Version: 1.0.0
 */
@Data
@AllArgsConstructor
public class OpenAIRequest {
    public final static String ROLE_SYSTEM = "system";
    public final static String ROLE_USER = "user";
    public final static String ROLE_ASSISTANT = "assistant";
    private String model;
    private List<Message> messages;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    @Data
    @AllArgsConstructor
    public static class Message implements Serializable {
        private String role;
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
