package com.cat2bug.quarkus.common.domain;

public class SysUserDefect {

    private Long userDefectId;
    private Long defectId;
    private Long userId;
    private Integer collect;

    public Long getUserDefectId() {
        return userDefectId;
    }

    public void setUserDefectId(Long userDefectId) {
        this.userDefectId = userDefectId;
    }

    public Long getDefectId() {
        return defectId;
    }

    public void setDefectId(Long defectId) {
        this.defectId = defectId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getCollect() {
        return collect;
    }

    public void setCollect(Integer collect) {
        this.collect = collect;
    }
}
