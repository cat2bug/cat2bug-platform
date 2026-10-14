package com.cat2bug.system.util;

import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.domain.dashboard.SysDefectLine;

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
 * 缺陷状态走势（与 Spring 仪表盘 defect-line 口径一致）
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
        Map<Integer, List<SysDefectLine>> defectStateGroup = defectLineList.stream()
                .collect(Collectors.groupingBy(SysDefectLine::getDefectState));
        Map<String, List<Long>> defectStateLines = new HashMap<>();
        defectStateGroup.forEach((stateOrdinal, rows) -> {
            String stateName = DefectConstants.DEFECT_STATES[stateOrdinal];
            defectStateLines.put(stateName, new ArrayList<>());
            for (String s : timeSet) {
                Optional<SysDefectLine> defectLine = rows.stream()
                        .filter(d -> d.getDefectTime().equals(s))
                        .findFirst();
                if (defectLine.isPresent()) {
                    defectStateLines.get(stateName).add(defectLine.get().getDefectCount());
                } else {
                    defectStateLines.get(stateName).add(0L);
                }
            }
        });
        Map<String, Object> ret = new HashMap<>();
        ret.put("data", defectStateLines);
        ret.put("types", DefectConstants.DEFECT_STATES);
        ret.put("times", timeSet);
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
        final long daySize = 1000L * 60 * 60 * 24;
        final long defaultDay = 30;
        long dayCount = (summaryStatistics.getMax() - summaryStatistics.getMin()) / daySize;

        if (defectLineList.isEmpty()) {
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

    private static void setCountOfMonth(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        LongSummaryStatistics summaryStatistics = defectLineList.stream().map(x -> {
            try {
                return format.parse(x.getDefectTime()).getTime();
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

        if (defectLineList.isEmpty()) {
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
