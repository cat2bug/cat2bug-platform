package com.cat2bug.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.uuid.UUID;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.SysGitRepositoryMember;
import com.cat2bug.system.domain.SysMemberInvite;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.domain.SysTeam;
import com.cat2bug.system.domain.SysUserProject;
import com.cat2bug.system.domain.SysUserTeam;
import com.cat2bug.system.domain.SysUserTeamRole;
import com.cat2bug.system.domain.git.GitRepositoryAclRoles;
import com.cat2bug.system.domain.type.MemberInviteScope;
import com.cat2bug.system.domain.vo.BatchUserRoleVo;
import com.cat2bug.system.domain.vo.CreateMemberInviteVo;
import com.cat2bug.system.domain.vo.MemberInviteAcceptResultVo;
import com.cat2bug.system.domain.vo.MemberInviteLinkVo;
import com.cat2bug.system.domain.vo.MemberInvitePreviewVo;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysMemberInviteMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.mapper.SysTeamMapper;
import com.cat2bug.system.mapper.SysUserMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.mapper.SysUserTeamMapper;
import com.cat2bug.system.mapper.SysUserTeamRoleMapper;
import com.cat2bug.system.service.ISysGitRepositoryMemberService;
import com.cat2bug.system.service.ISysMemberInviteService;
import com.cat2bug.system.service.ISysUserProjectService;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成员邀请链接 Service 实现
 */
@Service
public class SysMemberInviteServiceImpl implements ISysMemberInviteService {

    /** 团队/项目邀请 TTL：1 小时 */
    private static final int INVITE_TTL_SECONDS = 3600;

    /** 仓库 ACL 邀请 TTL：3 天 */
    private static final int REPO_INVITE_TTL_SECONDS = 3 * 24 * 3600;

    @Autowired
    private SysMemberInviteMapper sysMemberInviteMapper;

    @Autowired
    private SysTeamMapper sysTeamMapper;

    @Autowired
    private SysProjectMapper sysProjectMapper;

    @Autowired
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysUserTeamMapper sysUserTeamMapper;

    @Autowired
    private SysUserTeamRoleMapper sysUserTeamRoleMapper;

    @Autowired
    private SysUserProjectMapper sysUserProjectMapper;

    @Autowired
    private ISysUserProjectService sysUserProjectService;

    @Autowired
    private ISysGitRepositoryMemberService sysGitRepositoryMemberService;

    @Autowired
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Override
    public MemberInviteLinkVo createTeamInviteLink(Long teamId, CreateMemberInviteVo request, Long createdBy, String inviteBaseUrl) {
        Preconditions.checkNotNull(teamId, MessageUtils.message("team.team_not_empty"));
        Preconditions.checkNotNull(request, "roleIds is required");
        Preconditions.checkArgument(request.getRoleIds() != null && request.getRoleIds().length > 0,
                MessageUtils.message("team.role_not_empty"));
        Preconditions.checkNotNull(sysTeamMapper.selectSysTeamByTeamId(teamId), MessageUtils.message("team.team_not_empty"));
        return toLinkVo(insertInvite(MemberInviteScope.TEAM, teamId, null, null, request.getRoleIds(), createdBy), inviteBaseUrl);
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
        return toLinkVo(insertInvite(MemberInviteScope.PROJECT, project.getTeamId(), projectId, null, request.getRoleIds(), createdBy), inviteBaseUrl);
    }

    @Override
    public MemberInviteLinkVo createRepoInviteLink(Long repositoryId, String aclRole, Long createdBy, String inviteBaseUrl) {
        Preconditions.checkNotNull(repositoryId, MessageUtils.message("git.repo.repository-id-required"));
        assertRepoManage(repositoryId, createdBy);
        String normalizedRole = normalizeAclRole(aclRole);
        SysGitRepository repo = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        Preconditions.checkNotNull(repo, MessageUtils.message("git.repo.not-found"));
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(repo.getProjectId(), null);
        Preconditions.checkNotNull(project, MessageUtils.message("project.team_cannot_empty"));

        // 不自动吊销既有链接；「重新生成」由调用方 revoke + create
        Date now = DateUtils.getNowDate();
        SysMemberInvite invite = new SysMemberInvite();
        invite.setToken(generateToken());
        invite.setScope(MemberInviteScope.REPO);
        invite.setTeamId(project.getTeamId());
        invite.setProjectId(repo.getProjectId());
        invite.setRepositoryId(repositoryId);
        invite.setRoleIds(JSON.toJSONString(Collections.singletonList(normalizedRole)));
        invite.setExpiresAt(addSeconds(now, REPO_INVITE_TTL_SECONDS));
        invite.setRevoked(0);
        invite.setCreatedBy(createdBy);
        invite.setCreatedAt(now);
        Preconditions.checkState(sysMemberInviteMapper.insertSysMemberInvite(invite) == 1, "创建邀请链接失败");
        return toLinkVo(invite, inviteBaseUrl);
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
    public List<MemberInviteLinkVo> listActiveRepoInviteLinks(Long repositoryId, String inviteBaseUrl) {
        assertRepoManage(repositoryId, SecurityUtils.getUserId());
        Date now = DateUtils.getNowDate();
        return sysMemberInviteMapper.selectActiveSysMemberInviteListByRepositoryId(repositoryId, now).stream()
                .map(invite -> toLinkVo(invite, inviteBaseUrl))
                .collect(Collectors.toList());
    }

    @Override
    public int revokeTeamInviteLink(Long teamId, Long inviteId) {
        return sysMemberInviteMapper.revokeSysMemberInvite(inviteId, teamId, null, null);
    }

    @Override
    public int revokeProjectInviteLink(Long projectId, Long inviteId) {
        return sysMemberInviteMapper.revokeSysMemberInvite(inviteId, null, projectId, null);
    }

    @Override
    public int revokeRepoInviteLink(Long repositoryId, Long inviteId) {
        assertRepoManage(repositoryId, SecurityUtils.getUserId());
        return sysMemberInviteMapper.revokeSysMemberInvite(inviteId, null, null, repositoryId);
    }

    private void assertRepoManage(Long repositoryId, Long userId) {
        if (userId == null || !gitRepositoryAccessService.canManage(userId, repositoryId)) {
            throw new ServiceException(MessageUtils.message("no-permission"), HttpStatus.FORBIDDEN);
        }
    }

    @Override
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
        SysUser user = sysUserMapper.selectUserById(null, null, userId);
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
        result.setRepositoryId(invite.getRepositoryId());

        if (MemberInviteScope.REPO.equals(invite.getScope())) {
            return acceptRepoInvite(invite, userId, result);
        }

        Long[] roleIds = parseRoleIds(invite.getRoleIds());
        Preconditions.checkArgument(roleIds.length > 0, MessageUtils.message("team.role_not_empty"));

        if (MemberInviteScope.TEAM.equals(invite.getScope())) {
            return acceptTeamInvite(invite, userId, createBy, roleIds, result);
        }
        return acceptProjectInvite(invite, userId, roleIds, result);
    }

    private MemberInviteAcceptResultVo acceptRepoInvite(SysMemberInvite invite, Long userId,
                                                        MemberInviteAcceptResultVo result) {
        Preconditions.checkNotNull(invite.getRepositoryId(), MessageUtils.message("git.repo.repository-id-required"));
        List<String> roles = parseAclRoles(invite.getRoleIds());
        Preconditions.checkArgument(!roles.isEmpty(), MessageUtils.message("git.repo.member.role-invalid"));
        String aclRole = normalizeAclRole(roles.get(0));

        result.setRedirectPath(buildRepoRedirectPath(invite.getRepositoryId()));

        if (isAlreadyMember(invite, userId)) {
            result.setAlreadyMember(true);
            result.setMessage("已是仓库成员");
            return result;
        }

        try {
            // insertMember 会校验 effective manage；邀请接受方通常无 manage，故用 FromInvite
            sysGitRepositoryMemberService.insertMemberFromInvite(invite.getRepositoryId(), userId, aclRole);
        } catch (ServiceException e) {
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

    private SysMemberInvite insertInvite(String scope, Long teamId, Long projectId, Long repositoryId,
                                         Long[] roleIds, Long createdBy) {
        Date now = DateUtils.getNowDate();
        SysMemberInvite invite = new SysMemberInvite();
        invite.setToken(generateToken());
        invite.setScope(scope);
        invite.setTeamId(teamId);
        invite.setProjectId(projectId);
        invite.setRepositoryId(repositoryId);
        invite.setRoleIds(JSON.toJSONString(roleIds));
        invite.setExpiresAt(addSeconds(now, INVITE_TTL_SECONDS));
        invite.setRevoked(0);
        invite.setCreatedBy(createdBy);
        invite.setCreatedAt(now);
        Preconditions.checkState(sysMemberInviteMapper.insertSysMemberInvite(invite) == 1, "创建邀请链接失败");
        return invite;
    }

    private SysMemberInvite requireInvite(String token) {
        Preconditions.checkArgument(token != null && !StringUtils.isEmpty(token), "邀请 token 无效");
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

    /** TEAM/PROJECT：解析为 Long 角色 id */
    private Long[] parseRoleIds(String roleIdsJson) {
        if (StringUtils.isEmpty(roleIdsJson)) {
            return new Long[0];
        }
        List<Long> roleIds = JSON.parseArray(roleIdsJson, Long.class);
        return roleIds == null ? new Long[0] : roleIds.toArray(new Long[0]);
    }

    /** REPO：解析为 ACL 角色字面量 */
    private List<String> parseAclRoles(String roleIdsJson) {
        if (StringUtils.isEmpty(roleIdsJson)) {
            return Collections.emptyList();
        }
        List<String> roles = JSON.parseArray(roleIdsJson, String.class);
        return roles == null ? Collections.emptyList() : roles;
    }

    private String normalizeAclRole(String aclRole) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(aclRole), MessageUtils.message("git.repo.member.role-invalid"));
        String trimmed = aclRole.trim();
        Preconditions.checkArgument(GitRepositoryAclRoles.ALL.contains(trimmed),
                MessageUtils.message("git.repo.member.role-invalid"));
        return trimmed;
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
            String base = inviteBaseUrl.endsWith("/") ? inviteBaseUrl.substring(0, inviteBaseUrl.length() - 1) : inviteBaseUrl;
            vo.setInviteUrl(base + "/#/invite/" + invite.getToken());
        }
        return vo;
    }
}
