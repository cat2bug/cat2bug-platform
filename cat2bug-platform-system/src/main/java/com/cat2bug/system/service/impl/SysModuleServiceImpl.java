package com.cat2bug.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.cat2bug.common.utils.MessageUtils;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysModuleMapper;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.service.ISysModuleService;
import org.springframework.transaction.annotation.Transactional;

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
    public Set<Long> getAllChildIds(Long projectId, Long moduleId) {
        return sysModuleMapper.getAllChildIds(projectId, moduleId);
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

    @Override
    public List<SysModule> selectSysModulePathList(Long projectId) {
        return sysModuleMapper.selectSysModulePathList(projectId);
    }

    /**
     * 新增模块
     * 
     * @param sysModule 模块
     * @return 结果
     */
    @Override
    @Transactional
    public List<SysModule> insertSysModule(SysModule sysModule)
    {
        List<SysModule> ret = new ArrayList<>();
        if(sysModule.getBatchModuleNames()!=null && sysModule.getBatchModuleNames().size()>0) {
            sysModule.getBatchModuleNames().forEach(m->{
                SysModule module = new SysModule();
                module.setModuleName(m);
                module.setModulePid(sysModule.getModulePid());
                module.setProjectId(sysModule.getProjectId());
                int count = sysModuleMapper.insertSysModule(module);
                Preconditions.checkState(count>0, MessageUtils.message("module.insert-fail"));
                ret.add(module);
            });
        } else {
            int  count = sysModuleMapper.insertSysModule(sysModule);
            Preconditions.checkState(count>0, MessageUtils.message("module.insert-fail"));
            ret.add(sysModule);
        }
        return ret;
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
