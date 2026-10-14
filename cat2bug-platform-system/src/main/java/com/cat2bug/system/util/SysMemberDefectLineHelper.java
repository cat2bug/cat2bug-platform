package com.cat2bug.system.util;

import com.cat2bug.common.domain.dashboard.SysMemberOfDefectsLine;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成员处理缺陷走势（与 Spring 仪表盘 member-defect-line 口径一致）
 */
public final class SysMemberDefectLineHelper {

    public static final String MONTH_TYPE = "month";

    private SysMemberDefectLineHelper() {
    }

    public static Map<String, Object> buildMemberDefectLineResult(List<SysMemberOfDefectsLine> list, String timeType) {
        Set<String> timeSet = new LinkedHashSet<>();
        if (MONTH_TYPE.equals(timeType)) {
            setMemberDefectCountOfMonth(timeSet, list);
        } else {
            setMemberDefectCountOfDay(timeSet, list);
        }
        Map<String, Object> ret = new HashMap<>();
        ret.put("time", timeSet);
        ret.put("data", list);
        return ret;
    }

    private static void setMemberDefectCountOfDay(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        LongSummaryStatistics summaryStatistics = list.stream().map(x -> {
            try {
                return format.parse(x.getCreateTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));

        final long daySize = 1000L * 60 * 60 * 24;
        final long defaultDay = 30;
        long dayCount = (summaryStatistics.getMax() - summaryStatistics.getMin()) / daySize;
        if (list.isEmpty()) {
            for (long i = defaultDay; i >= 0; i--) {
                timeSet.add(format.format(new Date(System.currentTimeMillis() - i * daySize)));
            }
        } else if (dayCount < defaultDay) {
            for (long i = 0; i <= defaultDay; i++) {
                timeSet.add(format.format(new Date(summaryStatistics.getMin() + i * daySize)));
            }
        } else {
            for (long i = defaultDay; i >= 0; i--) {
                timeSet.add(format.format(new Date(summaryStatistics.getMax() - i * daySize)));
            }
        }
    }

    private static void setMemberDefectCountOfMonth(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        LongSummaryStatistics summaryStatistics = list.stream().map(x -> {
            try {
                return format.parse(x.getCreateTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));

        final int defaultMonth = 12;
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(new Date(summaryStatistics.getMin()));
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(new Date(summaryStatistics.getMax()));
        int monthCount = endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);

        if (list.isEmpty()) {
            startCalendar.setTime(new Date());
            startCalendar.add(Calendar.MONTH, -defaultMonth);
            for (long i = defaultMonth; i >= 0; i--) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else if (monthCount < defaultMonth) {
            timeSet.add(format.format(startCalendar.getTime()));
            for (int i = 1; i < defaultMonth; i++) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else {
            endCalendar.add(Calendar.MONTH, -defaultMonth);
            timeSet.add(format.format(endCalendar.getTime()));
            for (int i = 1; i <= defaultMonth; i++) {
                endCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(endCalendar.getTime()));
            }
        }
    }
}
