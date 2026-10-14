package com.cat2bug.common.domain.dashboard;

public class SysMemberRankOfDefects {

    private Long userId;
    private String nickName;
    private String userName;
    private String avatar;
    private int defectTotal;
    private int defectTodayCount;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getDefectTotal() {
        return defectTotal;
    }

    public void setDefectTotal(int defectTotal) {
        this.defectTotal = defectTotal;
    }

    public int getDefectTodayCount() {
        return defectTodayCount;
    }

    public void setDefectTodayCount(int defectTodayCount) {
        this.defectTodayCount = defectTodayCount;
    }
}
