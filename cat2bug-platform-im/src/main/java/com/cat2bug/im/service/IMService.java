package com.cat2bug.im.service;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.spring.SpringUtils;
import com.cat2bug.common.utils.uuid.IdUtils;
import com.cat2bug.im.domain.EnterpriseWeChatPlatformConfig;
import com.cat2bug.im.domain.FeishuPlatformConfig;
import com.cat2bug.im.domain.IMBasePlatformConfig;
import com.cat2bug.im.domain.IMConfig;
import com.cat2bug.im.domain.IMDingPlatformConfig;
import com.cat2bug.im.domain.IMMessage;
import com.cat2bug.im.domain.IMUserConfig;
import com.cat2bug.im.service.impl.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    IIMUserConfigService imUserConfigService;

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
    public <T> void sendMessage(Long projectId, String group, Long senderId, List<Long> recipientIds, String title, T content, String src, IMessageTemplate messageTemplate, Map<String,Object> defaultOption) {
        String sn = IdUtils.simpleUUID(); // 流水号

        // 飞书群机器人：项目级配置，每次事件只发一条消息到群，无需按用户循环
        Optional<IIMService> feishuOpt = this.iimServiceList.stream().filter(s->s.getMessageFactoryName().equals(FeishuMessageServiceImpl.MESSAGE_FACTORY_NAME)).findFirst();
        if(feishuOpt.isPresent()) {
            if(!recipientIds.isEmpty()) {
                IMUserConfig anyUserConfig = imUserConfigService.selectImUserConfigByProjectAndMember(projectId, recipientIds.get(0), defaultOption);
                FeishuPlatformConfig feishuPlatform = anyUserConfig.getConfig().getPlatforms().getFeishu();
                Boolean feishuGroupSwitch = feishuPlatform != null ? feishuPlatform.getGroupSwitch() : false;
                if(feishuPlatform != null
                    && Boolean.TRUE.equals(feishuGroupSwitch)
                    && StringUtils.isNotBlank(feishuPlatform.getHook())) {
                    FeishuPlatformConfig groupConfig = new FeishuPlatformConfig();
                    groupConfig.setSingleSwitch(false);
                    groupConfig.setGroupSwitch(true);
                    groupConfig.setHook(feishuPlatform.getHook());
                    groupConfig.setSecret(feishuPlatform.getSecret());
                    groupConfig.setKey(feishuPlatform.getKey());
                    this.sendMessage(sn, feishuOpt.get(), anyUserConfig.getConfig(), groupConfig, projectId, group, senderId, recipientIds.get(0), title, content, src, messageTemplate);
                }
            }
        }

        // 飞书企业应用单发：按用户逐个发送
        Optional<IIMService> feishuAppOpt = this.iimServiceList.stream().filter(s->s.getMessageFactoryName().equals(FeishuAppMessageServiceImpl.MESSAGE_FACTORY_NAME)).findFirst();
        Optional<IIMService> wechatOpt = this.iimServiceList.stream().filter(s->s.getMessageFactoryName().equals(EnterpriseWeChatMessageServiceImpl.MESSAGE_FACTORY_NAME)).findFirst();

        // 企业微信群发 webhook：只发送一次
        if(wechatOpt.isPresent() && !recipientIds.isEmpty()) {
            IMUserConfig firstWechatConfig = imUserConfigService.selectImUserConfigByProjectAndMember(projectId, recipientIds.get(0), defaultOption);
            EnterpriseWeChatPlatformConfig wechatPlatform = firstWechatConfig.getConfig().getPlatforms().getWechat();
            Boolean wechatGroupSwitch = wechatPlatform.getGroupSwitch();
            if(Boolean.TRUE.equals(wechatGroupSwitch)
                    && StringUtils.isNotBlank(wechatPlatform.getHook())) {
                EnterpriseWeChatPlatformConfig groupConfig = new EnterpriseWeChatPlatformConfig();
                groupConfig.setGroupSwitch(true);
                groupConfig.setSingleSwitch(false);
                groupConfig.setHook(wechatPlatform.getHook());
                groupConfig.setMobile(wechatPlatform.getMobile());
                this.sendMessage(sn, wechatOpt.get(), firstWechatConfig.getConfig(), groupConfig, projectId, group, senderId, recipientIds.get(0), title, content, src, messageTemplate);
            }
        }

        recipientIds.stream().forEach(recipientId->{
           IMUserConfig userConfig = imUserConfigService.selectImUserConfigByProjectAndMember(projectId,  recipientId, defaultOption);
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
            IMDingPlatformConfig dingPlatform = userConfig.getConfig().getPlatforms().getDing();
            if(dingPlatform != null){
                Optional<IIMService> opt = this.iimServiceList.stream().filter(s->s.getMessageFactoryName().equals(DingMessageServiceImpl.MESSAGE_FACTORY_NAME)).findFirst();
                if(opt.isPresent()) {
                    Boolean singleSwitch = dingPlatform.getSingleSwitch();
                    Boolean groupSwitch = dingPlatform.getGroupSwitch();
                    if(Boolean.TRUE.equals(singleSwitch) || Boolean.TRUE.equals(groupSwitch)) {
                        IMDingPlatformConfig sendConfig = new IMDingPlatformConfig();
                        sendConfig.setSingleSwitch(Boolean.TRUE.equals(singleSwitch));
                        sendConfig.setGroupSwitch(Boolean.TRUE.equals(groupSwitch));
                        sendConfig.setMobile(sendConfig.getSingleSwitch() ? dingPlatform.getMobile() : null);
                        sendConfig.setHook(sendConfig.getGroupSwitch() ? dingPlatform.getHook() : null);
                        sendConfig.setSecret(sendConfig.getGroupSwitch() ? dingPlatform.getSecret() : null);
                        sendConfig.setKey(sendConfig.getGroupSwitch() ? dingPlatform.getKey() : null);
                        this.sendMessage(sn, opt.get(),userConfig.getConfig(),sendConfig, projectId,group,senderId,recipientId,title,content,src,messageTemplate);
                    }
                }
            }
            EnterpriseWeChatPlatformConfig wechatPlatform = userConfig.getConfig().getPlatforms().getWechat();
            if(wechatPlatform != null){
                Optional<IIMService> opt = wechatOpt;
                Boolean wechatSingleSwitch = wechatPlatform.getSingleSwitch();
                // 循环内只处理单发（mobile）
                if(opt.isPresent() && Boolean.TRUE.equals(wechatSingleSwitch) && StringUtils.isNotBlank(wechatPlatform.getMobile())) {
                    EnterpriseWeChatPlatformConfig singleConfig = new EnterpriseWeChatPlatformConfig();
                    singleConfig.setSingleSwitch(true);
                    singleConfig.setGroupSwitch(false);
                    singleConfig.setMobile(wechatPlatform.getMobile());
                    this.sendMessage(sn, opt.get(),userConfig.getConfig(),singleConfig, projectId,group,senderId,recipientId,title,content,src,messageTemplate);
                }
            }
            if(feishuAppOpt.isPresent() && userConfig.getConfig().getPlatforms().getFeishu() != null) {
                FeishuPlatformConfig feishuPlatform = userConfig.getConfig().getPlatforms().getFeishu();
                Boolean feishuSingleSwitch = feishuPlatform.getSingleSwitch();
                if(Boolean.TRUE.equals(feishuSingleSwitch) && StringUtils.isNotBlank(feishuPlatform.getMobile())) {
                    FeishuPlatformConfig singleConfig = new FeishuPlatformConfig();
                    singleConfig.setSingleSwitch(true);
                    singleConfig.setGroupSwitch(false);
                    singleConfig.setMobile(feishuPlatform.getMobile());
                    this.sendMessage(sn, feishuAppOpt.get(), userConfig.getConfig(), singleConfig, projectId, group, senderId, recipientId, title, content, src, messageTemplate);
                }
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
