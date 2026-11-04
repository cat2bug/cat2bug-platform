package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysUserTeam;
import com.cat2bug.common.domain.SysUserTeamRole;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.system.mapper.SysUserTeamMapper;
import com.cat2bug.system.mapper.SysUserTeamRoleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SysUserTeamService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 团队创建人 team.create-by */
    private static final Long TEAM_CREATE_BY_ROLE_ID = 12L;
    /** 团队管理员 team.admin */
    private static final Long TEAM_ADMIN_ROLE_ID = 4L;

    @Inject
    SysUserTeamMapper sysUserTeamMapper;

    @Inject
    SysUserTeamRoleMapper sysUserTeamRoleMapper;

    public int updateSysUserTeamByTeamIdAndMemberId(Long teamId, Long memberId, SysUserTeam sysUserTeam) {
        sysUserTeam.setTeamId(teamId);
        sysUserTeam.setUserId(memberId);
        sysUserTeam.setUpdateTime(LocalDateTime.now().format(TIME_FMT));
        return sysUserTeamMapper.updateSysUserTeamByTeamIdAndMemberId(sysUserTeam);
    }

    public boolean isFormalMember(Long teamId, Long userId) {
        if (teamId == null || userId == null) {
            return false;
        }
        return sysUserTeamMapper.countFormalMember(teamId, userId) > 0;
    }

    public boolean isTeamCreator(Long teamId, Long userId) {
        if (teamId == null || userId == null) {
            return false;
        }
        return sysUserTeamRoleMapper.countByTeamUserAndRole(teamId, userId, TEAM_CREATE_BY_ROLE_ID) > 0;
    }

    public Map<String, Object> membershipInfo(Long teamId, Long userId) {
        Map<String, Object> body = new HashMap<>();
        boolean formal = isFormalMember(teamId, userId);
        body.put("formalMember", formal);
        body.put("teamMember", formal);
        body.put("creator", formal && isTeamCreator(teamId, userId));
        return body;
    }

    /**
     * 删除团队成员：只拆队籍与团队角色，不碰项目成员。创建人不可删。
     */
    @Transactional
    public int deleteTeamMember(Long teamId, Long memberId) {
        if (teamId == null || memberId == null) {
            throw new ApiException(400, "团队或成员无效");
        }
        SysUserTeam row = findUserTeam(teamId, memberId);
        if (row == null || row.getUserTeamId() == null) {
            throw new ApiException(404, "团队中没有找到此成员");
        }
        if (isTeamCreator(teamId, memberId)) {
            throw new ApiException(400, "团队创建人不可删除，请先转让创建人角色");
        }
        sysUserTeamRoleMapper.deleteSysUserTeamByUserTeamId(row.getUserTeamId());
        int deleted = sysUserTeamMapper.deleteSysUserTeamByTeamIdAndUserId(teamId, memberId);
        if (deleted != 1) {
            throw new ApiException("删除团队成员失败");
        }
        return deleted;
    }

    /**
     * 转让团队创建人：仅当前创建人可调用；目标须为正式队员。
     */
    @Transactional
    public int transferCreator(Long teamId, Long operatorId, Long targetUserId) {
        if (teamId == null || operatorId == null || targetUserId == null) {
            throw new ApiException(400, "参数无效");
        }
        if (operatorId.equals(targetUserId)) {
            throw new ApiException(400, "不能转让给自己");
        }
        if (!isTeamCreator(teamId, operatorId)) {
            throw new ApiException(403, "仅团队创建人可转让");
        }
        SysUserTeam target = findUserTeam(teamId, targetUserId);
        if (target == null || target.getUserTeamId() == null) {
            throw new ApiException(400, "受让人须为本团队正式成员");
        }
        SysUserTeam operator = findUserTeam(teamId, operatorId);
        if (operator == null || operator.getUserTeamId() == null) {
            throw new ApiException(403, "仅团队创建人可转让");
        }

        // 受让人角色改为仅创建人（避免残留 team.default=5 等，前端创建人选项里解析不出名称）
        sysUserTeamRoleMapper.deleteSysUserTeamByUserTeamId(target.getUserTeamId());
        SysUserTeamRole creatorLink = new SysUserTeamRole();
        creatorLink.setUserTeamId(target.getUserTeamId());
        creatorLink.setRoleId(TEAM_CREATE_BY_ROLE_ID);
        if (sysUserTeamRoleMapper.insertSysUserTeamRole(creatorLink) != 1) {
            throw new ApiException("转让创建人失败");
        }

        // 原创建人去掉创建人角色；若无其他团队角色则落为团队管理员
        sysUserTeamRoleMapper.deleteByUserTeamIdAndRoleId(operator.getUserTeamId(), TEAM_CREATE_BY_ROLE_ID);
        List<Long> remain = sysUserTeamRoleMapper.selectRoleIdsByUserTeamId(operator.getUserTeamId());
        if (remain == null || remain.isEmpty()) {
            SysUserTeamRole admin = new SysUserTeamRole();
            admin.setUserTeamId(operator.getUserTeamId());
            admin.setRoleId(TEAM_ADMIN_ROLE_ID);
            if (sysUserTeamRoleMapper.insertSysUserTeamRole(admin) != 1) {
                throw new ApiException("转让后写入原创建人角色失败");
            }
        }
        return 1;
    }

    private SysUserTeam findUserTeam(Long teamId, Long userId) {
        SysUserTeam query = new SysUserTeam();
        query.setTeamId(teamId);
        query.setUserId(userId);
        List<SysUserTeam> rows = sysUserTeamMapper.selectSysUserTeamList(query);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }
}
