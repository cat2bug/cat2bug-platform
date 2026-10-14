package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 测试计划倒计时统计块所需字段
 */
public class SysPlanCountdownSummary {

    private String planId;
    private String planName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planEndTime;

    private int itemTotal;
    private int unexecutedCount;
    private int passCount;
    private int defectCount;
    private int defectCloseStateCount;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Date getPlanEndTime() {
        return planEndTime;
    }

    public void setPlanEndTime(Date planEndTime) {
        this.planEndTime = planEndTime;
    }

    public int getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(int itemTotal) {
        this.itemTotal = itemTotal;
    }

    public int getUnexecutedCount() {
        return unexecutedCount;
    }

    public void setUnexecutedCount(int unexecutedCount) {
        this.unexecutedCount = unexecutedCount;
    }

    public int getPassCount() {
        return passCount;
    }

    public void setPassCount(int passCount) {
        this.passCount = passCount;
    }

    public int getDefectCount() {
        return defectCount;
    }

    public void setDefectCount(int defectCount) {
        this.defectCount = defectCount;
    }

    public int getDefectCloseStateCount() {
        return defectCloseStateCount;
    }

    public void setDefectCloseStateCount(int defectCloseStateCount) {
        this.defectCloseStateCount = defectCloseStateCount;
    }
}
