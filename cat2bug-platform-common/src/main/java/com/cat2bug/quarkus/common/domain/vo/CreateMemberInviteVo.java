package com.cat2bug.quarkus.common.domain.vo;

/**
 * 创建成员邀请链接请求
 */
public class CreateMemberInviteVo {

    private Long[] roleIds;

    public Long[] getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(Long[] roleIds) {
        this.roleIds = roleIds;
    }
}
