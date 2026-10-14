package com.cat2bug.common.domain.dashboard;

public class SysDefectStatistics {

    private int total;
    private int pendingCount;
    private int verifyCount;
    private int closedCount;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public int getVerifyCount() {
        return verifyCount;
    }

    public void setVerifyCount(int verifyCount) {
        this.verifyCount = verifyCount;
    }

    public int getClosedCount() {
        return closedCount;
    }

    public void setClosedCount(int closedCount) {
        this.closedCount = closedCount;
    }
}
