package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysPlan;

/**
 * 测试计划Service接口
 * 
 * @author yuzhantao
 * @date 2024-10-11
 */
public interface ISysPlanService 
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
     * 批量删除测试计划
     * 
     * @param planIds 需要删除的测试计划主键集合
     * @return 结果
     */
    public int deleteSysPlanByPlanIds(String[] planIds);

    /**
     * 删除测试计划信息
     * 
     * @param planId 测试计划主键
     * @return 结果
     */
    public int deleteSysPlanByPlanId(String planId);
}
