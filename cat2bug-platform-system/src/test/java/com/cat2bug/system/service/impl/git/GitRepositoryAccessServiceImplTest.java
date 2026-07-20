package com.cat2bug.system.service.impl.git;

import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.system.mapper.SysRoleMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * git-smart-http spec「Pull and push role matrix」/「Force-push restricted to manage roles」验证：
 * develop/tester 可 pull+push 不可 force-push；create-by/admin 均可；无四角色之一者全部拒绝；
 * 且不因超管/团队管理员角色而绕过（不传 teamId，只查目标项目角色）。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GitRepositoryAccessServiceImplTest
{
    private static final Long USER_ID = 42L;
    private static final Long PROJECT_ID = 10L;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @InjectMocks
    private GitRepositoryAccessServiceImpl gitRepositoryAccessService;

    private void mockRoles(String... roleKeys)
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

    @Before
    public void setUp()
    {
        when(sysRoleMapper.selectRolePermissionByUserId(any(), any(), any())).thenReturn(Collections.emptyList());
    }

    @Test
    public void tester_canPullAndPush_butNotManage()
    {
        mockRoles("project.tester");

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, PROJECT_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID));
    }

    @Test
    public void develop_canPullAndPush_butNotManage()
    {
        mockRoles("project.develop");

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, PROJECT_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID));
    }

    @Test
    public void createBy_canPullPushAndManage()
    {
        mockRoles("project.create-by");

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, PROJECT_ID));
        assertTrue(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID));
    }

    @Test
    public void admin_canPullPushAndManage()
    {
        mockRoles("project.admin");

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, PROJECT_ID));
        assertTrue(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID));
    }

    @Test
    public void memberWithoutFourRoles_deniedEverything()
    {
        mockRoles("project.outsider");

        assertFalse(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID));
        assertFalse(gitRepositoryAccessService.canPush(USER_ID, PROJECT_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID));
    }

    @Test
    public void noRoles_deniedEverything()
    {
        mockRoles();

        assertFalse(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID));
        assertFalse(gitRepositoryAccessService.canPush(USER_ID, PROJECT_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID));
    }

    @Test
    public void multiRoleUnion_developPlusOtherNonGitRole_stillGetsDevelopAccess()
    {
        mockRoles("project.develop", "project.outsider");

        assertTrue(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID));
        assertTrue(gitRepositoryAccessService.canPush(USER_ID, PROJECT_ID));
        assertFalse(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID));
    }

    @Test
    public void doesNotQueryTeamRoles_teamIdAlwaysNull()
    {
        mockRoles("project.admin");

        gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID);

        org.mockito.Mockito.verify(sysRoleMapper).selectRolePermissionByUserId(isNull(), eq(PROJECT_ID), eq(USER_ID));
    }

    @Test
    public void nullUserOrProject_denied()
    {
        assertFalse(gitRepositoryAccessService.canPull(null, PROJECT_ID));
        assertFalse(gitRepositoryAccessService.canPull(USER_ID, null));
    }
}
