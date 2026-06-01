package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.system.domain.SysColumnsInChart;
import com.cat2bug.system.domain.SysDefectOpenWorkload;
import com.cat2bug.system.domain.SysDefectOpenWorkloadSummary;
import com.cat2bug.system.domain.SysDefectParticipationDay;
import com.cat2bug.system.domain.SysPlan;
import com.cat2bug.system.domain.SysPlanMetricsItem;
import com.cat2bug.system.mapper.SysDefectStatisticMapper;
import com.cat2bug.system.service.ISysDashboardService;
import com.cat2bug.system.service.ISysDefectStatisticService;
import com.cat2bug.system.service.ISysPlanService;
import com.cat2bug.system.util.PlanBurndownUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-01-22 02:03
 * @Version: 1.0.0
 */
@Service
public class SysDefectStatisticServiceImpl implements ISysDefectStatisticService {
    private static final int PARTICIPATION_DAYS_MIN = 14;
    private static final int PARTICIPATION_DAYS_MAX = 90;
    private static final int PARTICIPATION_DAYS_DEFAULT = 30;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    private SysDefectStatisticMapper sysDefectStatisticMapper;
    @Autowired
    private ISysDashboardService sysDashboardService;
    @Autowired
    private ISysPlanService sysPlanService;

    @Override
    public List<Map<String,Object>> typeStatistic(Long projectId, Long memberId) {
        List<Map<String,Object>> ret = sysDefectStatisticMapper.typeStatistic(projectId,memberId);

        for(int i=0;i<SysDefectTypeEnum.values().length;i++) {
            int index = SysDefectTypeEnum.values()[i].ordinal();
            if(ret.stream().anyMatch(m->index==Long.valueOf(m.get("k").toString()))==false) {
                Map<String,Object> map = new HashMap<>();
                map.put("k",index);
                map.put("v",0);
                ret.add(map);
            }
        }
        return ret.stream().map(m->{
            m.put("k", SysDefectTypeEnum.values()[Long.valueOf(m.get("k").toString()).intValue()].name());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> stateStatistic(Long projectId, Long memberId) {
        List<Map<String,Object>> ret = sysDefectStatisticMapper.stateStatistic(projectId,memberId);
        for(int i=0;i<SysDefectStateEnum.values().length;i++) {
            int index = SysDefectStateEnum.values()[i].ordinal();
            if(ret.stream().anyMatch(m->index==Long.valueOf(m.get("k").toString()))==false) {
                Map<String,Object> map = new HashMap<>();
                map.put("id",index);
                map.put("k",index);
                map.put("a",0);
                map.put("d",0);
                map.put("w",0);
                ret.add(map);
            }
        }
        return ret.stream().map(m->{
            m.put("id",m.get("k"));
            m.put("k", SysDefectStateEnum.values()[Long.valueOf(m.get("k").toString()).intValue()].name());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> moduleStatistic(Long projectId) {
        return sysDefectStatisticMapper.moduleStatistic(projectId);
    }

    @Override
    public List<SysDefectOpenWorkload> openWorkload(Long projectId) {
        return sysDefectStatisticMapper.openWorkloadByProject(projectId);
    }

    @Override
    public SysDefectOpenWorkloadSummary openWorkloadMy(Long projectId, Long userId) {
        SysDefectOpenWorkloadSummary summary = sysDefectStatisticMapper.openWorkloadByMember(projectId, userId);
        if (summary == null) {
            summary = new SysDefectOpenWorkloadSummary();
        }
        return summary;
    }

    @Override
    public List<SysDefectParticipationDay> participationMy(Long projectId, Long userId, int days) {
        int windowDays = clampParticipationDays(days);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(windowDays - 1L);
        String startDate = start.format(DATE_FORMAT);

        List<SysDefectParticipationDay> raw = sysDefectStatisticMapper.participationByDay(projectId, userId, startDate);
        Map<String, Integer> countByDate = raw.stream()
                .collect(Collectors.toMap(SysDefectParticipationDay::getDate, SysDefectParticipationDay::getCount, (a, b) -> b));

        List<SysDefectParticipationDay> result = new ArrayList<>(windowDays);
        for (int i = 0; i < windowDays; i++) {
            String date = start.plusDays(i).format(DATE_FORMAT);
            result.add(new SysDefectParticipationDay(date, countByDate.getOrDefault(date, 0)));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> planList(Long projectId) {
        SysPlan query = new SysPlan();
        query.setProjectId(projectId);
        query.getParams().put("dataType", "simple");
        return sysPlanService.selectSysPlanList(query).stream().map(plan -> {
            Map<String, Object> item = new HashMap<>(2);
            item.put("planId", plan.getPlanId());
            item.put("planName", plan.getPlanName());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public List<SysColumnsInChart> planBurndownChart(String planId) {
        SysPlan sysPlan = sysPlanService.selectSysPlanByPlanId(planId);
        List<SysColumnsInChart> raw = sysDashboardService.planBurndown(planId);
        return PlanBurndownUtil.fillBurndown(raw, sysPlan);
    }

    /** 雷达图仅展示最近更新的前 N 个测试计划 */
    static final int PLAN_METRICS_TOP = 4;

    @Override
    public List<SysPlanMetricsItem> planMetrics(Long projectId) {
        SysPlan query = new SysPlan();
        query.setProjectId(projectId);
        return sysPlanService.selectSysPlanList(query).stream()
                .map(this::toPlanMetricsItem)
                .sorted(Comparator.comparing(
                        SysPlanMetricsItem::getUpdateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(PLAN_METRICS_TOP)
                .collect(Collectors.toList());
    }

    static int clampParticipationDays(int days) {
        if (days <= 0) {
            return PARTICIPATION_DAYS_DEFAULT;
        }
        return Math.max(PARTICIPATION_DAYS_MIN, Math.min(PARTICIPATION_DAYS_MAX, days));
    }

    static double parsePercent(String rate) {
        if (rate == null || rate.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(rate.replace("%", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private SysPlanMetricsItem toPlanMetricsItem(SysPlan plan) {
        SysPlanMetricsItem item = new SysPlanMetricsItem();
        item.setPlanId(plan.getPlanId());
        item.setPlanName(plan.getPlanName());
        item.setUpdateTime(plan.getUpdateTime());
        item.setDiscovery(parsePercent(plan.getDefectDiscoveryRate()));
        item.setRepair(parsePercent(plan.getDefectRepairRate()));
        item.setDetection(parsePercent(plan.getDefectDetectionRate()));
        item.setSeverity(parsePercent(plan.getDefectSeverityRate()));
        item.setRestart(parsePercent(plan.getDefectRestartRate()));
        item.setEscape(parsePercent(plan.getDefectEscapeRate()));
        return item;
    }
}
