package com.cat2bug.ai.vo;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2026-01-02 21:40
 * @Version: 1.0.0
 */
public class OpenAIResponse<T> {
    private List<Choice<T>> choices;

    public List<Choice<T>> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice<T>> choices) {
        this.choices = choices;
    }

    public static class Choice<T> {
        private Message<T> message;

        public Message<T> getMessage() {
            return message;
        }

        public void setMessage(Message<T> message) {
            this.message = message;
        }
    }

    public static class Message<T> {
        private String role;
        private T content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public T getContent() {
            return content;
        }

        public void setContent(T content) {
            this.content = content;
        }
    }
}
