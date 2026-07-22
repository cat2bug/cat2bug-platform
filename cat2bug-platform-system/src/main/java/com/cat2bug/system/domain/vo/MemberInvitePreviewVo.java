package com.cat2bug.system.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 邀请链接预览（公开）
 */
@Data
public class MemberInvitePreviewVo {

    private String scope;
    private String teamName;
    private String projectName;
    /** 仓库显示名（scope=REPO） */
    private String repositoryName;
    private String inviterName;
    private List<String> roleNames;
    private Date expiresAt;
    /** valid | expired | revoked */
    private String status;
    private Boolean expired;
    private Boolean revoked;
    private Boolean valid;
    /** 当前登录用户是否已是目标团队/项目/仓库成员（未登录时为 false） */
    private Boolean alreadyMember;
}
