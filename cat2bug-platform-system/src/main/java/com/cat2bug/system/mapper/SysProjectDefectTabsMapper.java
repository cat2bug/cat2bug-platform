package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysProjectDefectTabs;
import org.apache.ibatis.annotations.Param;

/**
 * 项目缺陷页签配置Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-04-06
 */
public interface SysProjectDefectTabsMapper 
{
    /**
     * 查询项目缺陷页签配置
     * 
     * @param tabId 项目缺陷页签配置主键
     * @return 项目缺陷页签配置
     */
    public SysProjectDefectTabs selectSysProjectDefectTabsByTabId(Long tabId);

    /**
     * 查询项目缺陷页签配置列表
     * 
     * @param sysProjectDefectTabs 项目缺陷页签配置
     * @return 项目缺陷页签配置集合
     */
    public List<SysProjectDefectTabs> selectSysProjectDefectTabsList(SysProjectDefectTabs sysProjectDefectTabs);

    /**
     * 新增项目缺陷页签配置
     * 
     * @param sysProjectDefectTabs 项目缺陷页签配置
     * @return 结果
     */
    public int insertSysProjectDefectTabs(SysProjectDefectTabs sysProjectDefectTabs);

    /**
     * 修改项目缺陷页签配置
     * 
     * @param sysProjectDefectTabs 项目缺陷页签配置
     * @return 结果
     */
    public int updateSysProjectDefectTabs(SysProjectDefectTabs sysProjectDefectTabs);

    /**
     * 更新标签集合的排序
     * @param list  标签集合
     * @return      更新数量
     */
    public int updateSort(List<SysProjectDefectTabs> list);

    /**
     * 删除项目缺陷页签配置
     * 
     * @param tabId 项目缺陷页签配置主键
     * @return 结果
     */
    public int deleteSysProjectDefectTabsByTabId(Long tabId);

    /**
     * 批量删除项目缺陷页签配置
     * 
     * @param tabIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysProjectDefectTabsByTabIds(Long[] tabIds);

    /**
     * 删除项目缺陷页签配置信息
     * @param projectId 项目ID
     * @param memberId  成员ID
     * @return  结果
     */
    public int deleteSysProjectDefectTabsByProjectIdAndMemberId(@Param("projectId") Long projectId, @Param("memberId") Long memberId);
}
