package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.system.domain.SysCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysPlanItemMapper;
import com.cat2bug.system.domain.SysPlanItem;
import com.cat2bug.system.service.ISysPlanItemService;

/**
 * 测试计划子项Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-10-11
 */
@Service
public class SysPlanItemServiceImpl implements ISysPlanItemService
{
    @Autowired
    private SysPlanItemMapper sysPlanItemMapper;

    /**
     * 查询测试计划子项
     * 
     * @param planItemId 测试计划子项主键
     * @return 测试计划子项
     */
    @Override
    public SysPlanItem selectSysPlanItemByPlanItemId(String planItemId)
    {
        return sysPlanItemMapper.selectSysPlanItemByPlanItemId(planItemId);
    }

    /**
     * 查询测试计划子项列表
     * 
     * @param sysPlanItem 测试计划子项
     * @return 测试计划子项
     */
    @Override
    public List<SysPlanItem> selectSysPlanItemList(SysPlanItem sysPlanItem)
    {
        return sysPlanItemMapper.selectSysPlanItemList(sysPlanItem);
    }

    @Override
    public List<SysCase> selectCaseList(SysCase sysCase) {
        return sysPlanItemMapper.selectCaseList(sysCase);
    }

    /**
     * 新增测试计划子项
     * 
     * @param sysPlanItem 测试计划子项
     * @return 结果
     */
    @Override
    public int insertSysPlanItem(SysPlanItem sysPlanItem)
    {
        return sysPlanItemMapper.insertSysPlanItem(sysPlanItem);
    }

    /**
     * 修改测试计划子项
     * 
     * @param sysPlanItem 测试计划子项
     * @return 结果
     */
    @Override
    public int updateSysPlanItem(SysPlanItem sysPlanItem)
    {
        sysPlanItem.setUpdateTime(DateUtils.getNowDate());
        return sysPlanItemMapper.updateSysPlanItem(sysPlanItem);
    }

    /**
     * 批量删除测试计划子项
     * 
     * @param planItemIds 需要删除的测试计划子项主键
     * @return 结果
     */
    @Override
    public int deleteSysPlanItemByPlanItemIds(String[] planItemIds)
    {
        return sysPlanItemMapper.deleteSysPlanItemByPlanItemIds(planItemIds);
    }

    /**
     * 删除测试计划子项信息
     * 
     * @param planItemId 测试计划子项主键
     * @return 结果
     */
    @Override
    public int deleteSysPlanItemByPlanItemId(String planItemId)
    {
        return sysPlanItemMapper.deleteSysPlanItemByPlanItemId(planItemId);
    }
}
