package com.cat2bug.system.util;

import com.cat2bug.system.domain.SysColumnsInChart;
import com.cat2bug.system.domain.SysPlan;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 测试计划燃尽图日期轴补齐（与仪表盘一致）
 */
public final class PlanBurndownUtil {

    private static final long DAY_SIZE = 1000L * 60 * 60 * 24;
    private static final long DEFAULT_DAY = 30;

    private PlanBurndownUtil() {
    }

    public static List<SysColumnsInChart> fillBurndown(List<SysColumnsInChart> sysColumnsInChartList, SysPlan sysPlan) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        LongSummaryStatistics summaryStatistics = sysColumnsInChartList.stream().map(x -> {
            try {
                return format.parse(x.getKey()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));

        Map<String, Long> sysColumnsInChartMap = sysColumnsInChartList.stream()
                .collect(Collectors.toMap(SysColumnsInChart::getKey, SysColumnsInChart::getValue, (a, b) -> b));
        List<SysColumnsInChart> list = new LinkedList<>();
        long dayCount = sysColumnsInChartList.isEmpty() ? 0
                : (summaryStatistics.getMax() - summaryStatistics.getMin()) / DAY_SIZE;
        long itemTotal = sysPlan != null ? sysPlan.getItemTotal() : 0;

        if (sysColumnsInChartList.isEmpty()) {
            for (long i = DEFAULT_DAY; i >= 0; i--) {
                String day = format.format(new Date(System.currentTimeMillis() - i * DAY_SIZE));
                list.add(new SysColumnsInChart(day, itemTotal));
            }
        } else if (dayCount < DEFAULT_DAY) {
            long prevCount = itemTotal;
            for (long i = 0; i <= DEFAULT_DAY; i++) {
                String day = format.format(new Date(summaryStatistics.getMin() + i * DAY_SIZE));
                long count = sysColumnsInChartMap.containsKey(day)
                        ? prevCount - sysColumnsInChartMap.get(day) : prevCount;
                list.add(new SysColumnsInChart(day, count));
                prevCount = count;
            }
        } else {
            long prevCount = itemTotal;
            for (long i = DEFAULT_DAY; i >= 0; i--) {
                String day = format.format(new Date(summaryStatistics.getMax() - i * DAY_SIZE));
                long count = sysColumnsInChartMap.containsKey(day)
                        ? prevCount - sysColumnsInChartMap.get(day) : prevCount;
                list.add(new SysColumnsInChart(day, count));
                prevCount = count;
            }
        }
        return list;
    }
}
