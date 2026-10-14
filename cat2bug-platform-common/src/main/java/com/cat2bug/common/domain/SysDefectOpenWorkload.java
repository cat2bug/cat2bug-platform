package com.cat2bug.common.domain;

/**
 * 成员未关闭待办负载（团队排行）
 */
public class SysDefectOpenWorkload {

    private Long userId;
    private String nickName;
    private String avatar;
    private int total;
    private int processing;
    private int audit;
    private int rejected;

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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

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
