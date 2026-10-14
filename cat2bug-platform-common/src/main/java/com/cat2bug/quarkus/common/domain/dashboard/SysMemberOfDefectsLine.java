package com.cat2bug.quarkus.common.domain.dashboard;

public class SysMemberOfDefectsLine {

    private String nickName;
    private String userName;
    private String avatar;
    private Long userId;
    private String createTime;
    private int defectTodayCount;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public int getDefectTodayCount() {
        return defectTodayCount;
    }

    public void setDefectTodayCount(int defectTodayCount) {
        this.defectTodayCount = defectTodayCount;
    }
}
