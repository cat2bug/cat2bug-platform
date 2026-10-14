package com.cat2bug.common.domain.open;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Open API 成员视图，对齐 Spring {@code com.cat2bug.api.domain.ApiMember}。
 */
public class ApiMember {

    @JsonIgnore
    private Long memberId;
    private String memberAccount;
    private String memberName;
    private String email;
    private String phoneNumber;
    private String avatar;
    private List<String> roleNameList;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getMemberAccount() {
        return memberAccount;
    }

    public void setMemberAccount(String memberAccount) {
        this.memberAccount = memberAccount;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public List<String> getRoleNameList() {
        return roleNameList;
    }

    public void setRoleNameList(List<String> roleNameList) {
        this.roleNameList = roleNameList;
    }
}
