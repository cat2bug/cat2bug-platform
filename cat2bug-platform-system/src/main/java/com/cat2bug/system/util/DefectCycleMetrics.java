package com.cat2bug.system.util;

import java.util.Date;

/** 缺陷本周期存活/驳回派生结果（不落库）。 */
public final class DefectCycleMetrics {

    private final Date handleStartTime;
    private final Date handlePassTime;
    private final int rejectCount;

    public DefectCycleMetrics(Date handleStartTime, Date handlePassTime, int rejectCount) {
        this.handleStartTime = handleStartTime;
        this.handlePassTime = handlePassTime;
        this.rejectCount = rejectCount;
    }

    public Date getHandleStartTime() {
        return handleStartTime;
    }

    public Date getHandlePassTime() {
        return handlePassTime;
    }

    public int getRejectCount() {
        return rejectCount;
    }
}
