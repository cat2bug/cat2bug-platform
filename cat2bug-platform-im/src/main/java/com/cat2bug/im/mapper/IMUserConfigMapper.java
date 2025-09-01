package com.cat2bug.im.mapper;

import java.util.List;
import com.cat2bug.im.domain.IMUserConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户消息配置Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-07-18
 */
@Mapper
public interface IMUserConfigMapper
{
    /**
     * 查询用户消息配置
     *
     * @param projectId 项目ID
     * @param memberId 成员ID
     * @return 用户消息配置
     */
    public IMUserConfig selectImUserConfigByProjectAndMember(@Param("projectId") Long projectId, @Param("memberId") Long memberId);

    /**
     * 查询用户消息配置列表
     * 
     * @param imUserConfig 用户消息配置
     * @return 用户消息配置集合
     */
    public List<IMUserConfig> selectImUserConfigList(IMUserConfig imUserConfig);

    /**
     * 新增用户消息配置
     * 
     * @param imUserConfig 用户消息配置
     * @return 结果
     */
    public int insertImUserConfig(IMUserConfig imUserConfig);

    /**
     * 修改用户消息配置
     * 
     * @param imUserConfig 用户消息配置
     * @return 结果
     */
    public int updateImUserConfig(IMUserConfig imUserConfig);

    /**
     * 删除用户消息配置
     * 
     * @param imConfigId 用户消息配置主键
     * @return 结果
     */
    public int deleteImUserConfigByImConfigId(Long imConfigId);

    /**
     * 批量删除用户消息配置
     * 
     * @param imConfigIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteImUserConfigByImConfigIds(Long[] imConfigIds);
}
