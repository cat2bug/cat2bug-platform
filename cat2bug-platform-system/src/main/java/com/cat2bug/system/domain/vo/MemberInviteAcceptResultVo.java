package com.cat2bug.system.domain.vo;

import lombok.Data;

/**
 * 接受邀请结果
 */
@Data
public class MemberInviteAcceptResultVo {

    private String scope;
    private Long teamId;
    private Long projectId;
    private Boolean alreadyMember;
    private String message;
}
