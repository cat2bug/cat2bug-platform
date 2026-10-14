package com.cat2bug.quarkus.common.domain.dashboard;

public class SysDefectLine {

    private long defectCount;
    private String defectTime;
    private int defectState;

    public long getDefectCount() {
        return defectCount;
    }

    public void setDefectCount(long defectCount) {
        this.defectCount = defectCount;
    }

    public String getDefectTime() {
        return defectTime;
    }

    public void setDefectTime(String defectTime) {
        this.defectTime = defectTime;
    }

    public int getDefectState() {
        return defectState;
    }

    public void setDefectState(int defectState) {
        this.defectState = defectState;
    }
}
