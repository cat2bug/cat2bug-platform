package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.SysPlan;
import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.domain.dashboard.SysAction;
import com.cat2bug.common.domain.dashboard.SysCaseStatistics;
import com.cat2bug.common.domain.dashboard.SysColumnsInChart;
import com.cat2bug.common.domain.dashboard.SysDefectStatistics;
import com.cat2bug.common.domain.dashboard.SysDocumentStatistics;
import com.cat2bug.common.domain.dashboard.SysMemberStatistics;
import com.cat2bug.common.domain.dashboard.SysMemberRankOfDefects;
import com.cat2bug.common.domain.dashboard.SysModuleStatistics;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.mapper.SysDashboardMapper;
import com.cat2bug.system.util.SysDefectTrendLineHelper;
import com.cat2bug.system.util.SysMemberDefectLineHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysDashboardService {

    @Inject
    SysDashboardMapper sysDashboardMapper;

    public SysCaseStatistics caseStatistics(Long projectId) {
        SysCaseStatistics stats = sysDashboardMapper.caseStatistics(projectId);
        return stats != null ? stats : emptyCaseStats();
    }

    public SysDefectStatistics defectStatistics(Long projectId) {
        SysDefectStatistics stats = sysDashboardMapper.defectStatistics(projectId);
        return stats != null ? stats : emptyDefectStats();
    }

    public SysModuleStatistics moduleStatistics(Long projectId) {
        SysModuleStatistics stats = sysDashboardMapper.moduleStatistics(projectId);
        return stats != null ? stats : emptyModuleStats();
    }

    public SysDocumentStatistics documentStatistics(Long projectId) {
        SysDocumentStatistics stats = sysDashboardMapper.documentStatistics(projectId);
        return stats != null ? stats : emptyDocumentStats();
    }

    public SysMemberStatistics memberStatistics(Long projectId) {
        SysMemberStatistics stats = sysDashboardMapper.memberStatistics(projectId);
        return stats != null ? stats : emptyMemberStats();
    }

    public Map<String, Object> defectLine(Long projectId, String timeType) {
        return SysDefectTrendLineHelper.buildDefectStateLineResult(
                sysDashboardMapper.defectLine(projectId, timeType), timeType);
    }

    public Map<String, Object> actionList(Long projectId, String type) {
        List<SysAction> actionList = sysDashboardMapper.actonList(projectId, type);
        Map<String, String> stateMap = new HashMap<>();
        for (int i = 0; i < DefectConstants.DEFECT_STATES.length; i++) {
            stateMap.put(String.valueOf(i), DefectConstants.DEFECT_STATES[i]);
        }
        Map<Long, List<SysAction>> actionMap = actionList.stream()
                .map(a -> {
                    if ("defect".equals(a.getType())
                            && StringUtils.isNotEmpty(a.getState())
                            && stateMap.containsKey(a.getState())) {
                        a.setState(stateMap.get(a.getState()));
                    }
                    return a;
                })
                .collect(Collectors.groupingBy(a ->
                        (long) Math.floor(a.getTime().getTime() / (1000.0 * 60 * 60 * 24)) * (1000 * 60 * 60 * 24)))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, List<SysAction>>comparingByKey().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> newValue,
                        TreeMap::new
                ));
        Map<String, Object> ret = new HashMap<>();
        ret.put("actions", actionMap);
        return ret;
    }

    public TableDataInfo planList(Long projectId) {
        List<SysPlan> list = sysDashboardMapper.planList(projectId);
        return new TableDataInfo(list, list.size());
    }

    public SysPlan selectPlanByPlanId(String planId) {
        return sysDashboardMapper.selectPlanByPlanId(planId);
    }

    public List<SysMemberRankOfDefects> memberRankOfDefects(Long projectId) {
        return sysDashboardMapper.memberRankOfDefects(projectId);
    }

    public Map<String, Object> memberOfDefectsLine(Long projectId, String timeType) {
        return SysMemberDefectLineHelper.buildMemberDefectLineResult(
                sysDashboardMapper.memberOfDefectsLine(projectId, timeType), timeType);
    }

    public List<SysColumnsInChart> planBurndown(String planId) {
        SysPlan sysPlan = sysDashboardMapper.selectPlanByPlanId(planId);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        List<SysColumnsInChart> raw = sysDashboardMapper.planBurndown(planId);
        LongSummaryStatistics summaryStatistics = raw.stream().map(x -> {
            try {
                return format.parse(x.getKey()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));

        Map<String, Long> chartMap = raw.stream()
                .collect(Collectors.toMap(SysColumnsInChart::getKey, SysColumnsInChart::getValue));
        List<SysColumnsInChart> list = new LinkedList<>();
        final long daySize = 1000L * 60 * 60 * 24;
        final long defaultDay = 30;
        long itemTotal = sysPlan != null ? sysPlan.getItemTotal() : 0L;
        long dayCount = raw.isEmpty() ? 0 : (summaryStatistics.getMax() - summaryStatistics.getMin()) / daySize;

        if (raw.isEmpty()) {
            for (long i = defaultDay; i >= 0; i--) {
                String day = format.format(new Date(System.currentTimeMillis() - i * daySize));
                list.add(new SysColumnsInChart(day, itemTotal));
            }
        } else if (dayCount < defaultDay) {
            long prevCount = itemTotal;
            for (long i = 0; i <= defaultDay; i++) {
                String day = format.format(new Date(summaryStatistics.getMin() + i * daySize));
                long count = chartMap.containsKey(day) ? prevCount - chartMap.get(day) : prevCount;
                list.add(new SysColumnsInChart(day, count));
                prevCount = count;
            }
        } else {
            long prevCount = itemTotal;
            for (long i = defaultDay; i >= 0; i--) {
                String day = format.format(new Date(summaryStatistics.getMax() - i * daySize));
                long count = chartMap.containsKey(day) ? prevCount - chartMap.get(day) : prevCount;
                list.add(new SysColumnsInChart(day, count));
                prevCount = count;
            }
        }
        return list;
    }

    private SysCaseStatistics emptyCaseStats() {
        SysCaseStatistics s = new SysCaseStatistics();
        s.setTotal(0);
        return s;
    }

    private SysDefectStatistics emptyDefectStats() {
        SysDefectStatistics s = new SysDefectStatistics();
        return s;
    }

    private SysModuleStatistics emptyModuleStats() {
        SysModuleStatistics s = new SysModuleStatistics();
        s.setTotal(0);
        return s;
    }

    private SysDocumentStatistics emptyDocumentStats() {
        SysDocumentStatistics s = new SysDocumentStatistics();
        s.setTotal(0);
        return s;
    }

    private SysMemberStatistics emptyMemberStats() {
        return new SysMemberStatistics();
    }
}
