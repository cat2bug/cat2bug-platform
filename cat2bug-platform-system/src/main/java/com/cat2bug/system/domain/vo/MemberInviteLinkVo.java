package com.cat2bug.system.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * 成员邀请链接响应
 */
@Data
public class MemberInviteLinkVo {

    private Long inviteId;
    private String token;
    private String inviteUrl;
    private String scope;
    private Long teamId;
    private Long projectId;
    /** 仓库 id（scope=REPO） */
    private Long repositoryId;
    /** 团队/项目：sys_role id 数组 */
    private Long[] roleIds;
    /** 仓库 ACL 角色字面量（read/write/admin） */
    private String role;
    private Date expiresAt;
    private Boolean revoked;
    private Date createdAt;
}
