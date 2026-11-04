package com.cat2bug.system.util;

import com.cat2bug.common.constant.DefectLogState;
import com.cat2bug.common.domain.SysDefectLog;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefectCycleMetricsCalculatorTest {

    @Test
    void inProgressUsesNowAsEndAndNonZeroLife() {
        Date create = minutesAgo(90);
        Date now = new Date();
        SysDefectLog createLog = log(DefectLogState.CREATE, create);

        DefectCycleMetrics metrics = DefectCycleMetricsCalculator.compute(List.of(createLog), create, now);

        assertEquals(create, metrics.getHandleStartTime());
        assertEquals(now, metrics.getHandlePassTime());
        assertTrue(metrics.getHandlePassTime().getTime() - metrics.getHandleStartTime().getTime()
                >= TimeUnit.MINUTES.toMillis(89));
        assertEquals(0, metrics.getRejectCount());
    }

    @Test
    void freezesAtLatestPassOrClose() {
        Date create = minutesAgo(120);
        Date pass = minutesAgo(60);
        Date close = minutesAgo(30);
        Date now = new Date();

        DefectCycleMetrics metrics = DefectCycleMetricsCalculator.compute(
                List.of(
                        log(DefectLogState.CREATE, create),
                        log(DefectLogState.PASS, pass),
                        log(DefectLogState.CLOSED, close)),
                create,
                now);

        assertEquals(close, metrics.getHandlePassTime());
        assertEquals(create, metrics.getHandleStartTime());
    }

    @Test
    void openResetsSurvivalAndRejectCount() {
        Date create = minutesAgo(200);
        Date reject1 = minutesAgo(180);
        Date close = minutesAgo(150);
        Date open = minutesAgo(100);
        Date reject2 = minutesAgo(50);
        Date now = new Date();

        DefectCycleMetrics metrics = DefectCycleMetricsCalculator.compute(
                List.of(
                        log(DefectLogState.CREATE, create),
                        log(DefectLogState.REJECTED, reject1),
                        log(DefectLogState.CLOSED, close),
                        log(DefectLogState.OPEN, open),
                        log(DefectLogState.REJECTED, reject2)),
                create,
                now);

        assertEquals(open, metrics.getHandleStartTime());
        assertEquals(now, metrics.getHandlePassTime());
        assertEquals(1, metrics.getRejectCount());
    }

    @Test
    void countsOnlyRejectedNotRejectOrdinal() {
        Date create = minutesAgo(60);
        Date now = new Date();

        DefectCycleMetrics metrics = DefectCycleMetricsCalculator.compute(
                List.of(
                        log(DefectLogState.CREATE, create),
                        log(DefectLogState.REJECT, minutesAgo(40)),
                        log(DefectLogState.REJECTED, minutesAgo(30)),
                        log(DefectLogState.REJECTED, minutesAgo(20))),
                create,
                now);

        assertEquals(2, metrics.getRejectCount());
    }

    @Test
    void fallsBackToCreateTimeWhenNoAnchorLog() {
        Date create = minutesAgo(45);
        Date now = new Date();

        DefectCycleMetrics metrics = DefectCycleMetricsCalculator.compute(List.of(), create, now);

        assertEquals(create, metrics.getHandleStartTime());
        assertEquals(now, metrics.getHandlePassTime());
    }

    private static Date minutesAgo(long minutes) {
        return new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes));
    }

    private static SysDefectLog log(DefectLogState type, Date time) {
        SysDefectLog log = new SysDefectLog();
        log.setDefectLogType(type);
        log.setCreateTime(time);
        return log;
    }
}
