package com.cat2bug.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.uuid.UUID;
import com.cat2bug.system.domain.SysMemberInvite;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.domain.SysTeam;
import com.cat2bug.system.domain.SysUserProject;
import com.cat2bug.system.domain.SysUserTeam;
import com.cat2bug.system.domain.SysUserTeamRole;
import com.cat2bug.system.domain.type.MemberInviteScope;
import com.cat2bug.system.domain.vo.BatchUserRoleVo;
import com.cat2bug.system.domain.vo.CreateMemberInviteVo;
import com.cat2bug.system.domain.vo.MemberInviteAcceptResultVo;
import com.cat2bug.system.domain.vo.MemberInviteLinkVo;
import com.cat2bug.system.domain.vo.MemberInvitePreviewVo;
import com.cat2bug.system.mapper.SysMemberInviteMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysTeamMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.mapper.SysUserTeamMapper;
import com.cat2bug.system.mapper.SysUserTeamRoleMapper;
import com.cat2bug.system.service.ISysMemberInviteService;
import com.cat2bug.system.service.ISysUserProjectService;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成员邀请链接 Service 实现
 */
@Service
public class SysMemberInviteServiceImpl implements ISysMemberInviteService {

    private static final int INVITE_TTL_SECONDS = 3600;

    @Autowired
    private SysMemberInviteMapper sysMemberInviteMapper;

    @Autowired
    private SysTeamMapper sysTeamMapper;

    @Autowired
    private SysProjectMapper sysProjectMapper;

    @Autowired
    private SysUserTeamMapper sysUserTeamMapper;

    @Autowired
    private SysUserTeamRoleMapper sysUserTeamRoleMapper;

    @Autowired
    private SysUserProjectMapper sysUserProjectMapper;

    @Autowired
    private ISysUserProjectService sysUserProjectService;

    @Override
    public MemberInviteLinkVo createTeamInviteLink(Long teamId, CreateMemberInviteVo request, Long createdBy, String inviteBaseUrl) {
        Preconditions.checkNotNull(teamId, MessageUtils.message("team.team_not_empty"));
        Preconditions.checkNotNull(request, "roleIds is required");
        Preconditions.checkArgument(request.getRoleIds() != null && request.getRoleIds().length > 0,
                MessageUtils.message("team.role_not_empty"));
        Preconditions.checkNotNull(sysTeamMapper.selectSysTeamByTeamId(teamId), MessageUtils.message("team.team_not_empty"));
        return toLinkVo(insertInvite(MemberInviteScope.TEAM, teamId, null, request.getRoleIds(), createdBy), inviteBaseUrl);
    }

    @Override
    public MemberInviteLinkVo createProjectInviteLink(Long projectId, CreateMemberInviteVo request, Long createdBy, String inviteBaseUrl) {
        Preconditions.checkNotNull(projectId, MessageUtils.message("project.team_cannot_empty"));
        Preconditions.checkNotNull(request, "roleIds is required");
        Preconditions.checkArgument(request.getRoleIds() != null && request.getRoleIds().length > 0,
                MessageUtils.message("project.member_role_not_empty"));
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, createdBy);
        if (project == null) {
            project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
        }
        Preconditions.checkNotNull(project, MessageUtils.message("project.team_cannot_empty"));
        return toLinkVo(insertInvite(MemberInviteScope.PROJECT, project.getTeamId(), projectId, request.getRoleIds(), createdBy), inviteBaseUrl);
    }

    @Override
    public List<MemberInviteLinkVo> listActiveTeamInviteLinks(Long teamId, String inviteBaseUrl) {
        Date now = DateUtils.getNowDate();
        return sysMemberInviteMapper.selectActiveSysMemberInviteListByTeamId(teamId, now).stream()
                .map(invite -> toLinkVo(invite, inviteBaseUrl))
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberInviteLinkVo> listActiveProjectInviteLinks(Long projectId, String inviteBaseUrl) {
        Date now = DateUtils.getNowDate();
        return sysMemberInviteMapper.selectActiveSysMemberInviteListByProjectId(projectId, now).stream()
                .map(invite -> toLinkVo(invite, inviteBaseUrl))
                .collect(Collectors.toList());
    }

    @Override
    public int revokeTeamInviteLink(Long teamId, Long inviteId) {
        return sysMemberInviteMapper.revokeSysMemberInvite(inviteId, teamId, null);
    }

    @Override
    public int revokeProjectInviteLink(Long projectId, Long inviteId) {
        return sysMemberInviteMapper.revokeSysMemberInvite(inviteId, null, projectId);
    }

    @Override
    public MemberInvitePreviewVo preview(String token) {
        SysMemberInvite invite = requireInvite(token);
        MemberInvitePreviewVo preview = new MemberInvitePreviewVo();
        preview.setScope(invite.getScope());
        preview.setExpired(isExpired(invite));
        preview.setRevoked(isRevoked(invite));
        preview.setValid(!preview.getExpired() && !preview.getRevoked());

        SysTeam team = sysTeamMapper.selectSysTeamByTeamId(invite.getTeamId());
        if (team != null) {
            preview.setTeamName(team.getTeamName());
        }
        if (MemberInviteScope.PROJECT.equals(invite.getScope()) && invite.getProjectId() != null) {
            SysProject project = sysProjectMapper.selectSysProjectByProjectId(invite.getProjectId(), null);
            if (project != null) {
                preview.setProjectName(project.getProjectName());
            }
        }
        return preview;
    }

    @Override
    @Transactional
    public MemberInviteAcceptResultVo accept(String token, Long userId, String createBy) {
        SysMemberInvite invite = requireInvite(token);
        Preconditions.checkState(!isRevoked(invite), "邀请链接已撤销");
        Preconditions.checkState(!isExpired(invite), "邀请链接已过期");

        MemberInviteAcceptResultVo result = new MemberInviteAcceptResultVo();
        result.setScope(invite.getScope());
        result.setTeamId(invite.getTeamId());
        result.setProjectId(invite.getProjectId());

        Long[] roleIds = parseRoleIds(invite.getRoleIds());
        Preconditions.checkArgument(roleIds.length > 0, MessageUtils.message("team.role_not_empty"));

        if (MemberInviteScope.TEAM.equals(invite.getScope())) {
            return acceptTeamInvite(invite, userId, createBy, roleIds, result);
        }
        return acceptProjectInvite(invite, userId, roleIds, result);
    }

    private MemberInviteAcceptResultVo acceptTeamInvite(SysMemberInvite invite, Long userId, String createBy,
                                                        Long[] roleIds, MemberInviteAcceptResultVo result) {
        SysUserTeam query = new SysUserTeam();
        query.setTeamId(invite.getTeamId());
        query.setUserId(userId);
        List<SysUserTeam> existing = sysUserTeamMapper.selectSysUserTeamList(query);
        if (existing != null && !existing.isEmpty()) {
            result.setAlreadyMember(true);
            result.setMessage("已是团队成员");
            return result;
        }

        SysUserTeam sysUserTeam = new SysUserTeam();
        sysUserTeam.setUserId(userId);
        sysUserTeam.setTeamId(invite.getTeamId());
        sysUserTeam.setCreateTime(DateUtils.getNowDate());
        sysUserTeam.setCreateBy(createBy);
        Preconditions.checkState(sysUserTeamMapper.insertSysUserTeam(sysUserTeam) == 1,
                MessageUtils.message("team.insert_user_team_role_fail"));

        for (Long roleId : roleIds) {
            SysUserTeamRole role = new SysUserTeamRole();
            role.setUserTeamId(sysUserTeam.getUserTeamId());
            role.setRoleId(roleId);
            Preconditions.checkState(sysUserTeamRoleMapper.insertSysUserTeamRole(role) == 1,
                    MessageUtils.message("team.insert_member_role_fail"));
        }

        result.setAlreadyMember(false);
        result.setMessage("已成功加入团队");
        return result;
    }

    private MemberInviteAcceptResultVo acceptProjectInvite(SysMemberInvite invite, Long userId,
                                                           Long[] roleIds, MemberInviteAcceptResultVo result) {
        SysUserProject query = new SysUserProject();
        query.setProjectId(invite.getProjectId());
        query.setUserId(userId);
        List<SysUserProject> existing = sysUserProjectMapper.selectSysUserProjectList(query);
        if (existing != null && !existing.isEmpty()) {
            result.setAlreadyMember(true);
            result.setMessage("已是项目成员");
            return result;
        }

        BatchUserRoleVo batchUserRoleVo = new BatchUserRoleVo();
        batchUserRoleVo.setProjectId(invite.getProjectId());
        batchUserRoleVo.setMemberIds(new Long[]{userId});
        batchUserRoleVo.setRoleIds(roleIds);
        Preconditions.checkState(sysUserProjectService.batchInsertSysUserProject(batchUserRoleVo) > 0,
                MessageUtils.message("project.add_member_fail"));

        SysUserTeam teamQuery = new SysUserTeam();
        teamQuery.setTeamId(invite.getTeamId());
        teamQuery.setUserId(userId);
        List<SysUserTeam> teamMembership = sysUserTeamMapper.selectSysUserTeamList(teamQuery);
        Preconditions.checkState(teamMembership == null || teamMembership.isEmpty(),
                "项目邀请不应写入团队成员关系");

        result.setAlreadyMember(false);
        result.setMessage("已成功加入项目");
        return result;
    }

    private SysMemberInvite insertInvite(String scope, Long teamId, Long projectId, Long[] roleIds, Long createdBy) {
        Date now = DateUtils.getNowDate();
        SysMemberInvite invite = new SysMemberInvite();
        invite.setToken(generateToken());
        invite.setScope(scope);
        invite.setTeamId(teamId);
        invite.setProjectId(projectId);
        invite.setRoleIds(JSON.toJSONString(roleIds));
        invite.setExpiresAt(addSeconds(now, INVITE_TTL_SECONDS));
        invite.setRevoked(0);
        invite.setCreatedBy(createdBy);
        invite.setCreatedAt(now);
        Preconditions.checkState(sysMemberInviteMapper.insertSysMemberInvite(invite) == 1, "创建邀请链接失败");
        return invite;
    }

    private SysMemberInvite requireInvite(String token) {
        Preconditions.checkArgument(token != null && !token.isBlank(), "邀请 token 无效");
        SysMemberInvite invite = sysMemberInviteMapper.selectSysMemberInviteByToken(token);
        Preconditions.checkNotNull(invite, "邀请链接不存在");
        return invite;
    }

    private boolean isExpired(SysMemberInvite invite) {
        return invite.getExpiresAt() != null && invite.getExpiresAt().before(DateUtils.getNowDate());
    }

    private boolean isRevoked(SysMemberInvite invite) {
        return invite.getRevoked() != null && invite.getRevoked() == 1;
    }

    private Long[] parseRoleIds(String roleIdsJson) {
        if (roleIdsJson == null || roleIdsJson.isBlank()) {
            return new Long[0];
        }
        List<Long> roleIds = JSON.parseArray(roleIdsJson, Long.class);
        return roleIds == null ? new Long[0] : roleIds.toArray(new Long[0]);
    }

    private Date addSeconds(Date base, int seconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(base);
        calendar.add(Calendar.SECOND, seconds);
        return calendar.getTime();
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    private MemberInviteLinkVo toLinkVo(SysMemberInvite invite, String inviteBaseUrl) {
        MemberInviteLinkVo vo = new MemberInviteLinkVo();
        vo.setInviteId(invite.getInviteId());
        vo.setToken(invite.getToken());
        vo.setScope(invite.getScope());
        vo.setTeamId(invite.getTeamId());
        vo.setProjectId(invite.getProjectId());
        vo.setRoleIds(parseRoleIds(invite.getRoleIds()));
        vo.setExpiresAt(invite.getExpiresAt());
        vo.setRevoked(isRevoked(invite));
        vo.setCreatedAt(invite.getCreatedAt());
        if (inviteBaseUrl != null && invite.getToken() != null) {
            String base = inviteBaseUrl.endsWith("/") ? inviteBaseUrl.substring(0, inviteBaseUrl.length() - 1) : inviteBaseUrl;
            vo.setInviteUrl(base + "/invite/" + invite.getToken());
        }
        return vo;
    }
}
