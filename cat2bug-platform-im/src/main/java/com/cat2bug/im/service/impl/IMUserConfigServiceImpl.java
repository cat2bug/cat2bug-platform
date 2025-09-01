package com.cat2bug.im.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.im.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.im.mapper.IMUserConfigMapper;
import com.cat2bug.im.service.IIMUserConfigService;

/**
 * 用户消息配置Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-07-18
 */
@Service
public class IMUserConfigServiceImpl implements IIMUserConfigService
{
    @Autowired
    private IMUserConfigMapper imUserConfigMapper;

    /**
     * 查询用户消息配置
     * 
     * @param projectId 项目ID
     * @param memberId 成员ID
     * @return 用户消息配置
     */
    @Override
    public IMUserConfig selectImUserConfigByProjectAndMember(Long projectId,Long memberId, Map<String, Object> defaultOption)
    {
        IMUserConfig config = imUserConfigMapper.selectImUserConfigByProjectAndMember(projectId, memberId);
        if(config==null) {
            if(defaultOption!=null) {
                config = this.createDefaultConfig(projectId, memberId, defaultOption);
            }   else  {
                config = this.createDefaultConfig(projectId, memberId, new HashMap<>());
            }
        }
        return config;
    }

    private IMUserConfig createDefaultConfig(Long projectId,Long memberId, Map<String, Object> defaultOption) {
        IMUserConfig config = new IMUserConfig();

        config.setProjectId(projectId);
        config.setUserId(memberId);
        // 设置配置
        IMConfig imConfig = new IMConfig();
        config.setConfig(imConfig);
        imConfig.setModules(defaultOption);

        IMPlatformConfig imPlatformConfig = new IMPlatformConfig();
        imConfig.setPlatforms(imPlatformConfig);
        // 设置系统配置
        IMSystemPlatformConfig imSystemPlatformConfig = new IMSystemPlatformConfig(true,true, "default.mp3",true);
        imPlatformConfig.setSystem(imSystemPlatformConfig);
        // 设置邮件配置
        IMMailPlatformConfig imMailPlatformConfig = new IMMailPlatformConfig(false, SecurityUtils.getLoginUser().getUser().getEmail());
        imPlatformConfig.setMail(imMailPlatformConfig);
        // 设置钉钉配置
        IMDingPlatformConfig imDingPlatformConfig = new IMDingPlatformConfig(false,null,null,SecurityUtils.getLoginUser().getUser().getDingUserId());
        imPlatformConfig.setDing(imDingPlatformConfig);
        // 设置企业微信
        EnterpriseWeChatPlatformConfig enterpriseWeChatPlatformConfig = new EnterpriseWeChatPlatformConfig();
        enterpriseWeChatPlatformConfig.setUserId(SecurityUtils.getLoginUser().getUser().getWechatUserId());
        imPlatformConfig.setWechat(enterpriseWeChatPlatformConfig);
        return config;
    }

    /**
     * 新增用户消息配置
     * 
     * @param imUserConfig 用户消息配置
     * @return 结果
     */
    @Override
    public int saveImUserConfig(IMUserConfig imUserConfig)
    {
        IMUserConfig config = imUserConfigMapper.selectImUserConfigByProjectAndMember(imUserConfig.getProjectId(), imUserConfig.getUserId());
        if(config==null) {
            return imUserConfigMapper.insertImUserConfig(imUserConfig);
        } else {
            return imUserConfigMapper.updateImUserConfig(imUserConfig);
        }
    }

    /**
     * 删除用户消息配置信息
     * 
     * @param imConfigId 用户消息配置主键
     * @return 结果
     */
    @Override
    public int deleteImUserConfigByImConfigId(Long imConfigId)
    {
        return imUserConfigMapper.deleteImUserConfigByImConfigId(imConfigId);
    }
}
