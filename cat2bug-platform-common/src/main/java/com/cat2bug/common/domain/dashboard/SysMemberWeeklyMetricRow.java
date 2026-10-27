package com.cat2bug.common.domain.dashboard;

/**
 * Mapper 中间行：某成员在某一维上的本周计数（缺陷或文档）。
 */
public class SysMemberWeeklyMetricRow {

    private Long userId;
    private String nickName;
    private String userName;
    private String avatar;
    private int metricCount;

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

    public int getMetricCount() {
        return metricCount;
    }

    public void setMetricCount(int metricCount) {
        this.metricCount = metricCount;
    }
}
