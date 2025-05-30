package com.cat2bug.system.mapper;

import java.util.Date;
import java.util.List;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.system.domain.SysPlan;
import com.cat2bug.system.domain.SysPlanItem;
import org.apache.ibatis.annotations.Param;

/**
 * 测试计划Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-10-11
 */
public interface SysPlanMapper 
{
    /**
     * 查询测试计划
     * 
     * @param planId 测试计划主键
     * @return 测试计划
     */
    public SysPlan selectSysPlanByPlanId(String planId);

    /**
     * 获取测试计划最大数量
     * @param projectId 项目ID
     * @return  数值
     */
    public long getProjectPlanMaxNum(Long projectId);
    /**
     * 查询测试计划列表
     * 
     * @param sysPlan 测试计划
     * @return 测试计划集合
     */
    public List<SysPlan> selectSysPlanList(SysPlan sysPlan);

    /**
     * 查询缺陷列表
     *
     * @param sysDefect 缺陷
     * @return 缺陷集合
     */
    public List<SysDefect> selectSysDefectList(@Param("planId") String planId, @Param("defect") SysDefect sysDefect, @Param("currentUserId") Long currentUserId, @Param("currentTime") Date currentTime);


    /**
     * 新增测试计划
     * 
     * @param sysPlan 测试计划
     * @return 结果
     */
    public int insertSysPlan(SysPlan sysPlan);

    /**
     * 修改测试计划
     * 
     * @param sysPlan 测试计划
     * @return 结果
     */
    public int updateSysPlan(SysPlan sysPlan);

    /**
     * 更新测试计划日期
     *
     * @param sysPlan 测试计划
     * @return 结果
     */
    public int updateUpdateTimeOfSysPlan(SysPlan sysPlan);

    /**
     * 删除测试计划
     * 
     * @param planId 测试计划主键
     * @return 结果
     */
    public int deleteSysPlanByPlanId(String planId);

    /**
     * 批量删除测试计划
     * 
     * @param planIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysPlanByPlanIds(String[] planIds);

    /**
     * 批量删除测试计划子项
     * 
     * @param planIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysPlanItemByPlanIds(String[] planIds);
    
    /**
     * 批量新增测试计划子项
     * 
     * @param sysPlanItemList 测试计划子项列表
     * @return 结果
     */
    public int batchSysPlanItem(List<SysPlanItem> sysPlanItemList);
    

    /**
     * 通过测试计划主键删除测试计划子项信息
     * 
     * @param planId 测试计划ID
     * @return 结果
     */
    public int deleteSysPlanItemByPlanId(String planId);
}
