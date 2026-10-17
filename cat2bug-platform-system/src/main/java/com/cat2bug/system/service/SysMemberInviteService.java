package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysGitRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cat2bug.common.domain.SysGitRepositoryMember;
import com.cat2bug.common.domain.SysMemberInvite;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysTeam;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserProject;
import com.cat2bug.common.domain.SysUserTeam;
import com.cat2bug.common.domain.SysUserTeamRole;
import com.cat2bug.common.domain.git.GitRepositoryAclRoles;
import com.cat2bug.common.domain.type.MemberInviteScope;
import com.cat2bug.common.domain.vo.BatchUserRoleVo;
import com.cat2bug.common.domain.vo.CreateMemberInviteVo;
import com.cat2bug.common.domain.vo.MemberInviteAcceptResultVo;
import com.cat2bug.common.domain.vo.MemberInviteLinkVo;
import com.cat2bug.common.domain.vo.MemberInvitePreviewVo;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.DateUtils;
import com.cat2bug.common.util.IdUtils;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.SecurityUtils;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysMemberInviteMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.mapper.SysTeamMapper;
import com.cat2bug.system.mapper.SysUserMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.mapper.SysUserTeamMapper;
import com.cat2bug.system.mapper.SysUserTeamRoleMapper;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysMemberInviteService {

    private static final int INVITE_TTL_SECONDS = 3600;
    private static final int REPO_INVITE_TTL_SECONDS = 3 * 24 * 3600;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    SysMemberInviteMapper sysMemberInviteMapper;

    @Inject
    SysTeamMapper sysTeamMapper;

    @Inject
    SysProjectMapper sysProjectMapper;

    @Inject
    SysGitRepositoryMapper sysGitRepositoryMapper;

    @Inject
    SysUserMapper sysUserMapper;

    @Inject
    SysRoleMapper sysRoleMapper;

    @Inject
    SysUserTeamMapper sysUserTeamMapper;

    @Inject
    SysUserTeamRoleMapper sysUserTeamRoleMapper;

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

    @Inject
    SysUserProjectService sysUserProjectService;

    @Inject
    ISysGitRepositoryMemberService sysGitRepositoryMemberService;

    @Inject
    GitRepositoryAccessService gitRepositoryAccessService;

    public MemberInviteLinkVo createTeamInviteLink(Long teamId, CreateMemberInviteVo request, Long createdBy,
                                                   String inviteBaseUrl) {
        requireNonNull(teamId, MessageUtils.message("team.team_not_empty"));
        requireNonNull(request, "roleIds is required");
        if (request.getRoleIds() == null || request.getRoleIds().length == 0) {
            throw new ApiException(MessageUtils.message("team.role_not_empty"));
        }
        if (sysTeamMapper.selectSysTeamByTeamId(teamId) == null) {
            throw new ApiException(MessageUtils.message("team.team_not_empty"));
        }
        return toLinkVo(insertInvite(MemberInviteScope.TEAM, teamId, null, null, request.getRoleIds(), createdBy),
                inviteBaseUrl);
    }

    public MemberInviteLinkVo createProjectInviteLink(Long projectId, CreateMemberInviteVo request, Long createdBy,
                                                      String inviteBaseUrl) {
        requireNonNull(projectId, MessageUtils.message("project.team_cannot_empty"));
        requireNonNull(request, "roleIds is required");
        if (request.getRoleIds() == null || request.getRoleIds().length == 0) {
            throw new ApiException(MessageUtils.message("project.member_role_not_empty"));
        }
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, createdBy);
        if (project == null) {
            project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
        }
        if (project == null) {
            throw new ApiException(MessageUtils.message("project.team_cannot_empty"));
        }
        return toLinkVo(insertInvite(MemberInviteScope.PROJECT, project.getTeamId(), projectId, null,
                request.getRoleIds(), createdBy), inviteBaseUrl);
    }

    public MemberInviteLinkVo createRepoInviteLink(Long repositoryId, String aclRole, Long createdBy,
                                                   String inviteBaseUrl) {
        requireNonNull(repositoryId, MessageUtils.message("git.repo.repository-id-required"));
        assertRepoManage(repositoryId, createdBy);
        String normalizedRole = normalizeAclRole(aclRole);
        SysGitRepository repo = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repo == null) {
            throw new ApiException(MessageUtils.message("git.repo.not-found"));
        }
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(repo.getProjectId(), null);
        if (project == null) {
            throw new ApiException(MessageUtils.message("project.team_cannot_empty"));
        }

        Date now = DateUtils.getNowDate();
        SysMemberInvite invite = new SysMemberInvite();
        invite.setToken(generateToken());
        invite.setScope(MemberInviteScope.REPO);
        invite.setTeamId(project.getTeamId());
        invite.setProjectId(repo.getProjectId());
        invite.setRepositoryId(repositoryId);
        invite.setRoleIds(toJson(Collections.singletonList(normalizedRole)));
        invite.setExpiresAt(addSeconds(now, REPO_INVITE_TTL_SECONDS));
        invite.setRevoked(0);
        invite.setCreatedBy(createdBy);
        invite.setCreatedAt(now);
        if (sysMemberInviteMapper.insertSysMemberInvite(invite) != 1) {
            throw new ApiException("创建邀请链接失败");
        }
        return toLinkVo(invite, inviteBaseUrl);
    }

    public List<MemberInviteLinkVo> listActiveTeamInviteLinks(Long teamId, String inviteBaseUrl) {
        Date now = DateUtils.getNowDate();
        return sysMemberInviteMapper.selectActiveSysMemberInviteListByTeamId(teamId, now).stream()
                .map(invite -> toLinkVo(invite, inviteBaseUrl))
                .collect(Collectors.toList());
    }

    public List<MemberInviteLinkVo> listActiveProjectInviteLinks(Long projectId, String inviteBaseUrl) {
        Date now = DateUtils.getNowDate();
        return sysMemberInviteMapper.selectActiveSysMemberInviteListByProjectId(projectId, now).stream()
                .map(invite -> toLinkVo(invite, inviteBaseUrl))
                .collect(Collectors.toList());
    }

    public List<MemberInviteLinkVo> listActiveRepoInviteLinks(Long repositoryId, String inviteBaseUrl) {
        assertRepoManage(repositoryId, SecurityUtils.getUserId());
        Date now = DateUtils.getNowDate();
        return sysMemberInviteMapper.selectActiveSysMemberInviteListByRepositoryId(repositoryId, now).stream()
                .map(invite -> toLinkVo(invite, inviteBaseUrl))
                .collect(Collectors.toList());
    }

    public int revokeTeamInviteLink(Long teamId, Long inviteId) {
        return sysMemberInviteMapper.revokeSysMemberInvite(inviteId, teamId, null, null);
    }

    public int revokeProjectInviteLink(Long projectId, Long inviteId) {
        return sysMemberInviteMapper.revokeSysMemberInvite(inviteId, null, projectId, null);
    }

    public int revokeRepoInviteLink(Long repositoryId, Long inviteId) {
        assertRepoManage(repositoryId, SecurityUtils.getUserId());
        return sysMemberInviteMapper.revokeSysMemberInvite(inviteId, null, null, repositoryId);
    }

    public MemberInvitePreviewVo preview(String token, Long userId) {
        SysMemberInvite invite = requireInvite(token);
        MemberInvitePreviewVo preview = new MemberInvitePreviewVo();
        preview.setScope(invite.getScope());
        preview.setExpired(isExpired(invite));
        preview.setRevoked(isRevoked(invite));
        preview.setValid(!preview.getExpired() && !preview.getRevoked());
        preview.setExpiresAt(invite.getExpiresAt());
        preview.setStatus(resolvePreviewStatus(preview.getExpired(), preview.getRevoked()));
        preview.setInviterName(resolveInviterName(invite.getCreatedBy()));
        preview.setAlreadyMember(userId != null && isAlreadyMember(invite, userId));

        if (MemberInviteScope.REPO.equals(invite.getScope())) {
            preview.setRoleNames(resolveAclRoleNames(parseAclRoles(invite.getRoleIds())));
            if (invite.getRepositoryId() != null) {
                SysGitRepository repo = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(invite.getRepositoryId());
                if (repo != null) {
                    preview.setRepositoryName(repo.getName());
                }
            }
        } else {
            preview.setRoleNames(resolveRoleNames(parseRoleIds(invite.getRoleIds())));
        }

        SysTeam team = sysTeamMapper.selectSysTeamByTeamId(invite.getTeamId());
        if (team != null) {
            preview.setTeamName(team.getTeamName());
        }
        if ((MemberInviteScope.PROJECT.equals(invite.getScope()) || MemberInviteScope.REPO.equals(invite.getScope()))
                && invite.getProjectId() != null) {
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

        MemberInviteAcceptResultVo result = new MemberInviteAcceptResultVo();
        result.setScope(invite.getScope());
        result.setTeamId(invite.getTeamId());
        result.setProjectId(invite.getProjectId());
        result.setRepositoryId(invite.getRepositoryId());

        if (MemberInviteScope.REPO.equals(invite.getScope())) {
            return acceptRepoInvite(invite, userId, result);
        }

        Long[] roleIds = parseRoleIds(invite.getRoleIds());
        if (roleIds.length == 0) {
            throw new ApiException(MessageUtils.message("team.role_not_empty"));
        }

        if (MemberInviteScope.TEAM.equals(invite.getScope())) {
            return acceptTeamInvite(invite, userId, createBy, roleIds, result);
        }
        return acceptProjectInvite(invite, userId, roleIds, result);
    }

    private void assertRepoManage(Long repositoryId, Long userId) {
        if (userId == null || !gitRepositoryAccessService.canManage(userId, repositoryId)) {
            throw new ApiException(MessageUtils.message("no-permission"), 403);
        }
    }

    private MemberInviteAcceptResultVo acceptRepoInvite(SysMemberInvite invite, Long userId,
                                                        MemberInviteAcceptResultVo result) {
        if (invite.getRepositoryId() == null) {
            throw new ApiException(MessageUtils.message("git.repo.repository-id-required"));
        }
        List<String> roles = parseAclRoles(invite.getRoleIds());
        if (roles.isEmpty()) {
            throw new ApiException(MessageUtils.message("git.repo.member.role-invalid"));
        }
        String aclRole = normalizeAclRole(roles.get(0));

        result.setRedirectPath(buildRepoRedirectPath(invite.getRepositoryId()));

        if (isAlreadyMember(invite, userId)) {
            result.setAlreadyMember(true);
            result.setMessage("已是仓库成员");
            return result;
        }

        try {
            sysGitRepositoryMemberService.insertMemberFromInvite(invite.getRepositoryId(), userId, aclRole);
        } catch (ApiException e) {
            String duplicateMsg = MessageUtils.message("git.repo.member.duplicate");
            if (duplicateMsg != null && duplicateMsg.equals(e.getMessage())) {
                result.setAlreadyMember(true);
                result.setMessage("已是仓库成员");
                return result;
            }
            throw e;
        }

        result.setAlreadyMember(false);
        result.setMessage("已成功加入仓库");
        return result;
    }

    private String buildRepoRedirectPath(Long repositoryId) {
        if (repositoryId == null) {
            return null;
        }
        SysGitRepository repo = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repo == null) {
            return null;
        }
        String teamKey = repo.getTeamKey();
        String projectKey = repo.getProjectKey();
        String slug = repo.getSlug();
        if (StringUtils.isEmpty(teamKey) || StringUtils.isEmpty(projectKey) || StringUtils.isEmpty(slug)) {
            return null;
        }
        return "/" + teamKey + "/" + projectKey + "/code/" + slug;
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

        String now = LocalDateTime.now().format(TIME_FMT);
        SysUserTeam sysUserTeam = new SysUserTeam();
        sysUserTeam.setUserId(userId);
        sysUserTeam.setTeamId(invite.getTeamId());
        sysUserTeam.setCreateTime(now);
        sysUserTeam.setCreateBy(createBy);
        if (sysUserTeamMapper.insertSysUserTeam(sysUserTeam) != 1) {
            throw new ApiException(MessageUtils.message("team.insert_user_team_role_fail"));
        }

        for (Long roleId : roleIds) {
            SysUserTeamRole role = new SysUserTeamRole();
            role.setUserTeamId(sysUserTeam.getUserTeamId());
            role.setRoleId(roleId);
            if (sysUserTeamRoleMapper.insertSysUserTeamRole(role) != 1) {
                throw new ApiException(MessageUtils.message("team.insert_member_role_fail"));
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

        BatchUserRoleVo batchUserRoleVo = new BatchUserRoleVo();
        batchUserRoleVo.setProjectId(invite.getProjectId());
        batchUserRoleVo.setMemberIds(new Long[]{userId});
        batchUserRoleVo.setRoleIds(roleIds);
        if (sysUserProjectService.batchInsertSysUserProject(batchUserRoleVo) <= 0) {
            throw new ApiException(MessageUtils.message("project.add_member_fail"));
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

    private SysMemberInvite insertInvite(String scope, Long teamId, Long projectId, Long repositoryId,
                                         Long[] roleIds, Long createdBy) {
        Date now = DateUtils.getNowDate();
        SysMemberInvite invite = new SysMemberInvite();
        invite.setToken(generateToken());
        invite.setScope(scope);
        invite.setTeamId(teamId);
        invite.setProjectId(projectId);
        invite.setRepositoryId(repositoryId);
        invite.setRoleIds(toJson(roleIds));
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
        if (token == null || StringUtils.isEmpty(token)) {
            throw new ApiException("邀请 token 无效");
        }
        SysMemberInvite invite = sysMemberInviteMapper.selectSysMemberInviteByToken(token);
        if (invite == null) {
            throw new ApiException("邀请链接不存在");
        }
        return invite;
    }

    private boolean isAlreadyMember(SysMemberInvite invite, Long userId) {
        if (MemberInviteScope.TEAM.equals(invite.getScope())) {
            SysUserTeam query = new SysUserTeam();
            query.setTeamId(invite.getTeamId());
            query.setUserId(userId);
            List<SysUserTeam> existing = sysUserTeamMapper.selectSysUserTeamList(query);
            return existing != null && !existing.isEmpty();
        }
        if (MemberInviteScope.PROJECT.equals(invite.getScope()) && invite.getProjectId() != null) {
            SysUserProject query = new SysUserProject();
            query.setProjectId(invite.getProjectId());
            query.setUserId(userId);
            List<SysUserProject> existing = sysUserProjectMapper.selectSysUserProjectList(query);
            return existing != null && !existing.isEmpty();
        }
        if (MemberInviteScope.REPO.equals(invite.getScope()) && invite.getRepositoryId() != null) {
            SysGitRepositoryMember existing = sysGitRepositoryMemberService
                    .selectByRepositoryIdAndUserId(invite.getRepositoryId(), userId);
            return existing != null;
        }
        return false;
    }

    private boolean isExpired(SysMemberInvite invite) {
        return invite.getExpiresAt() != null && invite.getExpiresAt().before(DateUtils.getNowDate());
    }

    private boolean isRevoked(SysMemberInvite invite) {
        return invite.getRevoked() != null && invite.getRevoked() == 1;
    }

    private Long[] parseRoleIds(String roleIdsJson) {
        if (StringUtils.isEmpty(roleIdsJson)) {
            return new Long[0];
        }
        List<Long> roleIds = readJsonList(roleIdsJson, new TypeReference<List<Long>>() { });
        return roleIds == null ? new Long[0] : roleIds.toArray(new Long[0]);
    }

    private List<String> parseAclRoles(String roleIdsJson) {
        if (StringUtils.isEmpty(roleIdsJson)) {
            return Collections.emptyList();
        }
        List<String> roles = readJsonList(roleIdsJson, new TypeReference<List<String>>() { });
        return roles == null ? Collections.emptyList() : roles;
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static <T> T readJsonList(String json, TypeReference<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String normalizeAclRole(String aclRole) {
        if (StringUtils.isEmpty(aclRole)) {
            throw new ApiException(MessageUtils.message("git.repo.member.role-invalid"));
        }
        String trimmed = aclRole.trim();
        if (!GitRepositoryAclRoles.ALL.contains(trimmed)) {
            throw new ApiException(MessageUtils.message("git.repo.member.role-invalid"));
        }
        return trimmed;
    }

    private Date addSeconds(Date base, int seconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(base);
        calendar.add(Calendar.SECOND, seconds);
        return calendar.getTime();
    }

    private String generateToken() {
        return IdUtils.fastSimpleUUID() + IdUtils.fastSimpleUUID();
    }

    private MemberInviteLinkVo toLinkVo(SysMemberInvite invite, String inviteBaseUrl) {
        MemberInviteLinkVo vo = new MemberInviteLinkVo();
        vo.setInviteId(invite.getInviteId());
        vo.setToken(invite.getToken());
        vo.setScope(invite.getScope());
        vo.setTeamId(invite.getTeamId());
        vo.setProjectId(invite.getProjectId());
        vo.setRepositoryId(invite.getRepositoryId());
        if (MemberInviteScope.REPO.equals(invite.getScope())) {
            List<String> roles = parseAclRoles(invite.getRoleIds());
            vo.setRole(roles.isEmpty() ? null : roles.get(0));
        } else {
            vo.setRoleIds(parseRoleIds(invite.getRoleIds()));
        }
        vo.setExpiresAt(invite.getExpiresAt());
        vo.setRevoked(isRevoked(invite));
        vo.setCreatedAt(invite.getCreatedAt());
        if (inviteBaseUrl != null && invite.getToken() != null) {
            String base = inviteBaseUrl.endsWith("/")
                    ? inviteBaseUrl.substring(0, inviteBaseUrl.length() - 1)
                    : inviteBaseUrl;
            vo.setInviteUrl(base + "/#/invite/" + invite.getToken());
        }
        return vo;
    }

    private String resolvePreviewStatus(boolean expired, boolean revoked) {
        if (revoked) {
            return "revoked";
        }
        if (expired) {
            return "expired";
        }
        return "valid";
    }

    private String resolveInviterName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectUserById(userId);
        if (user == null) {
            return null;
        }
        if (StringUtils.isNotEmpty(user.getNickName())) {
            return user.getNickName();
        }
        return user.getUserName();
    }

    private List<String> resolveRoleNames(Long[] roleIds) {
        List<String> names = new ArrayList<>();
        if (roleIds == null) {
            return names;
        }
        for (Long roleId : roleIds) {
            if (roleId == null) {
                continue;
            }
            SysRole role = sysRoleMapper.selectRoleById(roleId);
            if (role != null && StringUtils.isNotEmpty(role.getRoleName())) {
                names.add(role.getRoleName());
            }
        }
        return names;
    }

    private List<String> resolveAclRoleNames(List<String> roles) {
        List<String> names = new ArrayList<>();
        if (roles == null) {
            return names;
        }
        for (String role : roles) {
            if (StringUtils.isEmpty(role)) {
                continue;
            }
            String key = "git.repo.member.role." + role;
            String label = MessageUtils.message(key);
            names.add(StringUtils.isNotEmpty(label) && !key.equals(label) ? label : role);
        }
        return names;
    }

    private static void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new ApiException(message);
        }
    }
}
