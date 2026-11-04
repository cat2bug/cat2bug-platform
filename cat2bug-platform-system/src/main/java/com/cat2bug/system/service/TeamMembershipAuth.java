package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * 正式团队成员校验（{@code sys_user_team}）；系统超管豁免。
 */
@ApplicationScoped
public class TeamMembershipAuth {

    @Inject
    SysUserTeamService sysUserTeamService;

    public boolean isSystemAdmin(Long userId) {
        return SysUser.isAdmin(userId);
    }

    public boolean isFormalTeamMember(Long userId, Long teamId) {
        if (userId == null || teamId == null || teamId <= 0) {
            return false;
        }
        if (isSystemAdmin(userId)) {
            return true;
        }
        return sysUserTeamService.isFormalMember(teamId, userId);
    }

    public void requireFormalTeamMember(Long userId, Long teamId) {
        if (!isFormalTeamMember(userId, teamId)) {
            throw new ApiException(403, "仅团队正式成员可执行此操作");
        }
    }
}
