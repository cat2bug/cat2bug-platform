package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.uuid.IdUtils;
import com.cat2bug.common.utils.uuid.UUID;
import com.cat2bug.system.domain.*;
import com.cat2bug.system.mapper.SysModuleMapper;
import com.cat2bug.system.mapper.SysPlanItemMapper;
import com.cat2bug.system.service.ISysDefectService;
import com.cat2bug.system.service.ISysPlanItemService;
import com.cat2bug.system.service.ISysUserConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Autowired
    private ISysUserConfigService sysUserConfigService;
    @Autowired
    private SysModuleMapper sysModuleMapper;
    @Autowired
    private SysPlanServiceImpl sysPlanServiceImpl;
    @Autowired
    private ISysDefectService sysDefectService;
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
     * 获取上一个计划项
     * @param sysPlanItem
     * @return
     */
    @Override
    public SysPlanItem selectPrevSysPlanItem(SysPlanItem sysPlanItem) {
        return sysPlanItemMapper.selectPrevSysPlanItem(sysPlanItem);
    }

    /**
     * 获取下一个计划项
     * @param sysPlanItem
     * @return
     */
    @Override
    public SysPlanItem selectNextSysPlanItem(SysPlanItem sysPlanItem) {
        return sysPlanItemMapper.selectNextSysPlanItem(sysPlanItem);
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

    @Override
    public List<SysPlanItemModule> selectSysModuleList(SysPlanItemModule sysModule) {
        List<SysPlanItemModule> moduleList = sysPlanItemMapper.selectSysModuleList(sysModule);
        return moduleList.stream().map(m->{
            SysPlanItem sysPlanItem = new SysPlanItem();
//            sysPlanItem.setModuleId(m.getModuleId());
            sysPlanItem.setPlanId(sysModule.getPlanId());
            sysPlanItem.setParams(new HashMap<>());
            Set<Long> moduleIds = this.sysModuleMapper.getAllChildIds(sysModule.getProjectId(), m.getModuleId());
            if(moduleIds.size()>0) {
                sysPlanItem.getParams().put("moduleIdsOfProject", moduleIds);
            } else {
                sysPlanItem.getParams().put("moduleIdsOfProject", Arrays.asList(0));
            }
            List<SysPlanItem> itemList = sysPlanItemMapper.selectSysPlanItemList(sysPlanItem);
            m.setItemCount(itemList.size());
            m.setPassCount(itemList.stream().filter(i->"pass".equals(i.getPlanItemState())).count()); // 统计通过的数量
            m.setDefectCount(itemList.stream().mapToInt(i->i.getDefectIds().size()).sum());
            return m;
        }).collect(Collectors.toList());
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
        sysPlanItem.setPlanItemId(UUID.fastUUID().toString());
        sysPlanItem.setUpdateById(SecurityUtils.getUserId());
        sysPlanItem.setUpdateTime(DateUtils.getNowDate());
        if(StringUtils.isBlank(sysPlanItem.getPlanItemState())) {
            sysPlanItem.setPlanItemState(SysPlanItem.PLAN_ITEM_DEFAULT_STATE);
        }
        int ret = sysPlanItemMapper.insertSysPlanItem(sysPlanItem);
        if(ret>0) {
            SysPlan sysPlan = new SysPlan();
            sysPlan.setPlanId(sysPlanItem.getPlanId());
            sysPlanServiceImpl.updateUpdateTimeOfSysPlan(sysPlan);
        }
        return ret;
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
        sysPlanItem.setUpdateById(SecurityUtils.getUserId());
        sysPlanItem.setUpdateTime(DateUtils.getNowDate());
        int ret;
        if(sysPlanItem.getParams()!=null && sysPlanItem.getParams().containsKey("planItemIds")) {
            ret = sysPlanItemMapper.batchUpdateSysPlanItem(sysPlanItem);
        } else {
            ret = sysPlanItemMapper.updateSysPlanItem(sysPlanItem);
        }

        if(ret>0) {
            SysPlan sysPlan = new SysPlan();
            sysPlan.setPlanId(sysPlanItem.getPlanId());
            sysPlanServiceImpl.updateUpdateTimeOfSysPlan(sysPlan);
        }
        return ret;
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
