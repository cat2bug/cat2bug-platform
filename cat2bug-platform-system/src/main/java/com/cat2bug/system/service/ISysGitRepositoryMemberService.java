package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.common.domain.SysGitRepository;
import com.cat2bug.common.domain.SysGitRepositoryMember;
import com.cat2bug.common.domain.git.GitRepoMembersFromRepoResult;
import com.cat2bug.common.domain.git.GitRepoMyAccess;

/**
 * 仓库成员 ACL 服务
 */
public interface ISysGitRepositoryMemberService
{
    /**
     * 列出仓库 ACL 成员（需 effective manage）
     */
    List<SysGitRepositoryMember> selectMembersByRepositoryId(Long repositoryId);

    /**
     * 查询单条 ACL
     */
    SysGitRepositoryMember selectByRepositoryIdAndUserId(Long repositoryId, Long userId);

    /**
     * 添加成员（可邀请非项目成员）；拒绝重复；需调用方 effective manage
     */
    int insertMember(Long repositoryId, Long userId, String role);

    /**
     * 由有效邀请链接接受时写入 ACL；不做 effective manage 校验（调用方须已验证 token）。
     * 校验用户存在、角色合法，拒绝重复。
     */
    int insertMemberFromInvite(Long repositoryId, Long userId, String role);

    /**
     * 修改角色；拒绝导致零 admin 的降权
     */
    int updateMemberRole(Long repositoryId, Long userId, String role);

    /**
     * 移除成员；拒绝删除最后一位 admin
     */
    int deleteMember(Long repositoryId, Long userId);

    /**
     * 项目成员移除路径：列出该用户在本项目各仓的 ACL（不做仓 manage 校验）。
     */
    List<SysGitRepositoryMember> listAclByProjectMember(Long projectId, Long userId);

    /**
     * 用户是否为该仓最后一位 admin（不做 manage 校验）。
     */
    boolean isLastAdmin(Long repositoryId, Long userId);

    /**
     * 项目成员移除路径：同步清理该用户在本项目各仓 ACL。
     * 若某仓其为最后 admin，须提供 handoffUserId，先确保交接人为 admin 再删被删用户；不做仓 manage 校验。
     */
    void syncRemoveOnProjectMemberRemoval(Long projectId, Long memberId, Long handoffUserId);

    /**
     * 创建/导入成功后写入创建者 admin（幂等）
     */
    void ensureCreatorAdmin(Long repositoryId, Long userId);

    /**
     * 当前用户有效权限（仓库须存在）
     */
    GitRepoMyAccess getMyAccess(Long repositoryId);

    /**
     * 可作为「从仓邀请」源的仓库：同 projectId、排除本仓、调用方对该源仓 effective manage。
     * 调用方须对目标仓 effective manage。
     */
    List<SysGitRepository> listInviteSources(Long repositoryId);

    /**
     * 列出源仓 ACL 成员（供勾选）。目标仓与源仓均须 effective manage，且同项目、非本仓。
     */
    List<SysGitRepositoryMember> listInviteSourceMembers(Long repositoryId, Long sourceRepositoryId);

    /**
     * 从源仓批量写入目标仓 ACL。已是目标仓成员的用户跳过；返回 added/skipped。
     */
    GitRepoMembersFromRepoResult insertMembersFromRepo(Long repositoryId, Long sourceRepositoryId,
            List<Long> userIds, String role);
}
