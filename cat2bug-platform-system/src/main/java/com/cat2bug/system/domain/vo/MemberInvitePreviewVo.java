package com.cat2bug.system.domain.vo;

import lombok.Data;

/**
 * 邀请链接预览（公开）
 */
@Data
public class MemberInvitePreviewVo {

    private String scope;
    private String teamName;
    private String projectName;
    private Boolean expired;
    private Boolean revoked;
    private Boolean valid;
}
