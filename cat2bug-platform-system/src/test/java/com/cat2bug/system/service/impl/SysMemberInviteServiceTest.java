package com.cat2bug.system.service.impl;

import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.SysMemberInvite;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.domain.SysTeam;
import com.cat2bug.system.domain.SysUserProject;
import com.cat2bug.system.domain.SysUserTeam;
import com.cat2bug.system.domain.type.MemberInviteScope;
import com.cat2bug.system.domain.vo.CreateMemberInviteVo;
import com.cat2bug.system.domain.vo.MemberInviteAcceptResultVo;
import com.cat2bug.system.domain.vo.MemberInvitePreviewVo;
import com.cat2bug.system.mapper.SysMemberInviteMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.mapper.SysTeamMapper;
import com.cat2bug.system.mapper.SysUserMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.mapper.SysUserTeamMapper;
import com.cat2bug.system.mapper.SysUserTeamRoleMapper;
import com.cat2bug.system.service.ISysUserProjectService;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SysMemberInviteServiceTest {

    @Mock
    private SysMemberInviteMapper sysMemberInviteMapper;
    @Mock
    private SysTeamMapper sysTeamMapper;
    @Mock
    private SysProjectMapper sysProjectMapper;
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

    @InjectMocks
    private SysMemberInviteServiceImpl sysMemberInviteService;

    private SysMemberInvite activeTeamInvite;
    private SysMemberInvite activeProjectInvite;
    private Runnable messageCleanup;

    @Before
    public void setUp() {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        Date future = futureDate();
        activeTeamInvite = invite("team-token", MemberInviteScope.TEAM, 10L, null, future);
        activeProjectInvite = invite("project-token", MemberInviteScope.PROJECT, 10L, 20L, future);
    }

    @org.junit.After
    public void tearDown() {
        if (messageCleanup != null) {
            messageCleanup.run();
        }
    }

    @Test
    public void preview_expiredInvite_marksExpired() {
        SysMemberInvite expired = invite("expired", MemberInviteScope.TEAM, 10L, null, pastDate());
        expired.setCreatedBy(1L);
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("expired")).thenReturn(expired);
        when(sysTeamMapper.selectSysTeamByTeamId(10L)).thenReturn(team("Team A"));
        SysUser inviter = new SysUser();
        inviter.setNickName("Alice");
        when(sysUserMapper.selectUserById(null, null, 1L)).thenReturn(inviter);
        when(sysRoleMapper.selectRoleById(3L)).thenReturn(role("Admin"));

        MemberInvitePreviewVo preview = sysMemberInviteService.preview("expired", null);

        assertTrue(preview.getExpired());
        assertFalse(preview.getValid());
        assertEquals("expired", preview.getStatus());
        assertEquals("Alice", preview.getInviterName());
        assertEquals("Admin", preview.getRoleNames().get(0));
    }

    @Test
    public void preview_revokedInvite_marksRevoked() {
        SysMemberInvite revoked = invite("revoked", MemberInviteScope.TEAM, 10L, null, futureDate());
        revoked.setRevoked(1);
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("revoked")).thenReturn(revoked);
        when(sysTeamMapper.selectSysTeamByTeamId(10L)).thenReturn(team("Team A"));

        MemberInvitePreviewVo preview = sysMemberInviteService.preview("revoked", null);

        assertTrue(preview.getRevoked());
        assertFalse(preview.getValid());
    }

    @Test
    public void preview_teamInvite_alreadyMember_whenLoggedIn() {
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("team-token")).thenReturn(activeTeamInvite);
        when(sysTeamMapper.selectSysTeamByTeamId(10L)).thenReturn(team("Team A"));
        SysUserTeam existing = new SysUserTeam();
        existing.setUserTeamId(1L);
        when(sysUserTeamMapper.selectSysUserTeamList(any())).thenReturn(Collections.singletonList(existing));

        MemberInvitePreviewVo preview = sysMemberInviteService.preview("team-token", 5L);

        assertTrue(preview.getAlreadyMember());
    }

    @Test
    public void preview_projectInvite_alreadyMember_whenLoggedIn() {
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("project-token")).thenReturn(activeProjectInvite);
        when(sysTeamMapper.selectSysTeamByTeamId(10L)).thenReturn(team("Team A"));
        when(sysProjectMapper.selectSysProjectByProjectId(20L, null)).thenReturn(project("Cat2Bug-Platform"));
        SysUserProject existing = new SysUserProject();
        existing.setUserProjectId(1L);
        when(sysUserProjectMapper.selectSysUserProjectList(any())).thenReturn(Collections.singletonList(existing));

        MemberInvitePreviewVo preview = sysMemberInviteService.preview("project-token", 7L);

        assertTrue(preview.getAlreadyMember());
    }

    @Test
    public void preview_teamInvite_notAlreadyMember_whenAnonymous() {
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("team-token")).thenReturn(activeTeamInvite);
        when(sysTeamMapper.selectSysTeamByTeamId(10L)).thenReturn(team("Team A"));

        MemberInvitePreviewVo preview = sysMemberInviteService.preview("team-token", null);

        assertFalse(preview.getAlreadyMember());
    }

    @Test
    public void accept_teamInvite_insertsTeamMembership() {
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("team-token")).thenReturn(activeTeamInvite);
            when(sysUserTeamMapper.selectSysUserTeamList(any())).thenReturn(Collections.emptyList());
            when(sysUserTeamMapper.insertSysUserTeam(any())).thenAnswer(invocation -> {
                SysUserTeam ut = invocation.getArgument(0);
                ut.setUserTeamId(99L);
                return 1;
            });
            when(sysUserTeamRoleMapper.insertSysUserTeamRole(any())).thenReturn(1);

            MemberInviteAcceptResultVo result = sysMemberInviteService.accept("team-token", 5L, "5");

            assertFalse(result.getAlreadyMember());
            verify(sysUserTeamMapper).insertSysUserTeam(any());
            verify(sysUserProjectService, never()).batchInsertSysUserProject(any());
    }

    @Test
    public void accept_teamInvite_alreadyMember_isIdempotent() {
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("team-token")).thenReturn(activeTeamInvite);
            SysUserTeam existing = new SysUserTeam();
            existing.setUserTeamId(1L);
            when(sysUserTeamMapper.selectSysUserTeamList(any())).thenReturn(Collections.singletonList(existing));

            MemberInviteAcceptResultVo result = sysMemberInviteService.accept("team-token", 5L, "5");

            assertTrue(result.getAlreadyMember());
            verify(sysUserTeamMapper, never()).insertSysUserTeam(any());
    }

    @Test
    public void accept_projectInvite_writesProjectOnly() {
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("project-token")).thenReturn(activeProjectInvite);
            when(sysUserProjectMapper.selectSysUserProjectList(any())).thenReturn(Collections.emptyList());
            when(sysUserProjectService.batchInsertSysUserProject(any())).thenReturn(1);
            when(sysUserTeamMapper.selectSysUserTeamList(any())).thenReturn(Collections.emptyList());

            MemberInviteAcceptResultVo result = sysMemberInviteService.accept("project-token", 7L, "7");

            assertFalse(result.getAlreadyMember());
            verify(sysUserProjectService).batchInsertSysUserProject(any());
            verify(sysUserTeamMapper, never()).insertSysUserTeam(any());
    }

    @Test
    public void accept_projectInvite_alreadyMember_isIdempotent() {
        when(sysMemberInviteMapper.selectSysMemberInviteByToken("project-token")).thenReturn(activeProjectInvite);
            SysUserProject existing = new SysUserProject();
            existing.setUserProjectId(1L);
            when(sysUserProjectMapper.selectSysUserProjectList(any())).thenReturn(Collections.singletonList(existing));

            MemberInviteAcceptResultVo result = sysMemberInviteService.accept("project-token", 7L, "7");

            assertTrue(result.getAlreadyMember());
            verify(sysUserProjectService, never()).batchInsertSysUserProject(any());
    }

    @Test
    public void createTeamInviteLink_setsOneHourExpiry() {
        when(sysTeamMapper.selectSysTeamByTeamId(10L)).thenReturn(team("Team A"));
            when(sysMemberInviteMapper.insertSysMemberInvite(any())).thenAnswer(invocation -> {
                SysMemberInvite invite = invocation.getArgument(0);
                invite.setInviteId(1L);
                assertNotNull(invite.getExpiresAt());
                long delta = invite.getExpiresAt().getTime() - invite.getCreatedAt().getTime();
                assertEquals(3600_000L, delta, 2000L);
                return 1;
            });

            CreateMemberInviteVo request = new CreateMemberInviteVo();
            request.setRoleIds(new Long[]{3L});
            assertNotNull(sysMemberInviteService.createTeamInviteLink(10L, request, 1L, "http://localhost:2020"));
    }

    private SysMemberInvite invite(String token, String scope, Long teamId, Long projectId, Date expiresAt) {
        SysMemberInvite invite = new SysMemberInvite();
        invite.setInviteId(1L);
        invite.setToken(token);
        invite.setScope(scope);
        invite.setTeamId(teamId);
        invite.setProjectId(projectId);
        invite.setRoleIds("[3]");
        invite.setExpiresAt(expiresAt);
        invite.setRevoked(0);
        invite.setCreatedAt(DateUtils.getNowDate());
        return invite;
    }

    private SysTeam team(String name) {
        SysTeam team = new SysTeam();
        team.setTeamId(10L);
        team.setTeamName(name);
        return team;
    }

    private SysRole role(String name) {
        SysRole role = new SysRole();
        role.setRoleId(3L);
        role.setRoleName(name);
        return role;
    }

    private SysProject project(String name) {
        SysProject project = new SysProject();
        project.setProjectId(20L);
        project.setProjectName(name);
        return project;
    }

    private Date futureDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        return calendar.getTime();
    }

    private Date pastDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -1);
        return calendar.getTime();
    }
}
