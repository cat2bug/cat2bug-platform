package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 成员邀请链接 sys_member_invite
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class SysMemberInvite extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 邀请 id */
    private Long inviteId;

    /** URL token */
    private String token;

    /** TEAM | PROJECT */
    private String scope;

    /** 团队 id */
    private Long teamId;

    /** 项目 id（项目邀请必填） */
    private Long projectId;

    /** 角色 id JSON 数组 */
    private String roleIds;

    /** 过期时间 */
    private Date expiresAt;

    /** 是否撤销 0/1 */
    private Integer revoked;

    /** 创建人 user_id */
    private Long createdBy;

    /** 创建时间 */
    private Date createdAt;
}
