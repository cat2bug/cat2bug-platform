package com.cat2bug.system.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysModuleMapper;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.service.ISysModuleService;

/**
 * 模块Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-26
 */
@Service
public class SysModuleServiceImpl implements ISysModuleService 
{
    @Autowired
    private SysModuleMapper sysModuleMapper;

    @Override
    public Set<Long> getAllChildIds(Long moduleId) {
        return sysModuleMapper.getAllChildIds(moduleId);
    }

    /**
     * 查询模块
     * 
     * @param moduleId 模块主键
     * @return 模块
     */
    @Override
    public SysModule selectSysModuleByModuleId(Long moduleId)
    {
        return sysModuleMapper.selectSysModuleByModuleId(moduleId);
    }

    /**
     * 查询模块列表
     * 
     * @param sysModule 模块
     * @return 模块
     */
    @Override
    public List<SysModule> selectSysModuleList(SysModule sysModule)
    {
        return sysModuleMapper.selectSysModuleList(sysModule);
    }

    /**
     * 新增模块
     * 
     * @param sysModule 模块
     * @return 结果
     */
    @Override
    public int insertSysModule(SysModule sysModule)
    {
        return sysModuleMapper.insertSysModule(sysModule);
    }

    /**
     * 修改模块
     * 
     * @param sysModule 模块
     * @return 结果
     */
    @Override
    public int updateSysModule(SysModule sysModule)
    {
        return sysModuleMapper.updateSysModule(sysModule);
    }

    /**
     * 批量删除模块
     * 
     * @param moduleIds 需要删除的模块主键
     * @return 结果
     */
    @Override
    public int deleteSysModuleByModuleIds(Long[] moduleIds)
    {
        return sysModuleMapper.deleteSysModuleByModuleIds(moduleIds);
    }

    /**
     * 删除模块信息
     * 
     * @param moduleId 模块主键
     * @return 结果
     */
    @Override
    public int deleteSysModuleByModuleId(Long moduleId)
    {
        return sysModuleMapper.deleteSysModuleByModuleId(moduleId);
    }
}
