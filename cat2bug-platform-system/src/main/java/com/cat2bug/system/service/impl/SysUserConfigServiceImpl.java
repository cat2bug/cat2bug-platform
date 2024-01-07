package com.cat2bug.system.service.impl;

import java.util.List;

import com.cat2bug.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysUserConfigMapper;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.service.ISysUserConfigService;

/**
 * 用户配置Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-16
 */
@Service
public class SysUserConfigServiceImpl implements ISysUserConfigService 
{
    @Autowired
    private SysUserConfigMapper sysUserConfigMapper;

    /**
     * 查询用户配置
     * 
     * @param userConfigId 用户配置主键
     * @return 用户配置
     */
    @Override
    public SysUserConfig selectSysUserConfigByUserConfigId(Long userConfigId)
    {
        return sysUserConfigMapper.selectSysUserConfigByUserConfigId(userConfigId);
    }

    @Override
    public SysUserConfig selectSysUserConfigByCurrentUserId() {
        return sysUserConfigMapper.selectSysUserConfigByUserId(SecurityUtils.getUserId());
    }

    @Override
    public SysUserConfig selectSysUserConfigByUserId(Long memberId) {
        return sysUserConfigMapper.selectSysUserConfigByUserId(memberId);
    }

    @Override
    public SysUserConfig selectSysUserConfigByUserName(String memberName) {
        return sysUserConfigMapper.selectSysUserConfigByUserName(memberName);
    }

    /**
     * 查询用户配置列表
     * 
     * @param sysUserConfig 用户配置
     * @return 用户配置
     */
    @Override
    public List<SysUserConfig> selectSysUserConfigList(SysUserConfig sysUserConfig)
    {
        return sysUserConfigMapper.selectSysUserConfigList(sysUserConfig);
    }

    /**
     * 新增用户配置
     * 
     * @param sysUserConfig 用户配置
     * @return 结果
     */
    @Override
    public int insertSysUserConfig(SysUserConfig sysUserConfig)
    {
        return sysUserConfigMapper.insertSysUserConfig(sysUserConfig);
    }

    /**
     * 修改用户配置
     * 
     * @param sysUserConfig 用户配置
     * @return 结果
     */
    @Override
    public int updateSysUserConfig(SysUserConfig sysUserConfig)
    {
        // 根据用户id查找配置
        SysUserConfig selectSysUserConfig = sysUserConfigMapper.selectSysUserConfigByUserId(SecurityUtils.getUserId());
        // 有就更新，没有新建
        if(selectSysUserConfig==null){
            sysUserConfig.setUserId(SecurityUtils.getUserId());
            return sysUserConfigMapper.insertSysUserConfig(sysUserConfig);
        } else {
            sysUserConfig.setUserConfigId(selectSysUserConfig.getUserConfigId());
            sysUserConfig.setUserId(selectSysUserConfig.getUserId());
            return sysUserConfigMapper.updateSysUserConfig(sysUserConfig);
        }
    }

    /**
     * 批量删除用户配置
     * 
     * @param userConfigIds 需要删除的用户配置主键
     * @return 结果
     */
    @Override
    public int deleteSysUserConfigByUserConfigIds(Long[] userConfigIds)
    {
        return sysUserConfigMapper.deleteSysUserConfigByUserConfigIds(userConfigIds);
    }

    /**
     * 删除用户配置信息
     * 
     * @param userConfigId 用户配置主键
     * @return 结果
     */
    @Override
    public int deleteSysUserConfigByUserConfigId(Long userConfigId)
    {
        return sysUserConfigMapper.deleteSysUserConfigByUserConfigId(userConfigId);
    }
}
