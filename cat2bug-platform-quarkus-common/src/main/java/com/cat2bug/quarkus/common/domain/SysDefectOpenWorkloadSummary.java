package com.cat2bug.quarkus.common.domain;

/**
 * 当前用户未关闭待办汇总
 */
public class SysDefectOpenWorkloadSummary {

    private int total;
    private int processing;
    private int audit;
    private int rejected;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getProcessing() {
        return processing;
    }

    public void setProcessing(int processing) {
        this.processing = processing;
    }

    public int getAudit() {
        return audit;
    }

    public void setAudit(int audit) {
        this.audit = audit;
    }

    public int getRejected() {
        return rejected;
    }

    public void setRejected(int rejected) {
        this.rejected = rejected;
    }
}
