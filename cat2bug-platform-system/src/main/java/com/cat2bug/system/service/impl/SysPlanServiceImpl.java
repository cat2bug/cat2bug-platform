package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.uuid.UUID;
import com.cat2bug.system.mapper.SysPlanItemMapper;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.cat2bug.common.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.cat2bug.system.domain.SysPlanItem;
import com.cat2bug.system.mapper.SysPlanMapper;
import com.cat2bug.system.domain.SysPlan;
import com.cat2bug.system.service.ISysPlanService;

/**
 * 测试计划Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-10-11
 */
@Service
public class SysPlanServiceImpl implements ISysPlanService 
{
    @Autowired
    private SysPlanMapper sysPlanMapper;
    @Autowired
    private SysPlanItemMapper sysPlanItemMapper;

    /**
     * 查询测试计划
     * 
     * @param planId 测试计划主键
     * @return 测试计划
     */
    @Override
    public SysPlan selectSysPlanByPlanId(String planId)
    {
        return sysPlanMapper.selectSysPlanByPlanId(planId);
    }

    /**
     * 查询测试计划列表
     * 
     * @param sysPlan 测试计划
     * @return 测试计划
     */
    @Override
    public List<SysPlan> selectSysPlanList(SysPlan sysPlan)
    {
        return sysPlanMapper.selectSysPlanList(sysPlan);
    }

    /**
     * 新增测试计划
     * 
     * @param sysPlan 测试计划
     * @return 结果
     */
    @Transactional
    @Override
    public int insertSysPlan(SysPlan sysPlan)
    {
        sysPlan.setPlanId(UUID.fastUUID().toString());
        sysPlan.setCreateById(SecurityUtils.getUserId());
        sysPlan.setCreateTime(DateUtils.getNowDate());
        sysPlan.setUpdateById(SecurityUtils.getUserId());
        sysPlan.setUpdateTime(DateUtils.getNowDate());
        int rows = sysPlanMapper.insertSysPlan(sysPlan);
        Preconditions.checkArgument(rows>0, MessageUtils.message("plan.create-fail"));
        insertSysPlanItem(sysPlan);
        return rows;
    }

    /**
     * 修改测试计划
     * 
     * @param sysPlan 测试计划
     * @return 结果
     */
    @Transactional
    @Override
    public int updateSysPlan(SysPlan sysPlan)
    {
        sysPlan.setUpdateTime(DateUtils.getNowDate());
        sysPlan.setUpdateById(SecurityUtils.getUserId());

        // 获取数据库里旧的计划子项列表
        SysPlanItem itemParam = new SysPlanItem();
        itemParam.setPlanId(sysPlan.getPlanId());
        List<SysPlanItem> oldItemList = sysPlanItemMapper.selectSysPlanItemList(itemParam);

        // 计算要删除的子项
        String[] removeItems = oldItemList.stream().filter(i->
                sysPlan.getSysPlanItemList().stream().
                        map(SysPlanItem::getCaseId).
                        noneMatch(id->id==i.getCaseId())
        ).map(i->i.getPlanItemId()).collect(Collectors.toList()).toArray(new String[]{});

        // 删除子项
        if(removeItems.length>0) {
            sysPlanItemMapper.deleteSysPlanItemByPlanItemIds(removeItems);
        }

        // 计算添加的子项
        List<SysPlanItem> addItemList = sysPlan.getSysPlanItemList().stream().filter(i->
                oldItemList.stream().
                        map(SysPlanItem::getCaseId).
                        noneMatch(id->id==i.getCaseId())
        ).collect(Collectors.toList());
        // 添加新子项
        if(addItemList.size()>0) {
            sysPlan.setSysPlanItemList(addItemList);
            insertSysPlanItem(sysPlan);
        }

        // 更新计划
        return sysPlanMapper.updateSysPlan(sysPlan);
    }

    /**
     * 批量删除测试计划
     * 
     * @param planIds 需要删除的测试计划主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteSysPlanByPlanIds(String[] planIds)
    {
        sysPlanMapper.deleteSysPlanItemByPlanIds(planIds);
        return sysPlanMapper.deleteSysPlanByPlanIds(planIds);
    }

    /**
     * 删除测试计划信息
     * 
     * @param planId 测试计划主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteSysPlanByPlanId(String planId)
    {
        sysPlanMapper.deleteSysPlanItemByPlanId(planId);
        return sysPlanMapper.deleteSysPlanByPlanId(planId);
    }

    /**
     * 新增测试计划子项信息
     * 
     * @param sysPlan 测试计划对象
     */
    public void insertSysPlanItem(SysPlan sysPlan)
    {
        List<SysPlanItem> sysPlanItemList = sysPlan.getSysPlanItemList();
        String planId = sysPlan.getPlanId();
        if (StringUtils.isNotNull(sysPlanItemList))
        {
            List<SysPlanItem> list = new ArrayList<SysPlanItem>();
            for (SysPlanItem sysPlanItem : sysPlanItemList)
            {
                sysPlanItem.setPlanItemId(UUID.fastUUID().toString());
                sysPlanItem.setPlanId(planId);
                sysPlanItem.setUpdateById(SecurityUtils.getUserId());
                sysPlanItem.setUpdateTime(DateUtils.getNowDate());
                list.add(sysPlanItem);
            }
            if (list.size() > 0)
            {
                sysPlanMapper.batchSysPlanItem(list);
            }
        }
    }
}
