package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysPlan;
import com.cat2bug.system.domain.SysPlanItem;

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
     * 查询测试计划列表
     * 
     * @param sysPlan 测试计划
     * @return 测试计划集合
     */
    public List<SysPlan> selectSysPlanList(SysPlan sysPlan);

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
