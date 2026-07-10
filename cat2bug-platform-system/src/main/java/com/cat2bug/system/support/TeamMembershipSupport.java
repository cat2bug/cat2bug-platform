package com.cat2bug.system.support;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysUserTeam;
import com.cat2bug.system.mapper.SysUserTeamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 校验当前用户是否为指定团队的正式成员（sys_user_team）。
 */
@Component
public class TeamMembershipSupport {

    @Autowired
    private SysUserTeamMapper sysUserTeamMapper;

    public boolean isTeamMember(Long userId, Long teamId) {
        if (userId == null || teamId == null || teamId <= 0) {
            return false;
        }
        SysUserTeam query = new SysUserTeam();
        query.setUserId(userId);
        query.setTeamId(teamId);
        List<SysUserTeam> rows = sysUserTeamMapper.selectSysUserTeamList(query);
        return rows != null && rows.stream().anyMatch(r -> r.getTeamLock() == null || r.getTeamLock() != 1);
    }

    public void requireTeamMember(Long userId, Long teamId) {
        if (!isTeamMember(userId, teamId)) {
            throw new ServiceException("非团队成员，无法执行此操作", HttpStatus.FORBIDDEN);
        }
    }

    public void requireTeamMember(Long userId, Long[] teamIds) {
        if (teamIds == null) {
            return;
        }
        for (Long teamId : teamIds) {
            requireTeamMember(userId, teamId);
        }
    }
}
