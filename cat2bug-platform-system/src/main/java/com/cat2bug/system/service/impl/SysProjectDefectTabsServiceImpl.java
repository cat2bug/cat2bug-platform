package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysProjectDefectTabsMapper;
import com.cat2bug.system.domain.SysProjectDefectTabs;
import com.cat2bug.system.service.ISysProjectDefectTabsService;

/**
 * 项目缺陷页签配置Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-04-06
 */
@Service
public class SysProjectDefectTabsServiceImpl implements ISysProjectDefectTabsService 
{
    @Autowired
    private SysProjectDefectTabsMapper sysProjectDefectTabsMapper;

    /**
     * 查询项目缺陷页签配置
     * 
     * @param tabId 项目缺陷页签配置主键
     * @return 项目缺陷页签配置
     */
    @Override
    public SysProjectDefectTabs selectSysProjectDefectTabsByTabId(Long tabId)
    {
        return sysProjectDefectTabsMapper.selectSysProjectDefectTabsByTabId(tabId);
    }

    /**
     * 查询项目缺陷页签配置列表
     * 
     * @param sysProjectDefectTabs 项目缺陷页签配置
     * @return 项目缺陷页签配置
     */
    @Override
    public List<SysProjectDefectTabs> selectSysProjectDefectTabsList(SysProjectDefectTabs sysProjectDefectTabs)
    {
        return sysProjectDefectTabsMapper.selectSysProjectDefectTabsList(sysProjectDefectTabs);
    }

    /**
     * 新增项目缺陷页签配置
     * 
     * @param sysProjectDefectTabs 项目缺陷页签配置
     * @return 结果
     */
    @Override
    public SysProjectDefectTabs insertSysProjectDefectTabs(SysProjectDefectTabs sysProjectDefectTabs)
    {
        sysProjectDefectTabs.setCreateTime(DateUtils.getNowDate());
        int count = sysProjectDefectTabsMapper.insertSysProjectDefectTabs(sysProjectDefectTabs);
        return count==1?sysProjectDefectTabs:null;
    }

    /**
     * 修改项目缺陷页签配置
     * 
     * @param sysProjectDefectTabs 项目缺陷页签配置
     * @return 结果
     */
    @Override
    public int updateSysProjectDefectTabs(SysProjectDefectTabs sysProjectDefectTabs)
    {
        return sysProjectDefectTabsMapper.updateSysProjectDefectTabs(sysProjectDefectTabs);
    }

    @Override
    public int updateSort(List<SysProjectDefectTabs> list) {
        return sysProjectDefectTabsMapper.updateSort(list);
    }

    /**
     * 批量删除项目缺陷页签配置
     * 
     * @param tabIds 需要删除的项目缺陷页签配置主键
     * @return 结果
     */
    @Override
    public int deleteSysProjectDefectTabsByTabIds(Long[] tabIds)
    {
        return sysProjectDefectTabsMapper.deleteSysProjectDefectTabsByTabIds(tabIds);
    }

    /**
     * 删除项目缺陷页签配置信息
     * 
     * @param tabId 项目缺陷页签配置主键
     * @return 结果
     */
    @Override
    public int deleteSysProjectDefectTabsByTabId(Long tabId)
    {
        return sysProjectDefectTabsMapper.deleteSysProjectDefectTabsByTabId(tabId);
    }

    @Override
    public int deleteSysProjectDefectTabsByProjectIdAndMemberId(Long projectId, Long memberId) {
        return sysProjectDefectTabsMapper.deleteSysProjectDefectTabsByProjectIdAndMemberId(projectId,memberId);
    }
}
