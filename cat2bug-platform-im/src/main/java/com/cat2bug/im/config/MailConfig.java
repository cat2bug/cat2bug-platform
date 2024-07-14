package com.cat2bug.im.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Enumeration;
import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 13:08
 * @Version: 1.0.0
 * 邮件配置
 */
//@Configuration
//@ConfigurationProperties(prefix = "cat2bug.mail")
@Data
public class MailConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String from;

//    properties:
//    mail.smtp.socketFactory.fallback : true
//    mail.smtp.starttls.enable: true

//    @Bean
    public JavaMailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(this.getHost());
        mailSender.setPort(this.getPort());
        mailSender.setUsername(this.getUsername());
        mailSender.setPassword(this.getPassword());
        return mailSender;
    }
}
