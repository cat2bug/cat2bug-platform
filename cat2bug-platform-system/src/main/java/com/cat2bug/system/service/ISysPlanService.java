package com.cat2bug.system.service;

import java.util.List;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.SecurityUtils;
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
     * @param planId  测试计划ID
     * @param sysDefect 缺陷
     * @return 缺陷集合
     */
    public List<SysDefect> selectSysDefectList(String planId, SysDefect sysDefect);

    /**
     * 新增测试计划
     * 
     * @param sysPlan 测试计划
     * @return 结果
     */
    public int insertSysPlan(SysPlan sysPlan);

    /**
     * 复制测试计划
     * @param planId
     * @return
     */
    public int copySysPlan(String planId);

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
