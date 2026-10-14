package com.cat2bug.system.service.impl.git;
import jakarta.inject.Inject;

import jakarta.enterprise.context.ApplicationScoped;

import com.cat2bug.common.constant.SecurityConstants;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysGitRepository;
import com.cat2bug.common.domain.SysGitRepositoryMember;
import com.cat2bug.common.domain.git.GitRepositoryAclRoles;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMemberMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.common.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 仓级 ACL 优先，否则回落项目角色的 Git 仓库权限判定。
 *
 * <p>系统超级管理员拥有全部仓库的完整权限；不检查 {@code team.admin} / {@code team.create-by}：
 * 其余用户只有 ACL 行或目标项目下的四个项目角色才授予权限。</p>
 */
@ApplicationScoped
public class GitRepositoryAccessServiceImpl implements GitRepositoryAccessService
{
    /** pull + push：项目角色回落 */
    private static final Set<String> PULL_PUSH_ROLE_KEYS = new HashSet<>(Arrays.asList(
            "project.create-by", "project.admin", "project.develop", "project.tester"));

    /** manage（含 force-push）：项目角色回落 */
    private static final Set<String> MANAGE_ROLE_KEYS = new HashSet<>(Arrays.asList(
            "project.create-by", "project.admin"));

    @Inject
    private SysRoleMapper sysRoleMapper;

    @Inject
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Inject
    private SysGitRepositoryMemberMapper sysGitRepositoryMemberMapper;

    @Override
    public boolean canPull(Long userId, Long repositoryId)
    {
        AccessFlags flags = evaluate(userId, repositoryId);
        return flags.canPull;
    }

    @Override
    public boolean canPush(Long userId, Long repositoryId)
    {
        AccessFlags flags = evaluate(userId, repositoryId);
        return flags.canPush;
    }

    @Override
    public boolean canManage(Long userId, Long repositoryId)
    {
        AccessFlags flags = evaluate(userId, repositoryId);
        return flags.canManage;
    }

    @Override
    public String resolveEffectiveRole(Long userId, Long repositoryId)
    {
        return evaluate(userId, repositoryId).role;
    }

    @Override
    public boolean isSystemAdmin(Long userId)
    {
        if (userId == null)
        {
            return false;
        }
        if (SysUser.isAdmin(userId))
        {
            return true;
        }
        List<SysRole> roles = sysRoleMapper.selectRolePermissionByUserId(null, null, userId);
        if (CollectionUtils.isEmpty(roles))
        {
            return false;
        }
        return roles.stream().anyMatch(r -> r != null && SecurityConstants.SUPER_ADMIN.equals(r.getRoleKey()));
    }

    private AccessFlags evaluate(Long userId, Long repositoryId)
    {
        if (userId == null || repositoryId == null)
        {
            return AccessFlags.DENIED;
        }
        if (isSystemAdmin(userId))
        {
            return sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId) == null
                    ? AccessFlags.DENIED
                    : new AccessFlags(GitRepositoryAclRoles.ADMIN, true, true, true);
        }

        SysGitRepositoryMember acl = sysGitRepositoryMemberMapper
                .selectByRepositoryIdAndUserId(repositoryId, userId);
        if (acl != null && acl.getRole() != null)
        {
            return flagsFromAclRole(acl.getRole());
        }

        SysGitRepository repo = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repo == null || repo.getProjectId() == null)
        {
            return AccessFlags.DENIED;
        }
        return flagsFromProjectRoles(userId, repo.getProjectId());
    }

    private static AccessFlags flagsFromAclRole(String role)
    {
        if (GitRepositoryAclRoles.ADMIN.equals(role))
        {
            return new AccessFlags(GitRepositoryAclRoles.ADMIN, true, true, true);
        }
        if (GitRepositoryAclRoles.WRITE.equals(role))
        {
            return new AccessFlags(GitRepositoryAclRoles.WRITE, true, true, false);
        }
        if (GitRepositoryAclRoles.READ.equals(role))
        {
            return new AccessFlags(GitRepositoryAclRoles.READ, true, false, false);
        }
        return AccessFlags.DENIED;
    }

    private AccessFlags flagsFromProjectRoles(Long userId, Long projectId)
    {
        List<SysRole> roles = sysRoleMapper.selectRolePermissionByUserId(null, projectId, userId);
        if (CollectionUtils.isEmpty(roles))
        {
            return AccessFlags.DENIED;
        }
        boolean pullPush = false;
        boolean manage = false;
        for (SysRole role : roles)
        {
            if (role == null || role.getRoleKey() == null)
            {
                continue;
            }
            if (MANAGE_ROLE_KEYS.contains(role.getRoleKey()))
            {
                manage = true;
                pullPush = true;
            }
            else if (PULL_PUSH_ROLE_KEYS.contains(role.getRoleKey()))
            {
                pullPush = true;
            }
        }
        if (manage)
        {
            return new AccessFlags(GitRepositoryAclRoles.ADMIN, true, true, true);
        }
        if (pullPush)
        {
            return new AccessFlags(GitRepositoryAclRoles.WRITE, true, true, false);
        }
        return AccessFlags.DENIED;
    }

    private static final class AccessFlags
    {
        static final AccessFlags DENIED = new AccessFlags(null, false, false, false);

        final String role;
        final boolean canPull;
        final boolean canPush;
        final boolean canManage;

        AccessFlags(String role, boolean canPull, boolean canPush, boolean canManage)
        {
            this.role = role;
            this.canPull = canPull;
            this.canPush = canPush;
            this.canManage = canManage;
        }
    }
}
