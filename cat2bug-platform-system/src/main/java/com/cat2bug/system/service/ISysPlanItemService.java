package com.cat2bug.system.service;

import java.util.List;

import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysPlanItem;
import com.cat2bug.system.domain.SysPlanItemModule;

/**
 * 测试计划子项Service接口
 *
 * @author yuzhantao
 * @date 2024-10-11
 */
public interface ISysPlanItemService
{
    /**
     * 查询测试计划子项
     *
     * @param planItemId 测试计划子项主键
     * @return 测试计划子项
     */
    public SysPlanItem selectSysPlanItemByPlanItemId(String planItemId);

    /**
     * 查询测试计划子项列表
     *
     * @param sysPlanItem 测试计划子项
     * @return 测试计划子项集合
     */
    public List<SysPlanItem> selectSysPlanItemList(SysPlanItem sysPlanItem);

    /**
     * 查询测试计划中可选的测试用例
     * @param sysCase 查询的用例参数
     * @return  测试计划子项集合
     */
    public List<SysCase> selectCaseList(SysCase sysCase);

    /**
     * 查询模块列表
     *
     * @param sysModule 模块
     * @return 模块集合
     */
    public List<SysPlanItemModule> selectSysModuleList(SysPlanItemModule sysModule);

    /**
     * 新增测试计划子项
     *
     * @param sysPlanItem 测试计划子项
     * @return 结果
     */
    public int insertSysPlanItem(SysPlanItem sysPlanItem);

    /**
     * 修改测试计划子项
     *
     * @param sysPlanItem 测试计划子项
     * @return 结果
     */
    public int updateSysPlanItem(SysPlanItem sysPlanItem);

    /**
     * 批量删除测试计划子项
     *
     * @param planItemIds 需要删除的测试计划子项主键集合
     * @return 结果
     */
    public int deleteSysPlanItemByPlanItemIds(String[] planItemIds);

    /**
     * 删除测试计划子项信息
     *
     * @param planItemId 测试计划子项主键
     * @return 结果
     */
    public int deleteSysPlanItemByPlanItemId(String planItemId);
}
