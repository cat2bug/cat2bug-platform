package com.cat2bug.im.service;

import java.util.List;
import com.cat2bug.im.domain.IMUserConfig;

/**
 * 用户消息配置Service接口
 * 
 * @author yuzhantao
 * @date 2024-07-18
 */
public interface IIMUserConfigService
{
    /**
     * 查询用户消息配置
     * 
     * @param project 项目ID
     * @param memberId 成员ID
     * @return 用户消息配置
     */
    public IMUserConfig selectImUserConfigByProjectAndMember(Long project,Long memberId);

    /**
     * 新增用户消息配置
     * 
     * @param imUserConfig 用户消息配置
     * @return 结果
     */
    public int saveImUserConfig(IMUserConfig imUserConfig);

    /**
     * 删除用户消息配置信息
     *
     * @param imConfigId 用户消息配置主键
     * @return 结果
     */
    public int deleteImUserConfigByImConfigId(Long imConfigId);
}
