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
import java.util.List;
import java.util.stream.Collectors;

@Service(FeishuAppMessageServiceImpl.MESSAGE_FACTORY_NAME)
public class FeishuAppMessageFactoryImpl implements IIMFactoryService {
    private static final Logger log = LogManager.getLogger(FeishuAppMessageFactoryImpl.class);

    @Autowired
    private IIMProjectConfigService imProjectConfigService;

    @Override
    public <T> List<IMMessage> createMessage(Long projectId, String group, Long senderId, List<Long> recipientIds,
                                             String title, T content, String src,
                                             IMessageTemplate<T> messageTemplate, IMConfig config) {
        if (recipientIds == null) return new ArrayList<>();
        String text = messageTemplate.toText(content, config.getModules());
        if (StringUtils.isBlank(text)) return new ArrayList<>();

        IMProjectConfig projectConfig = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                projectId, FeishuProjectConfig.SYSTEM_CODE);
        if (projectConfig == null || StringUtils.isBlank(projectConfig.getConfigParams())) {
            log.warn("未在项目配置中找到飞书企业应用配置信息 projectId={}", projectId);
            return new ArrayList<>();
        }
        FeishuProjectConfig feishuProjectConfig = JSON.parseObject(projectConfig.getConfigParams(), FeishuProjectConfig.class);
        if (StringUtils.isBlank(feishuProjectConfig.getAppId()) || StringUtils.isBlank(feishuProjectConfig.getAppSecret())) {
            log.warn("飞书企业应用 appId/appSecret 未配置 projectId={}", projectId);
            return new ArrayList<>();
        }
        FeishuPlatformConfig platformConfig = config.getPlatforms().getFeishu();
        if (platformConfig == null || StringUtils.isBlank(platformConfig.getMobile())) {
            return new ArrayList<>();
        }

        return recipientIds.stream().map(r -> {
            FeishuAppMessage msg = new FeishuAppMessage(null, text);
            msg.setProjectId(projectId);
            msg.setSrc(src);
            msg.setGroup(group);
            msg.setTitle(title);
            msg.setReceiveMemberId(r);
            msg.setAppId(feishuProjectConfig.getAppId());
            msg.setAppSecret(feishuProjectConfig.getAppSecret());
            return (IMMessage) msg;
        }).collect(Collectors.toList());
    }
}
