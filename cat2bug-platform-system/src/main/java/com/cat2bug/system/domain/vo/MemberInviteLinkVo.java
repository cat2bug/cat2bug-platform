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
    private Long[] roleIds;
    private Date expiresAt;
    private Boolean revoked;
    private Date createdAt;
}
