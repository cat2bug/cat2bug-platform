package com.cat2bug.system.service.impl;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.SysGitRepositoryMember;
import com.cat2bug.system.domain.SysMemberInvite;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.domain.SysTeam;
import com.cat2bug.system.domain.git.GitRepositoryAclRoles;
import com.cat2bug.system.domain.type.MemberInviteScope;
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
import com.cat2bug.system.service.ISysUserProjectService;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * REPO scope 邀请：3 天 TTL、吊销、accept 写 ACL、已是成员、过期拒绝。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SysMemberInviteRepoScopeTest {

    private static final Long REPOSITORY_ID = 100L;
    private static final Long PROJECT_ID = 20L;
    private static final Long TEAM_ID = 10L;
    private static final Long USER_ID = 5L;
    private static final Long MANAGER_ID = 1L;

    @Mock
    private SysMemberInviteMapper sysMemberInviteMapper;
    @Mock
    private SysTeamMapper sysTeamMapper;
    @Mock
    private SysProjectMapper sysProjectMapper;
    @Mock
    private SysGitRepositoryMapper sysGitRepositoryMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysUserTeamMapper sysUserTeamMapper;
    @Mock
    private SysUserTeamRoleMapper sysUserTeamRoleMapper;
    @Mock
    private SysUserProjectMapper sysUserProjectMapper;
    @Mock
    private ISysUserProjectService sysUserProjectService;
    @Mock
    private ISysGitRepositoryMemberService sysGitRepositoryMemberService;
    @Mock
    private GitRepositoryAccessService gitRepositoryAccessService;

    @InjectMocks
    private SysMemberInviteServiceImpl sysMemberInviteService;

    private Runnable messageCleanup;

    @Before
    public void setUp() {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        when(gitRepositoryAccessService.canManage(MANAGER_ID, REPOSITORY_ID)).thenReturn(true);
        loginAs(MANAGER_ID);
    }

    @After
    public void tearDown() {
        if (messageCleanup != null) {
            messageCleanup.run();
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    public void createRepoInvite_setsThreeDayExpiryAndAclRoleJson() {
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repo());
        when(sysProjectMapper.selectSysProjectByProjectId(PROJECT_ID, null)).thenReturn(project());
        when(sysMemberInviteMapper.insertSysMemberInvite(any())).thenAnswer(invocation -> {
            SysMemberInvite invite = invocation.getArgument(0);
            invite.setInviteId(1L);
            assertEquals(MemberInviteScope.REPO, invite.getScope());
            assertEquals(REPOSITORY_ID, invite.getRepositoryId());
            assertEquals(PROJECT_ID, invite.getProjectId());
            assertEquals(TEAM_ID, invite.getTeamId());
            assertEquals("[\"write\"]", invite.getRoleIds());
            assertNotNull(invite.getExpiresAt());
            long delta = invite.getExpiresAt().getTime() - invite.getCreatedAt().getTime();
            assertEquals(3L * 24 * 3600 * 1000L, delta, 2000L);
            return 1;
        });

        MemberInviteLinkVo link = sysMemberInviteService.createRepoInviteLink(
                REPOSITORY_ID, GitRepositoryAclRoles.WRITE, MANAGER_ID, "http://localhost:2020");

        assertNotNull(link);
        assertEquals(MemberInviteScope.REPO, link.getScope());
        assertEquals(GitRepositoryAclRoles.WRITE, link.getRole());
        assertEquals(REPOSITORY_ID, link.getRepositoryId());
        assertTrue(link.getInviteUrl().contains("/#/invite/"));
    }

    @Test
    public void createRepoInvite_deniedWithoutManage() {
        when(gitRepositoryAccessService.canManage(MANAGER_ID, REPOSITORY_ID)).thenReturn(false);
        try {
            sysMemberInviteService.createRepoInviteLink(
                    REPOSITORY_ID, GitRepositoryAclRoles.WRITE, MANAGER_ID, "http://localhost");
            fail("expected manage deny");
        } catch (ServiceException expected) {
            assertNotNull(expected.getMessage());
        }
        verify(sysMemberInviteMapper, never()).insertSysMemberInvite(any());
    }

    @Test
    public void acceptRepoInvite_insertsAclMember() {
        SysMemberInvite invite = activeRepoInvite("repo-token", "[\"write\"]");
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("repo-token")).thenReturn(invite);
        when(sysGitRepositoryMemberService.selectByRepositoryIdAndUserId(REPOSITORY_ID, USER_ID)).thenReturn(null);
        when(sysGitRepositoryMemberService.insertMemberFromInvite(REPOSITORY_ID, USER_ID, GitRepositoryAclRoles.WRITE))
                .thenReturn(1);
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repoWithKeys());

        MemberInviteAcceptResultVo result = sysMemberInviteService.accept("repo-token", USER_ID, "5");

        assertFalse(result.getAlreadyMember());
        assertEquals(REPOSITORY_ID, result.getRepositoryId());
        assertEquals("/acme/demo/code/platform", result.getRedirectPath());
        verify(sysGitRepositoryMemberService).insertMemberFromInvite(REPOSITORY_ID, USER_ID, GitRepositoryAclRoles.WRITE);
        verify(sysUserTeamMapper, never()).insertSysUserTeam(any());
        verify(sysUserProjectService, never()).batchInsertSysUserProject(any());
    }

    @Test
    public void acceptRepoInvite_whenAlreadyMember_setsFlag() {
        SysMemberInvite invite = activeRepoInvite("repo-token", "[\"read\"]");
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("repo-token")).thenReturn(invite);
        SysGitRepositoryMember existing = new SysGitRepositoryMember();
        existing.setRepositoryId(REPOSITORY_ID);
        existing.setUserId(USER_ID);
        when(sysGitRepositoryMemberService.selectByRepositoryIdAndUserId(REPOSITORY_ID, USER_ID)).thenReturn(existing);
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repoWithKeys());

        MemberInviteAcceptResultVo result = sysMemberInviteService.accept("repo-token", USER_ID, "5");

        assertTrue(result.getAlreadyMember());
        verify(sysGitRepositoryMemberService, never()).insertMemberFromInvite(any(), any(), any());
    }

    @Test
    public void acceptRepoInvite_whenExpired_throws() {
        SysMemberInvite expired = activeRepoInvite("expired-repo", "[\"admin\"]");
        expired.setExpiresAt(pastDate());
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("expired-repo")).thenReturn(expired);

        try {
            sysMemberInviteService.accept("expired-repo", USER_ID, "5");
            fail("expected IllegalStateException for expired invite");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("过期"));
        }
        verify(sysGitRepositoryMemberService, never()).insertMemberFromInvite(any(), any(), any());
    }

    @Test
    public void acceptRepoInvite_whenRevoked_throws() {
        SysMemberInvite revoked = activeRepoInvite("revoked-repo", "[\"write\"]");
        revoked.setRevoked(1);
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("revoked-repo")).thenReturn(revoked);

        try {
            sysMemberInviteService.accept("revoked-repo", USER_ID, "5");
            fail("expected IllegalStateException for revoked invite");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("撤销"));
        }
        verify(sysGitRepositoryMemberService, never()).insertMemberFromInvite(any(), any(), any());
    }

    @Test
    public void revokeRepoInviteLink_delegatesToMapper() {
        when(sysMemberInviteMapper.revokeSysMemberInvite(9L, null, null, REPOSITORY_ID)).thenReturn(1);
        assertEquals(1, sysMemberInviteService.revokeRepoInviteLink(REPOSITORY_ID, 9L));
        verify(sysMemberInviteMapper).revokeSysMemberInvite(9L, null, null, REPOSITORY_ID);
    }

    @Test
    public void preview_repoInvite_setsRepositoryNameAndAclRoleLabel() {
        SysMemberInvite invite = activeRepoInvite("preview-repo", "[\"write\"]");
        invite.setCreatedBy(1L);
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("preview-repo")).thenReturn(invite);
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repo());
        when(sysTeamMapper.selectSysTeamByTeamId(TEAM_ID)).thenReturn(team());
        when(sysProjectMapper.selectSysProjectByProjectId(PROJECT_ID, null)).thenReturn(project());
        when(sysUserMapper.selectUserById(null, null, 1L)).thenReturn(inviter());

        MemberInvitePreviewVo preview = sysMemberInviteService.preview("preview-repo", null);

        assertTrue(preview.getValid());
        assertEquals("Demo Repo", preview.getRepositoryName());
        assertFalse(preview.getRoleNames().isEmpty());
        assertEquals("valid", preview.getStatus());
        verify(sysRoleMapper, never()).selectRoleById(any());
    }

    @Test
    public void createRepoInvite_rejectsInvalidAclRole() {
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repo());
        try {
            sysMemberInviteService.createRepoInviteLink(REPOSITORY_ID, "owner", MANAGER_ID, "http://localhost");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException | ServiceException e) {
            assertNotNull(e.getMessage());
        }
        verify(sysMemberInviteMapper, never()).insertSysMemberInvite(any());
    }

    private void loginAs(Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setUserName("manager");
        LoginUser loginUser = new LoginUser(userId, null, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    private SysMemberInvite activeRepoInvite(String token, String roleIdsJson) {
        SysMemberInvite invite = new SysMemberInvite();
        invite.setInviteId(1L);
        invite.setToken(token);
        invite.setScope(MemberInviteScope.REPO);
        invite.setTeamId(TEAM_ID);
        invite.setProjectId(PROJECT_ID);
        invite.setRepositoryId(REPOSITORY_ID);
        invite.setRoleIds(roleIdsJson);
        invite.setExpiresAt(futureDateDays(2));
        invite.setRevoked(0);
        invite.setCreatedAt(DateUtils.getNowDate());
        return invite;
    }

    private SysGitRepository repo() {
        SysGitRepository repo = new SysGitRepository();
        repo.setRepositoryId(REPOSITORY_ID);
        repo.setProjectId(PROJECT_ID);
        repo.setName("Demo Repo");
        repo.setSlug("platform");
        return repo;
    }

    private SysGitRepository repoWithKeys() {
        SysGitRepository repo = repo();
        repo.setTeamKey("acme");
        repo.setProjectKey("demo");
        return repo;
    }

    private SysProject project() {
        SysProject project = new SysProject();
        project.setProjectId(PROJECT_ID);
        project.setTeamId(TEAM_ID);
        project.setProjectName("Cat2Bug-Platform");
        return project;
    }

    private SysTeam team() {
        SysTeam team = new SysTeam();
        team.setTeamId(TEAM_ID);
        team.setTeamName("Team A");
        return team;
    }

    private com.cat2bug.common.core.domain.entity.SysUser inviter() {
        com.cat2bug.common.core.domain.entity.SysUser user = new com.cat2bug.common.core.domain.entity.SysUser();
        user.setNickName("Alice");
        return user;
    }

    private Date futureDateDays(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    private Date pastDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -1);
        return calendar.getTime();
    }
}
