package com.cat2bug.quarkus.common.domain;

/**
 * 按日缺陷日志参与统计
 */
public class SysDefectParticipationDay {

    /** yyyy-MM-dd */
    private String date;
    private int count;

    public SysDefectParticipationDay() {
    }

    public SysDefectParticipationDay(String date, int count) {
        this.date = date;
        this.count = count;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
