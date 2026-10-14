package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysDefect;
import com.cat2bug.quarkus.common.domain.SysPlan;
import com.cat2bug.quarkus.common.domain.SysPlanItem;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.framework.security.AuthContext;
import com.cat2bug.quarkus.system.mapper.SysModuleMapper;
import com.cat2bug.quarkus.system.mapper.SysPlanMapper;
import com.cat2bug.quarkus.system.util.DefectListKeywordSupport;
import com.cat2bug.quarkus.system.util.DefectMemberEnricher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysPlanService {

    @Inject
    SysPlanMapper sysPlanMapper;

    @Inject
    SysModuleMapper sysModuleMapper;

    @Inject
    DefectListKeywordSupport defectListKeywordSupport;

    @Inject
    AuthContext authContext;

    @Inject
    DefectMemberEnricher defectMemberEnricher;

    public TableDataInfo selectPlanPage(SysPlan query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysPlanMapper.countSysPlanList(query);
        List<SysPlan> rows = sysPlanMapper.selectSysPlanList(query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysPlan> selectPlanListAll(SysPlan query) {
        return sysPlanMapper.selectSysPlanList(query, 0, Integer.MAX_VALUE);
    }

    public SysPlan selectSysPlanByPlanId(String planId) {
        SysPlan plan = sysPlanMapper.selectSysPlanByPlanId(planId);
        if (plan == null) {
            throw new ApiException("测试计划不存在");
        }
        return plan;
    }

    public TableDataInfo selectPlanDefectPage(String planId, SysDefect sysDefect, Integer pageNum, Integer pageSize) {
        selectSysPlanByPlanId(planId);
        if (sysDefect == null) {
            sysDefect = new SysDefect();
        }
        if (sysDefect.getParams() == null) {
            sysDefect.setParams(new HashMap<>());
        }
        Set<Long> moduleIds = sysModuleMapper.getAllChildIds(sysDefect.getProjectId(), sysDefect.getModuleId());
        if (!moduleIds.isEmpty()) {
            sysDefect.getParams().put("moduleIdsOfProject", moduleIds);
        } else {
            sysDefect.getParams().put("moduleIdsOfProject", Arrays.asList(0L));
        }
        defectListKeywordSupport.fillNameVersionKeywordHandleBy(sysDefect);
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysPlanMapper.countSysDefectListByPlanId(planId, sysDefect);
        List<SysDefect> rows = sysPlanMapper.selectSysDefectListByPlanId(
                planId, sysDefect, page.getOffset(), page.getPageSize());
        defectMemberEnricher.enrich(rows);
        return new TableDataInfo(rows, total);
    }

    @Transactional
    public SysPlan insertSysPlan(SysPlan sysPlan) {
        LoginUser loginUser = requireLoginUser();
        Date now = new Date();
        sysPlan.setPlanId(UUID.randomUUID().toString().replace("-", ""));
        long maxNumber = sysPlanMapper.getProjectPlanMaxNum(sysPlan.getProjectId());
        sysPlan.setPlanNumber(maxNumber + 1);
        sysPlan.setCreateById(loginUser.getUserId());
        sysPlan.setCreateTime(now);
        sysPlan.setUpdateById(loginUser.getUserId());
        sysPlan.setUpdateTime(now);
        if (sysPlanMapper.insertSysPlan(sysPlan) <= 0) {
            throw new ApiException("创建测试计划失败");
        }
        insertSysPlanItem(sysPlan, loginUser.getUserId(), now);
        return sysPlan;
    }

    @Transactional
    public int copySysPlan(String planId) {
        SysPlan sysPlan = selectSysPlanByPlanId(planId);
        sysPlan.setPlanName(sysPlan.getPlanName() + "-复制");
        return insertSysPlan(sysPlan) != null ? 1 : 0;
    }

    @Transactional
    public int updateSysPlan(SysPlan sysPlan) {
        if (sysPlan.getPlanId() == null || sysPlan.getPlanId().isBlank()) {
            throw new ApiException("计划ID不能为空");
        }
        selectSysPlanByPlanId(sysPlan.getPlanId());
        LoginUser loginUser = requireLoginUser();
        Date now = new Date();
        sysPlan.setUpdateTime(now);
        sysPlan.setUpdateById(loginUser.getUserId());

        List<SysPlanItem> oldItemList = sysPlanMapper.selectSysPlanItemListByPlanId(sysPlan.getPlanId());
        if (sysPlan.getSysPlanItemList() == null) {
            sysPlan.setSysPlanItemList(new ArrayList<>());
        }

        String[] removeItems = oldItemList.stream()
                .filter(i -> sysPlan.getSysPlanItemList().stream()
                        .map(SysPlanItem::getCaseId)
                        .noneMatch(id -> id != null && id.equals(i.getCaseId())))
                .map(SysPlanItem::getPlanItemId)
                .toArray(String[]::new);
        if (removeItems.length > 0) {
            sysPlanMapper.deleteSysPlanItemByPlanItemIds(removeItems);
        }

        List<SysPlanItem> addItemList = sysPlan.getSysPlanItemList().stream()
                .filter(i -> oldItemList.stream()
                        .map(SysPlanItem::getCaseId)
                        .noneMatch(id -> id != null && id.equals(i.getCaseId())))
                .collect(Collectors.toList());
        if (!addItemList.isEmpty()) {
            sysPlan.setSysPlanItemList(addItemList);
            insertSysPlanItem(sysPlan, loginUser.getUserId(), now);
        }

        return sysPlanMapper.updateSysPlan(sysPlan);
    }

    @Transactional
    public int deleteSysPlanByPlanIds(String[] planIds) {
        if (planIds == null || planIds.length == 0) {
            throw new ApiException("计划ID不能为空");
        }
        sysPlanMapper.deleteSysPlanItemByPlanIds(planIds);
        return sysPlanMapper.deleteSysPlanByPlanIds(planIds);
    }

    private void insertSysPlanItem(SysPlan sysPlan, Long userId, Date now) {
        List<SysPlanItem> sysPlanItemList = sysPlan.getSysPlanItemList();
        String planId = sysPlan.getPlanId();
        if (sysPlanItemList == null || sysPlanItemList.isEmpty()) {
            return;
        }
        List<SysPlanItem> list = new ArrayList<>();
        for (SysPlanItem item : sysPlanItemList) {
            item.setPlanItemId(UUID.randomUUID().toString().replace("-", ""));
            item.setPlanId(planId);
            item.setUpdateById(userId);
            item.setUpdateTime(now);
            item.setPlanItemState(SysPlanItem.PLAN_ITEM_DEFAULT_STATE);
            list.add(item);
        }
        sysPlanMapper.batchSysPlanItem(list);
    }

    public int updateUpdateTimeOfSysPlan(SysPlan sysPlan) {
        if (sysPlan.getUpdateTime() == null) {
            sysPlan.setUpdateTime(new Date());
        }
        return sysPlanMapper.updateUpdateTimeOfSysPlan(sysPlan);
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
