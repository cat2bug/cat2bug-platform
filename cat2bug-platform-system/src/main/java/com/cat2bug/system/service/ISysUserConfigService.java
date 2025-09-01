package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysUserConfig;

/**
 * 用户配置Service接口
 * 
 * @author yuzhantao
 * @date 2023-11-16
 */
public interface ISysUserConfigService 
{
    /**
     * 查询用户配置
     * 
     * @param userConfigId 用户配置主键
     * @return 用户配置
     */
    public SysUserConfig selectSysUserConfigByUserConfigId(Long userConfigId);

    /**
     * 查询用户配置
     * @return 用户配置
     */
    public SysUserConfig selectSysUserConfigByCurrentUserId();

    /**
     * 查询用户配置
     * @param memberId 成员id
     * @return 用户配置
     */
    public SysUserConfig selectSysUserConfigByUserId(Long memberId);

    /**
     * 查询用户配置
     * @param memberName 成员名称
     * @return 用户配置
     */
    public SysUserConfig selectSysUserConfigByUserName(String memberName);
    /**
     * 查询用户配置列表
     * 
     * @param sysUserConfig 用户配置
     * @return 用户配置集合
     */
    public List<SysUserConfig> selectSysUserConfigList(SysUserConfig sysUserConfig);

    /**
     * 新增用户配置
     * 
     * @param sysUserConfig 用户配置
     * @return 结果
     */
    public int insertSysUserConfig(SysUserConfig sysUserConfig);

    /**
     * 修改用户配置
     * 
     * @param sysUserConfig 用户配置
     * @return 结果
     */
    public int updateSysUserConfig(SysUserConfig sysUserConfig);

    /**
     * 批量删除用户配置
     * 
     * @param userConfigIds 需要删除的用户配置主键集合
     * @return 结果
     */
    public int deleteSysUserConfigByUserConfigIds(Long[] userConfigIds);

    /**
     * 删除用户配置信息
     * 
     * @param userConfigId 用户配置主键
     * @return 结果
     */
    public int deleteSysUserConfigByUserConfigId(Long userConfigId);
}
