package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.constant.DefectConstants;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysDefectOpenWorkload;
import com.cat2bug.quarkus.common.domain.SysDefectOpenWorkloadSummary;
import com.cat2bug.quarkus.common.domain.SysDefectParticipationDay;
import com.cat2bug.quarkus.common.domain.SysPlan;
import com.cat2bug.quarkus.common.domain.SysPlanCountdownSummary;
import com.cat2bug.quarkus.common.domain.SysPlanMetricsItem;
import com.cat2bug.quarkus.common.domain.dashboard.SysColumnsInChart;
import com.cat2bug.quarkus.system.mapper.SysDefectStatisticMapper;
import com.cat2bug.quarkus.system.mapper.SysPlanMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysDefectStatisticService {

    private static final int PARTICIPATION_DAYS_MIN = 14;
    private static final int PARTICIPATION_DAYS_MAX = 90;
    private static final int PARTICIPATION_DAYS_DEFAULT = 30;
    private static final int PLAN_METRICS_TOP = 4;
    private static final int PLAN_LIST_LIMIT = 500;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Inject
    SysDefectStatisticMapper sysDefectStatisticMapper;

    @Inject
    SysDashboardService sysDashboardService;

    @Inject
    SysPlanMapper sysPlanMapper;

    public List<Map<String, Object>> typeStatistic(Long projectId, Long memberId) {
        List<Map<String, Object>> ret = sysDefectStatisticMapper.typeStatistic(projectId, memberId);
        for (int i = 0; i < DefectConstants.DEFECT_TYPES.length; i++) {
            int index = i;
            if (ret.stream().noneMatch(m -> index == Long.valueOf(m.get("k").toString()))) {
                Map<String, Object> map = new HashMap<>();
                map.put("k", index);
                map.put("v", 0);
                ret.add(map);
            }
        }
        return ret.stream().map(m -> {
            int ordinal = Long.valueOf(m.get("k").toString()).intValue();
            m.put("k", DefectConstants.DEFECT_TYPES[ordinal]);
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> stateStatistic(Long projectId, Long memberId) {
        List<Map<String, Object>> ret = sysDefectStatisticMapper.stateStatistic(projectId, memberId);
        for (int i = 0; i < DefectConstants.DEFECT_STATES.length; i++) {
            int index = i;
            if (ret.stream().noneMatch(m -> index == Long.valueOf(m.get("k").toString()))) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", index);
                map.put("k", index);
                map.put("a", 0);
                map.put("d", 0);
                map.put("w", 0);
                ret.add(map);
            }
        }
        return ret.stream().map(m -> {
            m.put("id", m.get("k"));
            int ordinal = Long.valueOf(m.get("k").toString()).intValue();
            m.put("k", DefectConstants.DEFECT_STATES[ordinal]);
            return m;
        }).collect(Collectors.toList());
    }

    public TableDataInfo moduleStatisticPage(Long projectId, Integer pageNum, Integer pageSize) {
        return paginate(sysDefectStatisticMapper.moduleStatistic(projectId), pageNum, pageSize);
    }

    public TableDataInfo openWorkloadPage(Long projectId, Integer pageNum, Integer pageSize) {
        return paginate(sysDefectStatisticMapper.openWorkloadByProject(projectId), pageNum, pageSize);
    }

    public SysDefectOpenWorkloadSummary openWorkloadMy(Long projectId, Long userId) {
        SysDefectOpenWorkloadSummary summary = sysDefectStatisticMapper.openWorkloadByMember(projectId, userId);
        if (summary == null) {
            summary = new SysDefectOpenWorkloadSummary();
        }
        return summary;
    }

    public List<SysDefectParticipationDay> participationMy(Long projectId, Long userId, int days) {
        int windowDays = clampParticipationDays(days);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(windowDays - 1L);
        String startDate = start.format(DATE_FORMAT);

        List<SysDefectParticipationDay> raw =
                sysDefectStatisticMapper.participationByDay(projectId, userId, startDate);
        Map<String, Integer> countByDate = raw.stream()
                .collect(Collectors.toMap(SysDefectParticipationDay::getDate,
                        SysDefectParticipationDay::getCount, (a, b) -> b));

        List<SysDefectParticipationDay> result = new ArrayList<>(windowDays);
        for (int i = 0; i < windowDays; i++) {
            String date = start.plusDays(i).format(DATE_FORMAT);
            result.add(new SysDefectParticipationDay(date, countByDate.getOrDefault(date, 0)));
        }
        return result;
    }

    public List<Map<String, Object>> planList(Long projectId) {
        SysPlan query = new SysPlan();
        query.setProjectId(projectId);
        return sysPlanMapper.selectSysPlanList(query, 0, PLAN_LIST_LIMIT).stream().map(plan -> {
            Map<String, Object> item = new HashMap<>(2);
            item.put("planId", plan.getPlanId());
            item.put("planName", plan.getPlanName());
            return item;
        }).collect(Collectors.toList());
    }

    public List<SysColumnsInChart> planBurndownChart(String planId) {
        return sysDashboardService.planBurndown(planId);
    }

    public List<SysPlanMetricsItem> planMetrics(Long projectId) {
        SysPlan query = new SysPlan();
        query.setProjectId(projectId);
        return sysPlanMapper.selectSysPlanList(query, 0, PLAN_LIST_LIMIT).stream()
                .map(this::toPlanMetricsItem)
                .sorted(Comparator.comparing(
                        SysPlanMetricsItem::getUpdateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(PLAN_METRICS_TOP)
                .collect(Collectors.toList());
    }

    public Map<String, Object> defectStateLine(Long projectId, String timeType) {
        return sysDashboardService.defectLine(projectId, timeType);
    }

    public Map<String, Object> memberDefectLine(Long projectId, String timeType) {
        return sysDashboardService.memberOfDefectsLine(projectId, timeType);
    }

    public SysPlanCountdownSummary planCountdownSummary(String planId) {
        SysPlan plan = sysPlanMapper.selectSysPlanByPlanId(planId);
        if (plan == null) {
            return null;
        }
        SysPlanCountdownSummary summary = new SysPlanCountdownSummary();
        summary.setPlanId(plan.getPlanId());
        summary.setPlanName(plan.getPlanName());
        summary.setPlanEndTime(plan.getPlanEndTime());
        summary.setItemTotal(plan.getItemTotal());
        summary.setUnexecutedCount(plan.getUnexecutedCount());
        summary.setPassCount(plan.getPassCount());
        summary.setDefectCount(plan.getDefectCount());
        summary.setDefectCloseStateCount(plan.getDefectCloseStateCount());
        return summary;
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

    private TableDataInfo paginate(List<?> all, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = all.size();
        int from = page.getOffset();
        if (from >= all.size()) {
            return new TableDataInfo(List.of(), total);
        }
        int to = Math.min(from + page.getPageSize(), all.size());
        return new TableDataInfo(all.subList(from, to), total);
    }
}
