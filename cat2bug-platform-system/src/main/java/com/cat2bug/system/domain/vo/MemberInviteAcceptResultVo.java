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
    /** 仓库 id（scope=REPO） */
    private Long repositoryId;
    /**
     * 成功后建议跳转路径（如 {@code /{teamKey}/{projectKey}/code/{slug}}）；
     * 键或 slug 缺失时为 null。
     */
    private String redirectPath;
    private Boolean alreadyMember;
    private String message;
}
