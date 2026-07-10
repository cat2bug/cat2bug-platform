package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.SysMemberInvite;
import com.cat2bug.quarkus.common.domain.SysProject;
import com.cat2bug.quarkus.common.domain.SysTeam;
import com.cat2bug.quarkus.common.domain.SysUserProject;
import com.cat2bug.quarkus.common.domain.SysUserProjectRole;
import com.cat2bug.quarkus.common.domain.SysUserTeam;
import com.cat2bug.quarkus.common.domain.SysUserTeamRole;
import com.cat2bug.quarkus.common.domain.type.MemberInviteScope;
import com.cat2bug.quarkus.common.domain.vo.CreateMemberInviteVo;
import com.cat2bug.quarkus.common.domain.vo.MemberInviteAcceptResultVo;
import com.cat2bug.quarkus.common.domain.vo.MemberInviteLinkVo;
import com.cat2bug.quarkus.common.domain.vo.MemberInvitePreviewVo;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.system.mapper.SysMemberInviteMapper;
import com.cat2bug.quarkus.system.mapper.SysProjectMapper;
import com.cat2bug.quarkus.system.mapper.SysTeamMapper;
import com.cat2bug.quarkus.system.mapper.SysUserProjectMapper;
import com.cat2bug.quarkus.system.mapper.SysUserProjectRoleMapper;
import com.cat2bug.quarkus.system.mapper.SysUserTeamMapper;
import com.cat2bug.quarkus.system.mapper.SysUserTeamRoleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysMemberInviteService {

    private static final int INVITE_TTL_SECONDS = 3600;

    @Inject
    SysMemberInviteMapper sysMemberInviteMapper;

    @Inject
    SysTeamMapper sysTeamMapper;

    @Inject
    SysProjectMapper sysProjectMapper;

    @Inject
    SysUserTeamMapper sysUserTeamMapper;

    @Inject
    SysUserTeamRoleMapper sysUserTeamRoleMapper;

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

    @Inject
    SysUserProjectRoleMapper sysUserProjectRoleMapper;

    @Inject
    ObjectMapper objectMapper;

    public MemberInviteLinkVo createTeamInviteLink(Long teamId, CreateMemberInviteVo request, Long createdBy, String inviteBaseUrl) {
        requireTeam(teamId);
        requireRoleIds(request);
        return toLinkVo(insertInvite(MemberInviteScope.TEAM, teamId, null, request.getRoleIds(), createdBy), inviteBaseUrl);
    }

    public MemberInviteLinkVo createProjectInviteLink(Long projectId, CreateMemberInviteVo request, Long createdBy, String inviteBaseUrl) {
        SysProject project = requireProject(projectId, createdBy);
        requireRoleIds(request);
        return toLinkVo(insertInvite(MemberInviteScope.PROJECT, project.getTeamId(), projectId, request.getRoleIds(), createdBy), inviteBaseUrl);
    }

    public List<MemberInviteLinkVo> listActiveTeamInviteLinks(Long teamId, String inviteBaseUrl) {
        Date now = new Date();
        return sysMemberInviteMapper.selectActiveSysMemberInviteListByTeamId(teamId, now).stream()
                .map(invite -> toLinkVo(invite, inviteBaseUrl))
                .collect(Collectors.toList());
    }

    public List<MemberInviteLinkVo> listActiveProjectInviteLinks(Long projectId, String inviteBaseUrl) {
        Date now = new Date();
        return sysMemberInviteMapper.selectActiveSysMemberInviteListByProjectId(projectId, now).stream()
                .map(invite -> toLinkVo(invite, inviteBaseUrl))
                .collect(Collectors.toList());
    }

    public int revokeTeamInviteLink(Long teamId, Long inviteId) {
        return sysMemberInviteMapper.revokeSysMemberInvite(inviteId, teamId, null);
    }

    public int revokeProjectInviteLink(Long projectId, Long inviteId) {
        return sysMemberInviteMapper.revokeSysMemberInvite(inviteId, null, projectId);
    }

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

    @Transactional
    public MemberInviteAcceptResultVo accept(String token, Long userId, String createBy) {
        SysMemberInvite invite = requireInvite(token);
        if (isRevoked(invite)) {
            throw new ApiException("邀请链接已撤销");
        }
        if (isExpired(invite)) {
            throw new ApiException("邀请链接已过期");
        }

        Long[] roleIds = parseRoleIds(invite.getRoleIds());
        if (roleIds.length == 0) {
            throw new ApiException("成员角色不能为空!");
        }

        MemberInviteAcceptResultVo result = new MemberInviteAcceptResultVo();
        result.setScope(invite.getScope());
        result.setTeamId(invite.getTeamId());
        result.setProjectId(invite.getProjectId());

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
        sysUserTeam.setCreateBy(createBy);
        if (sysUserTeamMapper.insertSysUserTeam(sysUserTeam) != 1) {
            throw new ApiException("新增用户团队关联失败");
        }

        for (Long roleId : roleIds) {
            SysUserTeamRole role = new SysUserTeamRole();
            role.setUserTeamId(sysUserTeam.getUserTeamId());
            role.setRoleId(roleId);
            if (sysUserTeamRoleMapper.insertSysUserTeamRole(role) != 1) {
                throw new ApiException("新增成员角色失败");
            }
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

        SysUserProject up = new SysUserProject();
        up.setProjectId(invite.getProjectId());
        up.setUserId(userId);
        up.setProjectLock(0);
        if (sysUserProjectMapper.insertSysUserProject(up) != 1) {
            throw new ApiException("向项目中新增成员失败!");
        }

        for (Long roleId : roleIds) {
            SysUserProjectRole upr = new SysUserProjectRole();
            upr.setUserProjectId(up.getUserProjectId());
            upr.setRoleId(roleId);
            if (sysUserProjectRoleMapper.insertSysUserProjectRole(upr) != 1) {
                throw new ApiException("向项目中新增成员失败!");
            }
        }

        SysUserTeam teamQuery = new SysUserTeam();
        teamQuery.setTeamId(invite.getTeamId());
        teamQuery.setUserId(userId);
        List<SysUserTeam> teamMembership = sysUserTeamMapper.selectSysUserTeamList(teamQuery);
        if (teamMembership != null && !teamMembership.isEmpty()) {
            throw new ApiException("项目邀请不应写入团队成员关系");
        }

        result.setAlreadyMember(false);
        result.setMessage("已成功加入项目");
        return result;
    }

    private SysMemberInvite insertInvite(String scope, Long teamId, Long projectId, Long[] roleIds, Long createdBy) {
        Date now = new Date();
        SysMemberInvite invite = new SysMemberInvite();
        invite.setToken(generateToken());
        invite.setScope(scope);
        invite.setTeamId(teamId);
        invite.setProjectId(projectId);
        invite.setRoleIds(writeRoleIds(roleIds));
        invite.setExpiresAt(addSeconds(now, INVITE_TTL_SECONDS));
        invite.setRevoked(0);
        invite.setCreatedBy(createdBy);
        invite.setCreatedAt(now);
        if (sysMemberInviteMapper.insertSysMemberInvite(invite) != 1) {
            throw new ApiException("创建邀请链接失败");
        }
        return invite;
    }

    private SysMemberInvite requireInvite(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException("邀请 token 无效");
        }
        SysMemberInvite invite = sysMemberInviteMapper.selectSysMemberInviteByToken(token);
        if (invite == null) {
            throw new ApiException("邀请链接不存在");
        }
        return invite;
    }

    private void requireTeam(Long teamId) {
        if (teamId == null || sysTeamMapper.selectSysTeamByTeamId(teamId) == null) {
            throw new ApiException("团队不能为空");
        }
    }

    private SysProject requireProject(Long projectId, Long userId) {
        if (projectId == null) {
            throw new ApiException("项目不能为空");
        }
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, userId);
        if (project == null) {
            project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
        }
        if (project == null) {
            throw new ApiException("项目不能为空");
        }
        return project;
    }

    private void requireRoleIds(CreateMemberInviteVo request) {
        if (request == null || request.getRoleIds() == null || request.getRoleIds().length == 0) {
            throw new ApiException("成员角色不能为空!");
        }
    }

    private boolean isExpired(SysMemberInvite invite) {
        return invite.getExpiresAt() != null && invite.getExpiresAt().before(new Date());
    }

    private boolean isRevoked(SysMemberInvite invite) {
        return invite.getRevoked() != null && invite.getRevoked() == 1;
    }

    private Long[] parseRoleIds(String roleIdsJson) {
        if (roleIdsJson == null || roleIdsJson.isBlank()) {
            return new Long[0];
        }
        try {
            List<Long> roleIds = objectMapper.readValue(roleIdsJson, new TypeReference<List<Long>>() {});
            return roleIds == null ? new Long[0] : roleIds.toArray(new Long[0]);
        } catch (Exception e) {
            throw new ApiException("邀请角色配置无效");
        }
    }

    private String writeRoleIds(Long[] roleIds) {
        try {
            return objectMapper.writeValueAsString(roleIds);
        } catch (Exception e) {
            throw new ApiException("邀请角色配置无效");
        }
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
