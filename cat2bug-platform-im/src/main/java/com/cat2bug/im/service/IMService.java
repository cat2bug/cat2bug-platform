package com.cat2bug.im.service;

import com.cat2bug.common.utils.spring.SpringUtils;
import com.cat2bug.common.utils.uuid.IdUtils;
import com.cat2bug.common.utils.uuid.UUID;
import com.cat2bug.im.domain.IMBasePlatformConfig;
import com.cat2bug.im.domain.IMConfig;
import com.cat2bug.im.domain.IMMessage;
import com.cat2bug.im.domain.IMUserConfig;
import com.cat2bug.im.mapper.IMUserConfigMapper;
import com.cat2bug.im.service.impl.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 01:49
 * @Version: 1.0.0
 */
@Service
public class IMService {
    private final static Logger log = LogManager.getLogger(IMMessage.class);

    private final static ExecutorService es = Executors.newFixedThreadPool(3);

    @Autowired
    IMUserConfigMapper userConfigMapper;

    @Autowired
    private DefaultMessageTemplateImpl defaultMessageTemplateImpl;

    @Autowired
    private List<IIMService> iimServiceList;
    /**
     * 发送文本消息
     * @param projectId     项目ID
     * @param senderId      发送者ID
     * @param recipientIds  接收人ID集合
     * @param content       发送内容
     * @param group         分组
     */
    public <T> void sendMessage(Long projectId, String group, Long senderId, List<Long> recipientIds, String title, T content, String src, IMessageTemplate messageTemplate) {
        String sn = IdUtils.simpleUUID(); // 流水号
        recipientIds.stream().forEach(recipientId->{
            IMUserConfig userConfig = userConfigMapper.selectImUserConfigByProjectAndMember(projectId,  recipientId);
            if(userConfig.getConfig().getPlatforms().getSystem().isConfigSwitch()){
                Optional<IIMService> opt = this.iimServiceList.stream().filter(s->s.getMessageFactoryName().equals(NoticeMessageServiceImpl.MESSAGE_FACTORY_NAME)).findFirst();
                if(opt.isPresent())
                    this.sendMessage(sn, opt.get(),userConfig.getConfig(),userConfig.getConfig().getPlatforms().getSystem(),projectId,group,senderId,recipientId,title,content,src,messageTemplate);
                opt = this.iimServiceList.stream().filter(s->s.getMessageFactoryName().equals(PanelNoticeMessageServiceImpl.MESSAGE_FACTORY_NAME)).findFirst();
                if(opt.isPresent())
                    this.sendMessage(sn, opt.get(),userConfig.getConfig(),userConfig.getConfig().getPlatforms().getSystem(),projectId,group,senderId,recipientId,title,content,src,messageTemplate);
            }
            if(userConfig.getConfig().getPlatforms().getMail().isConfigSwitch()){
                Optional<IIMService> opt = this.iimServiceList.stream().filter(s->s.getMessageFactoryName().equals(MailMessageServiceImpl.MESSAGE_FACTORY_NAME)).findFirst();
                if(opt.isPresent())
                    this.sendMessage(sn, opt.get(), userConfig.getConfig(),userConfig.getConfig().getPlatforms().getMail(), projectId,group,senderId,recipientId,title,content,src,messageTemplate);
            }
            if(userConfig.getConfig().getPlatforms().getDing().isConfigSwitch()){
                Optional<IIMService> opt = this.iimServiceList.stream().filter(s->s.getMessageFactoryName().equals(DingMessageServiceImpl.MESSAGE_FACTORY_NAME)).findFirst();
                if(opt.isPresent())
                    this.sendMessage(sn, opt.get(),userConfig.getConfig(),userConfig.getConfig().getPlatforms().getDing(), projectId,group,senderId,recipientId,title,content,src,messageTemplate);
            }
        });
    }

    private  <T> void sendMessage(String sn, IIMService im, IMConfig config, IMBasePlatformConfig platformConfig, Long projectId, String group, Long senderId, Long recipientId, String title, T content, String src, IMessageTemplate messageTemplate) {
        IIMFactoryService factory = SpringUtils.getBean(im.getMessageFactoryName());
        if(factory==null) {
            log.error("Bean 实例 {} 未找到，无法发送IM信息",im.getMessageFactoryName());
            return;
        }
        IMessageTemplate template = messageTemplate==null? defaultMessageTemplateImpl :messageTemplate;
        List<IMMessage> messageList = factory.createMessage(projectId,group,senderId,Arrays.asList(recipientId),title,content,src,template,config);

        messageList.forEach(m->{
            m.setSn(sn);
            es.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        im.sendNoticeMessage(m,platformConfig);
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            });
        });
    }
}
