package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.im.*;
import com.cat2bug.quarkus.common.util.ImNoticeDefaults;
import com.cat2bug.quarkus.system.mapper.IMUserConfigMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ImUserConfigService {

    @Inject
    IMUserConfigMapper imUserConfigMapper;

    public IMUserConfig selectImUserConfigByProjectAndMember(Long projectId, Long memberId,
                                                             Map<String, Object> defaultOption) {
        IMUserConfig config = imUserConfigMapper.selectImUserConfigByProjectAndMember(projectId, memberId);
        if (config == null) {
            config = createDefaultConfig(projectId, memberId,
                    defaultOption != null ? defaultOption : new HashMap<>(), null);
        }
        return config;
    }

    public IMUserConfig selectImUserConfigByProjectAndMember(Long projectId, Long memberId,
                                                             Map<String, Object> defaultOption, LoginUser loginUser) {
        IMUserConfig config = imUserConfigMapper.selectImUserConfigByProjectAndMember(projectId, memberId);
        if (config == null) {
            config = createDefaultConfig(projectId, memberId,
                    defaultOption != null ? defaultOption : ImNoticeDefaults.defaultDefectNoticeOption(),
                    loginUser);
        }
        return config;
    }

    public int saveImUserConfig(IMUserConfig imUserConfig) {
        IMUserConfig existing = imUserConfigMapper.selectImUserConfigByProjectAndMember(
                imUserConfig.getProjectId(), imUserConfig.getUserId());
        if (existing == null) {
            return imUserConfigMapper.insertImUserConfig(imUserConfig);
        }
        imUserConfig.setImConfigId(existing.getImConfigId());
        return imUserConfigMapper.updateImUserConfig(imUserConfig);
    }

    public int deleteImUserConfigByImConfigId(Long imConfigId) {
        return imUserConfigMapper.deleteImUserConfigByImConfigId(imConfigId);
    }

    private IMUserConfig createDefaultConfig(Long projectId, Long memberId,
                                             Map<String, Object> defaultOption, LoginUser loginUser) {
        IMUserConfig config = new IMUserConfig();
        config.setProjectId(projectId);
        config.setUserId(memberId);

        IMConfig imConfig = new IMConfig();
        config.setConfig(imConfig);
        imConfig.setModules(defaultOption);

        IMPlatformConfig imPlatformConfig = new IMPlatformConfig();
        imConfig.setPlatforms(imPlatformConfig);

        imPlatformConfig.setSystem(new IMSystemPlatformConfig(true, true, "default.mp3", true));

        String email = loginUser != null && loginUser.getUser() != null ? loginUser.getUser().getEmail() : null;
        imPlatformConfig.setMail(new IMMailPlatformConfig(false, email));

        String phone = loginUser != null && loginUser.getUser() != null ? loginUser.getUser().getPhoneNumber() : null;
        imPlatformConfig.setDing(new IMDingPlatformConfig(false, false, false, null, null, null, null));
        EnterpriseWeChatPlatformConfig wechat = new EnterpriseWeChatPlatformConfig();
        wechat.setSingleSwitch(false);
        wechat.setGroupSwitch(false);
        wechat.setMobile(phone);
        imPlatformConfig.setWechat(wechat);
        imPlatformConfig.setFeishu(new FeishuPlatformConfig(false, false, false, phone, null, null, null));
        return config;
    }
}
