package com.cat2bug.system.service.impl.git;

import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.SysGitRepositoryMember;
import com.cat2bug.system.domain.git.GitRepositoryAclRoles;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMemberMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 仓级 ACL 优先 + 项目角色回落；超管/团队管理员不绕过。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GitRepositoryAccessServiceImplTest
{
    private static final Long USER_ID = 42L;
    private static final Long PROJECT_ID = 10L;
    private static final Long REPOSITORY_ID = 100L;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Mock
    private SysGitRepositoryMemberMapper sysGitRepositoryMemberMapper;

    @InjectMocks
    private GitRepositoryAccessServiceImpl gitRepositoryAccessService;

    @Before
    public void setUp()
    {
        when(sysRoleMapper.selectRolePermissionByUserId(any(), any(), any())).thenReturn(Collections.emptyList());
        when(sysGitRepositoryMemberMapper.selectByRepositoryIdAndUserId(any(), any())).thenReturn(null);
        SysGitRepository repo = new SysGitRepository();
        repo.setRepositoryId(REPOSITORY_ID);
        repo.setProjectId(PROJECT_ID);
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repo);
    }

    private void mockProjectRoles(String... roleKeys)
    {
        List<SysRole> roles = new java.util.ArrayList<>();
        for (String key : roleKeys)
        {
            SysRole role = new SysRole();
            role.setRoleKey(key);
            roles.add(role);
        }
        when(sysRoleMapper.selectRolePermissionByUserId(isNull(), eq(PROJECT_ID), eq(USER_ID))).thenReturn(roles);
    }

    private void mockAcl(String role)
    {
        SysGitRepositoryMember member = new SysGitRepositoryMember();
        member.setRepositoryId(REPOSITORY_ID);
        member.setUserId(USER_ID);
        member.setRole(role);
        when(sysGitRepositoryMemberMapper.selectByRepositoryIdAndUserId(REPOSITORY_ID, USER_ID)).thenReturn(member);
    }

    @Test
    public void tester_canPullAndPush_butNotManage()
    {
        mockProjectRoles("project.tester");

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID));
        assertEquals(GitRepositoryAclRoles.WRITE, gitRepositoryAccessService.resolveEffectiveRole(USER_ID, REPOSITORY_ID));
    }

    @Test
    public void develop_canPullAndPush_butNotManage()
    {
        mockProjectRoles("project.develop");

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID));
    }

    @Test
    public void createBy_canPullPushAndManage()
    {
        mockProjectRoles("project.create-by");

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID));
        assertTrue(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID));
        assertEquals(GitRepositoryAclRoles.ADMIN, gitRepositoryAccessService.resolveEffectiveRole(USER_ID, REPOSITORY_ID));
    }

    @Test
    public void admin_canPullPushAndManage()
    {
        mockProjectRoles("project.admin");

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID));
        assertTrue(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID));
    }

    @Test
    public void memberWithoutFourRoles_deniedEverything()
    {
        mockProjectRoles("project.outsider");

        assertFalse(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID));
        assertFalse(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID));
        assertNull(gitRepositoryAccessService.resolveEffectiveRole(USER_ID, REPOSITORY_ID));
    }

    @Test
    public void noRoles_deniedEverything()
    {
        mockProjectRoles();

        assertFalse(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID));
        assertFalse(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID));
    }

    @Test
    public void multiRoleUnion_developPlusOtherNonGitRole_stillGetsDevelopAccess()
    {
        mockProjectRoles("project.develop", "project.outsider");

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID));
    }

    @Test
    public void doesNotQueryTeamRoles_teamIdAlwaysNull()
    {
        mockProjectRoles("project.admin");

        gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID);

        verify(sysRoleMapper).selectRolePermissionByUserId(isNull(), eq(PROJECT_ID), eq(USER_ID));
    }

    @Test
    public void nullUserOrRepository_denied()
    {
        assertFalse(gitRepositoryAccessService.canPull(null, REPOSITORY_ID));
        assertFalse(gitRepositoryAccessService.canPull(USER_ID, null));
    }

    @Test
    public void aclRead_demotesProjectAdmin_toPullOnly()
    {
        mockProjectRoles("project.admin");
        mockAcl(GitRepositoryAclRoles.READ);

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID));
        assertFalse(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID));
        assertEquals(GitRepositoryAclRoles.READ, gitRepositoryAccessService.resolveEffectiveRole(USER_ID, REPOSITORY_ID));
        verify(sysRoleMapper, never()).selectRolePermissionByUserId(any(), any(), any());
    }

    @Test
    public void aclWrite_nonProjectMember_canPullAndPush()
    {
        mockAcl(GitRepositoryAclRoles.WRITE);

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID));
        verify(sysRoleMapper, never()).selectRolePermissionByUserId(any(), any(), any());
    }

    @Test
    public void aclAdmin_grantsManage()
    {
        mockAcl(GitRepositoryAclRoles.ADMIN);

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID));
        assertTrue(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID));
    }
}
