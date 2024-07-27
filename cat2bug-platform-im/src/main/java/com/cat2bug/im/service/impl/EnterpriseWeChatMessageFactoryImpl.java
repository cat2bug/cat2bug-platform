package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.*;
import com.cat2bug.im.service.IIMFactoryService;
import com.cat2bug.im.service.IIMProjectConfigService;
import com.cat2bug.im.service.IMessageTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-27 13:51
 * @Version: 1.0.0
 */
@Service(EnterpriseWeChatMessageServiceImpl.MESSAGE_FACTORY_NAME)
public class EnterpriseWeChatMessageFactoryImpl implements IIMFactoryService {
    private final static Logger log = LogManager.getLogger(EnterpriseWeChatMessageFactoryImpl.class);
    @Autowired
    private IIMProjectConfigService imProjectConfigService;

    @Override
    public <T> List<IMMessage> createMessage(Long projectId, String group, Long senderId, List<Long> recipientIds, String title, T content, String src, IMessageTemplate<T> messageTemplate, IMConfig config) {
        if(recipientIds==null) return new ArrayList<>();
        String text = messageTemplate.toHtml(content, config.getModules());
        if(StringUtils.isBlank(text)) return new ArrayList<>();

        // 获取企业微信在项目中的配置
        IMProjectConfig projectConfig = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                projectId,
                EnterpriseWeChatProjectConfig.SYSTEM_CODE);
        if(projectConfig==null) {
            log.warn("未在项目配置中找到企业微信配置信息! projectId={}", projectId);
            return new ArrayList<>();
        }
        EnterpriseWeChatProjectConfig weChatConfig = JSON.parseObject(projectConfig.getConfigParams(), EnterpriseWeChatProjectConfig.class);

        return recipientIds.stream().map(r->{
            TextEnterpriseWeChatAppMessage msg = new TextEnterpriseWeChatAppMessage(text);
            msg.setProjectId(projectId);
            msg.setSrc(src);
            msg.setProjectId(projectId);
            msg.setGroup(group);
            msg.setTitle(title);
            msg.setType(IMMessageType.MARKDOWN);
            msg.setAgentid(Integer.valueOf(weChatConfig.getAgentid()));
            msg.setCorpId(weChatConfig.getCorpId());
            msg.setCorpSecret(weChatConfig.getCorpSecret());
            msg.setContent(text);
            msg.setReceiveMemberId(r);
            return msg;
        }).collect(Collectors.toList());
    }
}
