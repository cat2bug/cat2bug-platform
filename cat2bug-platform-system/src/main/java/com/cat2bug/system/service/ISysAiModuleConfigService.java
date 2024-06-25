package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysAiModuleConfig;

/**
 * AI模型配置Service接口
 * 
 * @author yuzhantao
 * @date 2024-06-20
 */
public interface ISysAiModuleConfigService 
{
    /**
     * 查询AI模型配置
     * 
     * @param aiId AI模型配置主键
     * @return AI模型配置
     */
    public SysAiModuleConfig selectSysAiModuleConfigByAiId(Long aiId);

    /**
     * 查询AI模型配置
     *
     * @param projectId 项目主键
     * @return AI模型配置
     */
    public SysAiModuleConfig selectSysAiModuleConfigByProjectId(Long projectId);

    /**
     * 查询AI模型配置列表
     * 
     * @param sysAiModuleConfig AI模型配置
     * @return AI模型配置集合
     */
    public List<SysAiModuleConfig> selectSysAiModuleConfigList(SysAiModuleConfig sysAiModuleConfig);

    /**
     * 新增AI模型配置
     * 
     * @param sysAiModuleConfig AI模型配置
     * @return 结果
     */
    public int insertSysAiModuleConfig(SysAiModuleConfig sysAiModuleConfig);

    /**
     * 修改AI模型配置
     * 
     * @param sysAiModuleConfig AI模型配置
     * @return 结果
     */
    public int updateSysAiModuleConfig(SysAiModuleConfig sysAiModuleConfig);

    /**
     * 批量删除AI模型配置
     * 
     * @param aiIds 需要删除的AI模型配置主键集合
     * @return 结果
     */
    public int deleteSysAiModuleConfigByAiIds(Long[] aiIds);

    /**
     * 删除AI模型配置信息
     * 
     * @param aiId AI模型配置主键
     * @return 结果
     */
    public int deleteSysAiModuleConfigByAiId(Long aiId);
}
