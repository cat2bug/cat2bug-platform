package com.cat2bug.web.controller.system;

import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysAction;
import com.cat2bug.system.domain.SysColumnsInChart;
import com.cat2bug.system.domain.SysDefectLine;
import com.cat2bug.system.domain.SysMemberRankOfDefects;
import com.cat2bug.system.service.ISysDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-01-19 04:49
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/system/dashboard")
public class SysDashboardController {
    private final static String MONTH_TYPE = "month";
    @Autowired
    ISysDashboardService sysDashboardService;

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/case")
    public AjaxResult caseStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.caseStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/defect")
    public AjaxResult defectStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.defectStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/module")
    public AjaxResult moduleStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.moduleStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/report")
    public AjaxResult reportStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.reportStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/document")
    public AjaxResult documentStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.documentStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/member")
    public AjaxResult memberStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.memberStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/defect-line")
    public AjaxResult defectLine(@PathVariable("projectId") Long projectId, @RequestParam("timeType") String timeType) {
        List<SysDefectLine> defectLineList = sysDashboardService.defectLine(projectId, timeType);
        Set<String> timeSet = new LinkedHashSet<>();
        switch (timeType) {
            case MONTH_TYPE:
                setCountOfMonth(timeSet, defectLineList);
                break;
            default:
                setCountOfDay(timeSet, defectLineList);
                break;
        }
        Map<SysDefectStateEnum, List<SysDefectLine>> defectStateGroup = defectLineList.stream().collect(Collectors.groupingBy(SysDefectLine::getDefectState));
        Map<String, List<Long>> defectStateLines = new HashMap<>();
        defectStateGroup.forEach((k,v)->{
            defectStateLines.put(k.name(), new ArrayList<>());
            for (String s : timeSet) {
                Optional<SysDefectLine> defectLine = v.stream().filter(d->d.getDefectTime().equals(s)).findFirst();
                if(defectLine.isPresent()) {
                    defectStateLines.get(k.name()).add(defectLine.get().getDefectCount());
                } else {
                    defectStateLines.get(k.name()).add(0L);
                }
            }
        });
        Map<String, Object> ret = new HashMap<>();
        ret.put("data", defectStateLines);              // 按状态类型分组的日完成数量
        ret.put("types", SysDefectStateEnum.values());  // 状态分类
        ret.put("times", timeSet);                      // 时间数组
        return AjaxResult.success(ret);
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/actions")
    public AjaxResult actionList(@PathVariable("projectId") Long projectId, @RequestParam("type") String type) {
        List<SysAction> actionList = sysDashboardService.actonList(projectId, type);
        Map<String, String> stateMap = Arrays.stream(SysDefectStateEnum.values()).collect(Collectors.toMap(v->String.valueOf(v.ordinal()), v->v.name()));
        Map<Long, List<SysAction>> actionMap = actionList.stream()
                .map(a->{
                    // 设置缺陷的状态名称
                    if("defect".equals(a.getType()) && StringUtils.isNotBlank(a.getState()) && stateMap.containsKey(a.getState())) {
                        a.setState(stateMap.get(a.getState()));
                    }
                    return a;
                })
                // 分组
                .collect(Collectors.groupingBy(a->((Double)(Math.floor(a.getTime().getTime()/(1000*60*60*24)))).longValue()*(1000*60*60*24)))
                // 排序
                .entrySet().stream().sorted(Map.Entry.<Long, List<SysAction>>comparingByKey().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> newValue,
                        TreeMap::new
                ));
        Map<String, Object> ret = new HashMap<>();
        ret.put("actions", actionMap);              // 按状态类型分组的日完成数量
        return AjaxResult.success(ret);
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/plan/{planId}")
    public AjaxResult planBurndown(@PathVariable("projectId") Long projectId, @PathVariable("planId") String planId) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        List<SysColumnsInChart> sysColumnsInChartList =  sysDashboardService.planBurndown(planId);
        // 获取最大最小日期的操作对象
        LongSummaryStatistics summaryStatistics = sysColumnsInChartList.stream().map(x-> {
            try {
                return format.parse(x.getKey()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));

        Map<String, Long> sysColumnsInChartMap = sysColumnsInChartList.stream().collect(Collectors.toMap(SysColumnsInChart::getKey, SysColumnsInChart::getValue));
        List<SysColumnsInChart> list = new LinkedList<>();
        final long DAY_SIZE = 1000*60*60*24;    // 一天的毫秒时长
        final long DEFAULT_DAY = 30;            // 默认最大天数
        long dayCount = (summaryStatistics.getMax() - summaryStatistics.getMin())/DAY_SIZE;
        if(dayCount<DEFAULT_DAY) {
            for(long i=0;i<=DEFAULT_DAY;i++) {
                String day = format.format(new Date(summaryStatistics.getMin()+i*DAY_SIZE));
                list.add(new SysColumnsInChart(day,
                        sysColumnsInChartMap.containsKey(day)?sysColumnsInChartMap.get(day):0));
            }
        } else {
            // 如果缺陷数据大于30天，就显示30的的
            for(long i=DEFAULT_DAY;i>=0;i--) {
                String day = format.format(new Date(summaryStatistics.getMax()-i*DAY_SIZE));
                list.add(new SysColumnsInChart(day,
                        sysColumnsInChartMap.containsKey(day)?sysColumnsInChartMap.get(day):0));
            }
        }
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/member-defect")
    public AjaxResult memberRankOfDefects(@PathVariable("projectId") Long projectId) {
        List<SysMemberRankOfDefects> list = sysDashboardService.memberRankOfDefects(projectId);
        return AjaxResult.success(list);
    }

    private void setCountOfDay(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        LongSummaryStatistics summaryStatistics = defectLineList.stream().map(x-> {
            try {
                return format.parse(x.getDefectTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));
        final long DAY_SIZE = 1000*60*60*24;    // 一天的毫秒时长
        final long DEFAULT_DAY = 30;            // 默认最大天数
        long dayCount = (summaryStatistics.getMax() - summaryStatistics.getMin())/DAY_SIZE;

        // 如果缺陷数据小于30天，就显示30天的，没有的天数补齐
        if(dayCount<DEFAULT_DAY) {
            for(long i=0;i<=DEFAULT_DAY;i++) {
                timeSet.add(format.format(new Date(summaryStatistics.getMin()+i*DAY_SIZE)));
            }
        } else {
            // 如果缺陷数据大于30天，就显示30的的
            for(long i=DEFAULT_DAY;i>=0;i--) {
                timeSet.add(format.format(new Date(summaryStatistics.getMax()-i*DAY_SIZE)));
            }
        }
    }

    private void setCountOfMonth(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        LongSummaryStatistics summaryStatistics = defectLineList.stream().map(x-> {
            try {
                return format.parse(x.getDefectTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));
        final int DEFAULT_MONTH = 12;            // 默认最大天数
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(new Date(summaryStatistics.getMin()));
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(new Date(summaryStatistics.getMax()));
        int monthCount = endCalendar.get(Calendar.MONTH)-startCalendar.get(Calendar.MONTH);

        // 如果缺陷数据小于12个月，就显示12个月的，没有的天数补齐
        if(monthCount<DEFAULT_MONTH) {
            timeSet.add(format.format(startCalendar.getTime()));
            for(int i=1;i<DEFAULT_MONTH;i++) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else {
            // 如果缺陷数据大于12个月，就显示30的的
            endCalendar.add(Calendar.MONTH, -DEFAULT_MONTH);
            timeSet.add(format.format(endCalendar.getTime()));
            for(int i=1;i<=DEFAULT_MONTH;i++) {
                endCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(endCalendar.getTime()));
            }
        }
    }
}
