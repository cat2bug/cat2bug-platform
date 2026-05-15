package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysAiModuleConfigMapper;
import com.cat2bug.system.domain.SysAiModuleConfig;
import com.cat2bug.system.service.ISysAiModuleConfigService;

/**
 * AI模型配置Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-06-20
 */
@Service
public class SysAiModuleConfigServiceImpl implements ISysAiModuleConfigService 
{
    @Autowired
    private SysAiModuleConfigMapper sysAiModuleConfigMapper;

    /**
     * 查询AI模型配置
     * 
     * @param aiId AI模型配置主键
     * @return AI模型配置
     */
    @Override
    public SysAiModuleConfig selectSysAiModuleConfigByAiId(Long aiId)
    {
        return sysAiModuleConfigMapper.selectSysAiModuleConfigByAiId(aiId);
    }

    /**
     * 查询AI模型配置
     *
     * @param projectId 项目主键
     * @return AI模型配置
     */
    @Override
    public SysAiModuleConfig selectSysAiModuleConfigByProjectId(Long projectId) {
        return sysAiModuleConfigMapper.selectSysAiModuleConfigByProjectId(projectId);
    }

    /**
     * 查询AI模型配置列表
     * 
     * @param sysAiModuleConfig AI模型配置
     * @return AI模型配置
     */
    @Override
    public List<SysAiModuleConfig> selectSysAiModuleConfigList(SysAiModuleConfig sysAiModuleConfig)
    {
        return sysAiModuleConfigMapper.selectSysAiModuleConfigList(sysAiModuleConfig);
    }

    /**
     * 新增AI模型配置
     * 
     * @param sysAiModuleConfig AI模型配置
     * @return 结果
     */
    @Override
    public int insertSysAiModuleConfig(SysAiModuleConfig sysAiModuleConfig)
    {
        sysAiModuleConfig.setCreateTime(DateUtils.getNowDate());
        return sysAiModuleConfigMapper.insertSysAiModuleConfig(sysAiModuleConfig);
    }

    /**
     * 修改AI模型配置
     * 
     * @param sysAiModuleConfig AI模型配置
     * @return 结果
     */
    @Override
    public int updateSysAiModuleConfig(SysAiModuleConfig sysAiModuleConfig)
    {
        SysAiModuleConfig oldSysAiModuleConfig = sysAiModuleConfigMapper.selectSysAiModuleConfigByProjectId(sysAiModuleConfig.getProjectId());
        if(oldSysAiModuleConfig==null) {
            sysAiModuleConfig.setCreateTime(DateUtils.getNowDate());
            sysAiModuleConfig.setCreateById(SecurityUtils.getUserId());
            sysAiModuleConfig.setUpdateTime(DateUtils.getNowDate());
            sysAiModuleConfig.setUpdateById(SecurityUtils.getUserId());
            return sysAiModuleConfigMapper.insertSysAiModuleConfig(sysAiModuleConfig);
        } else {
            oldSysAiModuleConfig.setUpdateById(SecurityUtils.getUserId());
            oldSysAiModuleConfig.setUpdateTime(DateUtils.getNowDate());
            return sysAiModuleConfigMapper.updateSysAiModuleConfig(oldSysAiModuleConfig);
        }

    }

    /**
     * 批量删除AI模型配置
     * 
     * @param aiIds 需要删除的AI模型配置主键
     * @return 结果
     */
    @Override
    public int deleteSysAiModuleConfigByAiIds(Long[] aiIds)
    {
        return sysAiModuleConfigMapper.deleteSysAiModuleConfigByAiIds(aiIds);
    }

    /**
     * 删除AI模型配置信息
     * 
     * @param aiId AI模型配置主键
     * @return 结果
     */
    @Override
    public int deleteSysAiModuleConfigByAiId(Long aiId)
    {
        return sysAiModuleConfigMapper.deleteSysAiModuleConfigByAiId(aiId);
    }
}
