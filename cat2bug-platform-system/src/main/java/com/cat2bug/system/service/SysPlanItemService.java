package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.domain.SysPlan;
import com.cat2bug.common.domain.SysPlanItem;
import com.cat2bug.common.domain.SysPlanItemModule;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysModuleMapper;
import com.cat2bug.system.mapper.SysPlanItemMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysPlanItemService {

    @Inject
    SysPlanItemMapper sysPlanItemMapper;

    @Inject
    SysModuleMapper sysModuleMapper;

    @Inject
    SysPlanService sysPlanService;

    @Inject
    AuthContext authContext;

    private final PlanUpdateTimeThrottle planUpdateTimeThrottle = new PlanUpdateTimeThrottle();

    public TableDataInfo selectPlanItemPage(SysPlanItem query, Integer pageNum, Integer pageSize) {
        applyModuleFilter(query);
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysPlanItemMapper.countSysPlanItemList(query);
        List<SysPlanItem> rows = sysPlanItemMapper.selectSysPlanItemList(query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysPlanItem> selectPlanItemListAll(SysPlanItem query) {
        applyModuleFilter(query);
        return sysPlanItemMapper.selectAllSysPlanItemList(query);
    }

    public TableDataInfo selectCasePage(SysCase query, Integer pageNum, Integer pageSize) {
        applyCaseModuleFilter(query);
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysPlanItemMapper.countCaseList(query);
        List<SysCase> rows = sysPlanItemMapper.selectCaseList(query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysPlanItemModule> selectSysModuleList(SysPlanItemModule sysModule) {
        List<SysPlanItemModule> moduleList = sysPlanItemMapper.selectSysModuleList(sysModule);
        return moduleList.stream().map(m -> {
            SysPlanItem probe = new SysPlanItem();
            probe.setPlanId(sysModule.getPlanId());
            probe.setParams(new HashMap<>());
            Set<Long> moduleIds = sysModuleMapper.getAllChildIds(sysModule.getProjectId(), m.getModuleId());
            if (moduleIds != null && !moduleIds.isEmpty()) {
                probe.getParams().put("moduleIdsOfProject", moduleIds);
            } else {
                probe.getParams().put("moduleIdsOfProject", Arrays.asList(0));
            }
            List<SysPlanItem> itemList = sysPlanItemMapper.selectSysPlanItemList(probe, 0, Integer.MAX_VALUE);
            m.setItemCount(itemList.size());
            m.setPassCount(itemList.stream().filter(i -> "pass".equals(i.getPlanItemState())).count());
            m.setDefectCount(itemList.stream().mapToInt(i -> i.getDefectIds() == null ? 0 : i.getDefectIds().size()).sum());
            return m;
        }).collect(Collectors.toList());
    }

    public SysPlanItem selectSysPlanItemByPlanItemId(String planItemId) {
        SysPlanItem item = sysPlanItemMapper.selectSysPlanItemByPlanItemId(planItemId);
        if (item == null) {
            throw new ApiException("计划子项不存在");
        }
        return item;
    }

    public SysPlanItem selectPrevSysPlanItem(SysPlanItem query, String planItemId) {
        if (query.getParams() == null) {
            query.setParams(new HashMap<>());
        }
        applyModuleFilter(query);
        query.getParams().put("prevPlanItemId", planItemId);
        return sysPlanItemMapper.selectPrevSysPlanItem(query);
    }

    public SysPlanItem selectNextSysPlanItem(SysPlanItem query, String planItemId) {
        if (query.getParams() == null) {
            query.setParams(new HashMap<>());
        }
        applyModuleFilter(query);
        query.getParams().put("nextPlanItemId", planItemId);
        return sysPlanItemMapper.selectNextSysPlanItem(query);
    }

    @Transactional
    public int insertSysPlanItem(SysPlanItem sysPlanItem) {
        LoginUser loginUser = requireLoginUser();
        sysPlanItem.setPlanItemId(UUID.randomUUID().toString().replace("-", ""));
        sysPlanItem.setUpdateById(loginUser.getUserId());
        sysPlanItem.setUpdateTime(new Date());
        if (StringUtils.isBlank(sysPlanItem.getPlanItemState())) {
            sysPlanItem.setPlanItemState(SysPlanItem.PLAN_ITEM_DEFAULT_STATE);
        }
        int ret = sysPlanItemMapper.insertSysPlanItem(sysPlanItem);
        if (ret > 0) {
            touchPlanUpdateTime(sysPlanItem.getPlanId(), loginUser.getUserId());
        }
        return ret;
    }

    @Transactional
    public int updateSysPlanItem(SysPlanItem sysPlanItem) {
        LoginUser loginUser = requireLoginUser();
        sysPlanItem.setUpdateById(loginUser.getUserId());
        sysPlanItem.setUpdateTime(new Date());
        int ret;
        if (sysPlanItem.getParams() != null && sysPlanItem.getParams().containsKey("planItemIds")) {
            ret = sysPlanItemMapper.batchUpdateSysPlanItem(sysPlanItem);
        } else {
            ret = sysPlanItemMapper.updateSysPlanItem(sysPlanItem);
        }
        if (ret > 0) {
            touchPlanUpdateTime(sysPlanItem.getPlanId(), loginUser.getUserId());
        }
        return ret;
    }

    @Transactional
    public int deleteSysPlanItemByPlanItemIds(String[] planItemIds) {
        if (planItemIds == null || planItemIds.length == 0) {
            throw new ApiException("计划子项ID不能为空");
        }
        return sysPlanItemMapper.deleteSysPlanItemByPlanItemIds(planItemIds);
    }

    private void touchPlanUpdateTime(String planId, Long userId) {
        if (!planUpdateTimeThrottle.shouldTouch(planId, System.currentTimeMillis())) {
            return;
        }
        SysPlan plan = new SysPlan();
        plan.setPlanId(planId);
        plan.setUpdateById(userId);
        plan.setUpdateTime(new Date());
        sysPlanService.updateUpdateTimeOfSysPlan(plan);
    }

    private void applyModuleFilter(SysPlanItem query) {
        if (query.getParams() == null) {
            query.setParams(new HashMap<>());
        }
        if (query.getProjectId() == null) {
            return;
        }
        Set<Long> moduleIds = sysModuleMapper.getAllChildIds(query.getProjectId(), query.getModuleId());
        if (moduleIds != null && !moduleIds.isEmpty()) {
            query.getParams().put("moduleIdsOfProject", moduleIds);
        } else {
            query.getParams().put("moduleIdsOfProject", Arrays.asList(0));
        }
    }

    private void applyCaseModuleFilter(SysCase query) {
        if (query.getParams() == null) {
            query.setParams(new HashMap<>());
        }
        if (query.getProjectId() == null) {
            return;
        }
        Set<Long> moduleIds = sysModuleMapper.getAllChildIds(query.getProjectId(), query.getModuleId());
        if (moduleIds != null && !moduleIds.isEmpty()) {
            query.getParams().put("moduleIdsOfProject", moduleIds);
        } else {
            query.getParams().put("moduleIdsOfProject", Arrays.asList(0));
        }
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
