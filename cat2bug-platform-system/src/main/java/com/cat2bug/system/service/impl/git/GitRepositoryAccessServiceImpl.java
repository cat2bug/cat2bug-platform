package com.cat2bug.system.service.impl.git;

import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于项目角色 role_key 的 Git 仓库权限判定实现。
 *
 * <p>刻意不检查 {@code SysUser#isAdmin()} / {@code team.admin} / {@code team.create-by}：
 * design.md 明确「超管与团队管理员不绕过」，只有目标项目下的四个项目角色才授予 pull/push，
 * 其中两个（create-by/admin）才授予 manage/force-push。</p>
 */
@Service
public class GitRepositoryAccessServiceImpl implements GitRepositoryAccessService
{
    /** pull + push：design.md D3 「pull + push」一行 */
    private static final Set<String> PULL_PUSH_ROLE_KEYS = new HashSet<>(Arrays.asList(
            "project.create-by", "project.admin", "project.develop", "project.tester"));

    /** manage（含 force-push）：design.md D3 「manage」一行 */
    private static final Set<String> MANAGE_ROLE_KEYS = new HashSet<>(Arrays.asList(
            "project.create-by", "project.admin"));

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public boolean canPull(Long userId, Long projectId)
    {
        return hasAnyRoleKey(userId, projectId, PULL_PUSH_ROLE_KEYS);
    }

    @Override
    public boolean canPush(Long userId, Long projectId)
    {
        return hasAnyRoleKey(userId, projectId, PULL_PUSH_ROLE_KEYS);
    }

    @Override
    public boolean canManage(Long userId, Long projectId)
    {
        return hasAnyRoleKey(userId, projectId, MANAGE_ROLE_KEYS);
    }

    /**
     * 用户在目标项目下持有的角色（不含团队角色：teamId 传 null，见 SysRoleMapper.xml
     * selectTeamAndProjectRoleVo，团队 UNION 分支仅在 teamId 非空时生效）。
     */
    private boolean hasAnyRoleKey(Long userId, Long projectId, Set<String> allowedRoleKeys)
    {
        if (userId == null || projectId == null)
        {
            return false;
        }
        List<SysRole> roles = sysRoleMapper.selectRolePermissionByUserId(null, projectId, userId);
        if (CollectionUtils.isEmpty(roles))
        {
            return false;
        }
        for (SysRole role : roles)
        {
            if (role != null && allowedRoleKeys.contains(role.getRoleKey()))
            {
                return true;
            }
        }
        return false;
    }
}
