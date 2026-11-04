package com.cat2bug.system.util;

import com.cat2bug.common.constant.DefectLogState;
import com.cat2bug.common.domain.SysDefectLog;

import java.util.Date;
import java.util.List;

/**
 * 按缺陷日志计算本周期存活起止与驳回次数。
 * <p>锚点：最近 CREATE/OPEN；终点：锚点后最晚 PASS/CLOSED，无则 {@code now}；
 * 驳回：锚点后 REJECTED 条数。
 */
public final class DefectCycleMetricsCalculator {

    private DefectCycleMetricsCalculator() {
    }

    public static DefectCycleMetrics compute(List<SysDefectLog> logs, Date createTimeFallback, Date now) {
        Date clock = now != null ? now : new Date();
        Date start = latestAnchor(logs);
        if (start == null) {
            start = createTimeFallback != null ? createTimeFallback : clock;
        }
        Date end = latestCloseAfter(logs, start);
        if (end == null) {
            end = clock;
        }
        int rejectCount = countRejectedAfter(logs, start);
        return new DefectCycleMetrics(start, end, rejectCount);
    }

    private static Date latestAnchor(List<SysDefectLog> logs) {
        Date best = null;
        if (logs == null) {
            return null;
        }
        for (SysDefectLog log : logs) {
            if (log == null || log.getCreateTime() == null) {
                continue;
            }
            DefectLogState type = log.getDefectLogType();
            if (type != DefectLogState.CREATE && type != DefectLogState.OPEN) {
                continue;
            }
            if (best == null || log.getCreateTime().after(best)) {
                best = log.getCreateTime();
            }
        }
        return best;
    }

    private static Date latestCloseAfter(List<SysDefectLog> logs, Date start) {
        Date best = null;
        if (logs == null || start == null) {
            return null;
        }
        for (SysDefectLog log : logs) {
            if (log == null || log.getCreateTime() == null) {
                continue;
            }
            DefectLogState type = log.getDefectLogType();
            if (type != DefectLogState.PASS && type != DefectLogState.CLOSED) {
                continue;
            }
            if (log.getCreateTime().before(start)) {
                continue;
            }
            if (best == null || log.getCreateTime().after(best)) {
                best = log.getCreateTime();
            }
        }
        return best;
    }

    private static int countRejectedAfter(List<SysDefectLog> logs, Date start) {
        if (logs == null || start == null) {
            return 0;
        }
        int count = 0;
        for (SysDefectLog log : logs) {
            if (log == null || log.getCreateTime() == null) {
                continue;
            }
            if (log.getDefectLogType() != DefectLogState.REJECTED) {
                continue;
            }
            if (log.getCreateTime().before(start)) {
                continue;
            }
            count++;
        }
        return count;
    }
}
