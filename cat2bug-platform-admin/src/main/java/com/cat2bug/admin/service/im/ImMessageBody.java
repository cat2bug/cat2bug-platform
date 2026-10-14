package com.cat2bug.admin.service.im;

/**
 * 同一事件的三种正文：钉钉群 / 飞书用文本，站内通知 / 钉钉个人 / 企业微信用 Markdown，邮件用 HTML。
 */
public record ImMessageBody(String text, String markdown, String html) {
}
