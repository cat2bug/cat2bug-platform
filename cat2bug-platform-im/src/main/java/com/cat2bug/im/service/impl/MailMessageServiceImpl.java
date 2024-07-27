package com.cat2bug.im.service.impl;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.IMMailPlatformConfig;
import com.cat2bug.im.domain.MailMessage;
import com.cat2bug.im.service.IIMService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 12:41
 * @Version: 1.0.0
 */
@Service
public class MailMessageServiceImpl implements IIMService<MailMessage, IMMailPlatformConfig> {
    public final static String MESSAGE_FACTORY_NAME = "ImMailMessage";
    private final Logger logger = LogManager.getLogger(this.getClass());
    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendNoticeMessage(MailMessage message, IMMailPlatformConfig config) {
        //获取MimeMessage对象

//        Properties prop = System.getProperties();
//        prop.put("mail.smtp.host", "mail.cat2bug.com");
//        prop.put("mail.smtp.host", "mail.cat2bug.com");
//        prop.put("mail.smtp.user", "dev@cat2bug.com");
//        prop.put("mail.smtp.port",587);
//        prop.put("mail.smtp.starttls.enable","true");
//        prop.put("mail.smtp.debug", true);
//        prop.put("mail.smtp.auth", true);
//        Session session = Session.getDefaultInstance(prop);
//        Message mm = new MimeMessage(session);
        MimeMessage mm = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper;
        try {
            messageHelper = new MimeMessageHelper(mm, true);
            //邮件发送人
            if(StringUtils.isNotBlank(config.getReceiver())) {
                messageHelper.setTo(config.getReceiver());
            } else {
                messageHelper.setTo(message.getTo());
            }
            //邮件接收人,设置多个收件人地址
            InternetAddress[] internetAddressTo = InternetAddress.parse(message.getTo());
            messageHelper.setTo(internetAddressTo);
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
