package com.cat2bug.system.service;

import com.cat2bug.system.domain.vo.CreateMemberInviteVo;
import com.cat2bug.system.domain.vo.MemberInviteAcceptResultVo;
import com.cat2bug.system.domain.vo.MemberInviteLinkVo;
import com.cat2bug.system.domain.vo.MemberInvitePreviewVo;

import java.util.List;

/**
 * 成员邀请链接 Service
 */
public interface ISysMemberInviteService {

    MemberInviteLinkVo createTeamInviteLink(Long teamId, CreateMemberInviteVo request, Long createdBy, String inviteBaseUrl);

    MemberInviteLinkVo createProjectInviteLink(Long projectId, CreateMemberInviteVo request, Long createdBy, String inviteBaseUrl);

    /**
     * 创建仓库 ACL 邀请链接（scope=REPO，固定 3 天有效；role_ids 存 ACL 字面量 JSON）。
     * 不自动吊销既有链接；UI「重新生成」应先 revoke 再 create。
     */
    MemberInviteLinkVo createRepoInviteLink(Long repositoryId, String aclRole, Long createdBy, String inviteBaseUrl);

    List<MemberInviteLinkVo> listActiveTeamInviteLinks(Long teamId, String inviteBaseUrl);

    List<MemberInviteLinkVo> listActiveProjectInviteLinks(Long projectId, String inviteBaseUrl);

    List<MemberInviteLinkVo> listActiveRepoInviteLinks(Long repositoryId, String inviteBaseUrl);

    int revokeTeamInviteLink(Long teamId, Long inviteId);

    int revokeProjectInviteLink(Long projectId, Long inviteId);

    int revokeRepoInviteLink(Long repositoryId, Long inviteId);

    MemberInvitePreviewVo preview(String token, Long userId);

    MemberInviteAcceptResultVo accept(String token, Long userId, String createBy);
}
