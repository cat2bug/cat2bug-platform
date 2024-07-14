package com.cat2bug.im.service.impl;

import com.cat2bug.im.service.IIMService;
import com.cat2bug.im.domain.MailMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 12:41
 * @Version: 1.0.0
 */
@Service
public class MailMessageServiceImpl implements IIMService<MailMessage> {
    public final static String MESSAGE_FACTORY_NAME = "ImMailMessage";
    private final Logger logger = LogManager.getLogger(this.getClass());
    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendNoticeMessage(MailMessage message) {
        //获取MimeMessage对象
        MimeMessage mm = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper;
        try {
            messageHelper = new MimeMessageHelper(mm, true);
            //邮件发送人
            messageHelper.setFrom(message.getFrom());
            //邮件接收人,设置多个收件人地址
            InternetAddress[] internetAddressTo = InternetAddress.parse(message.getTo());
            messageHelper.setTo(internetAddressTo);
            //messageHelper.setTo(to);
            //邮件主题
            mm.setSubject(message.getTitle());
            //邮件内容，html格式
            messageHelper.setText(message.getContent(), true);
            //发送
            mailSender.send(mm);
            //日志信息
            logger.info("邮件已经发送。");
        } catch (Exception e) {
            logger.error("发送邮件时发生异常！", e);
        }
    }

    @Override
    public String getMessageFactoryName() {
        return MESSAGE_FACTORY_NAME;
    }

    @Override
    public void receiveMessage(MailMessage message) {

    }
}
