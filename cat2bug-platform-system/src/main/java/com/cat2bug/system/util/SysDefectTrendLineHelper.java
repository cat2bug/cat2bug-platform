package com.cat2bug.system.util;

import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.system.domain.SysDefectLine;
import com.cat2bug.system.domain.SysMemberOfDefectsLine;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 缺陷状态 / 成员处理走势（与仪表盘 defect-line、member-defect-line 口径一致）
 */
public final class SysDefectTrendLineHelper {

    public static final String MONTH_TYPE = "month";

    private SysDefectTrendLineHelper() {
    }

    public static Map<String, Object> buildDefectStateLineResult(List<SysDefectLine> defectLineList, String timeType) {
        Set<String> timeSet = new LinkedHashSet<>();
        if (MONTH_TYPE.equals(timeType)) {
            setCountOfMonth(timeSet, defectLineList);
        } else {
            setCountOfDay(timeSet, defectLineList);
        }
        Map<SysDefectStateEnum, List<SysDefectLine>> defectStateGroup = defectLineList.stream()
                .collect(Collectors.groupingBy(SysDefectLine::getDefectState));
        Map<String, List<Long>> defectStateLines = new HashMap<>();
        defectStateGroup.forEach((k, v) -> {
            defectStateLines.put(k.name(), new ArrayList<>());
            for (String s : timeSet) {
                Optional<SysDefectLine> defectLine = v.stream().filter(d -> d.getDefectTime().equals(s)).findFirst();
                if (defectLine.isPresent()) {
                    defectStateLines.get(k.name()).add(defectLine.get().getDefectCount());
                } else {
                    defectStateLines.get(k.name()).add(0L);
                }
            }
        });
        Map<String, Object> ret = new HashMap<>();
        ret.put("data", defectStateLines);
        ret.put("types", SysDefectStateEnum.values());
        ret.put("times", timeSet);
        return ret;
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

    private static void setCountOfDay(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        LongSummaryStatistics summaryStatistics = defectLineList.stream().map(x -> {
            try {
                return format.parse(x.getDefectTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));
        final long DAY_SIZE = 1000 * 60 * 60 * 24;
        final long DEFAULT_DAY = 30;
        long dayCount = (summaryStatistics.getMax() - summaryStatistics.getMin()) / DAY_SIZE;

        if (defectLineList.isEmpty()) {
            for (long i = DEFAULT_DAY; i >= 0; i--) {
                timeSet.add(format.format(new Date(System.currentTimeMillis() - i * DAY_SIZE)));
            }
        } else if (dayCount < DEFAULT_DAY) {
            for (long i = 0; i <= DEFAULT_DAY; i++) {
                timeSet.add(format.format(new Date(summaryStatistics.getMin() + i * DAY_SIZE)));
            }
        } else {
            for (long i = DEFAULT_DAY; i >= 0; i--) {
                timeSet.add(format.format(new Date(summaryStatistics.getMax() - i * DAY_SIZE)));
            }
        }
    }

    private static void setCountOfMonth(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        LongSummaryStatistics summaryStatistics = defectLineList.stream().map(x -> {
            try {
                return format.parse(x.getDefectTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));
        final int DEFAULT_MONTH = 12;
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(new Date(summaryStatistics.getMin()));
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(new Date(summaryStatistics.getMax()));
        int monthCount = endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);

        if (defectLineList.isEmpty()) {
            startCalendar.setTime(new Date());
            startCalendar.add(Calendar.MONTH, -DEFAULT_MONTH);
            for (long i = DEFAULT_MONTH; i >= 0; i--) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else if (monthCount < DEFAULT_MONTH) {
            timeSet.add(format.format(startCalendar.getTime()));
            for (int i = 1; i < DEFAULT_MONTH; i++) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else {
            endCalendar.add(Calendar.MONTH, -DEFAULT_MONTH);
            timeSet.add(format.format(endCalendar.getTime()));
            for (int i = 1; i <= DEFAULT_MONTH; i++) {
                endCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(endCalendar.getTime()));
            }
        }
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

        final long DAY_SIZE = 1000 * 60 * 60 * 24;
        final long DEFAULT_DAY = 30;
        long dayCount = (summaryStatistics.getMax() - summaryStatistics.getMin()) / DAY_SIZE;
        if (list.isEmpty()) {
            for (long i = DEFAULT_DAY; i >= 0; i--) {
                timeSet.add(format.format(new Date(System.currentTimeMillis() - i * DAY_SIZE)));
            }
        } else if (dayCount < DEFAULT_DAY) {
            for (long i = 0; i <= DEFAULT_DAY; i++) {
                timeSet.add(format.format(new Date(summaryStatistics.getMin() + i * DAY_SIZE)));
            }
        } else {
            for (long i = DEFAULT_DAY; i >= 0; i--) {
                timeSet.add(format.format(new Date(summaryStatistics.getMax() - i * DAY_SIZE)));
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

        final int DEFAULT_MONTH = 12;
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(new Date(summaryStatistics.getMin()));
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(new Date(summaryStatistics.getMax()));
        int monthCount = endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);

        if (list.isEmpty()) {
            startCalendar.setTime(new Date());
            startCalendar.add(Calendar.MONTH, -DEFAULT_MONTH);
            for (long i = DEFAULT_MONTH; i >= 0; i--) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else if (monthCount < DEFAULT_MONTH) {
            timeSet.add(format.format(startCalendar.getTime()));
            for (int i = 1; i < DEFAULT_MONTH; i++) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else {
            endCalendar.add(Calendar.MONTH, -DEFAULT_MONTH);
            timeSet.add(format.format(endCalendar.getTime()));
            for (int i = 1; i <= DEFAULT_MONTH; i++) {
                endCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(endCalendar.getTime()));
            }
        }
    }
}
