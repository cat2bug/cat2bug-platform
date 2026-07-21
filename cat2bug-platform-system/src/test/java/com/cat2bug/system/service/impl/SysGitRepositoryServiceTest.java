package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.service.git.GitPathValidator;
import com.cat2bug.system.service.git.JGitBareRepositoryHelper;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SysGitRepositoryServiceImpl} 单测：项目角色鉴权（manage/pull 并集、系统管理员与团队管理员
 * 无项目角色时拒绝）、多工程 slug 唯一性、slug 重命名联动磁盘迁移。对应 tasks 2.6。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SysGitRepositoryServiceTest
{
    private static final Long PROJECT_ID = 10L;

    @Mock
    private SysGitRepositoryMapper sysGitRepositoryMapper;
    @Mock
    private SysProjectMapper sysProjectMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private GitPathValidator gitPathValidator;
    @Mock
    private JGitBareRepositoryHelper jGitBareRepositoryHelper;

    @InjectMocks
    private SysGitRepositoryServiceImpl service;

    private Runnable messageCleanup;

    @Before
    public void setUp()
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        when(sysProjectMapper.selectSysProjectByProjectId(eq(PROJECT_ID), isNull())).thenReturn(project());
        when(gitPathValidator.resolveStoredDiskPath(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @After
    public void tearDown()
    {
        if (messageCleanup != null)
        {
            messageCleanup.run();
        }
        SecurityContextHolder.clearContext();
    }

    // ---------- 权限矩阵 ----------

    @Test
    public void selectSysGitRepositoryList_projectAdmin_allowed()
    {
        loginAs(1L, 6L);
        List<SysGitRepository> expected = Collections.singletonList(repository(1L, "api"));
        when(sysGitRepositoryMapper.selectSysGitRepositoryList(any())).thenReturn(expected);

        List<SysGitRepository> result = service.selectSysGitRepositoryList(queryFor(PROJECT_ID));

        assertEquals(expected, result);
    }

    @Test
    public void selectSysGitRepositoryList_projectDeveloper_allowedButNotManage()
    {
        loginAs(2L, 7L);
        when(sysGitRepositoryMapper.selectSysGitRepositoryList(any())).thenReturn(Collections.emptyList());

        // pull 权限够用：不抛异常
        service.selectSysGitRepositoryList(queryFor(PROJECT_ID));

        // 但 manage 操作应被拒绝
        try
        {
            service.insertSysGitRepository(newRepositoryRequest("api"));
            fail("expected ServiceException: developer 不具备 manage 权限");
        }
        catch (ServiceException expected)
        {
            // developer 只有 pull/push
        }
    }

    @Test
    public void selectSysGitRepositoryList_projectMemberWithoutAnyProjectRole_denied()
    {
        loginAs(3L); // 无任何项目角色
        try
        {
            service.selectSysGitRepositoryList(queryFor(PROJECT_ID));
            fail("expected ServiceException for member without project role");
        }
        catch (ServiceException expected)
        {
            // 无项目角色
        }
        verify(sysGitRepositoryMapper, never()).selectSysGitRepositoryList(any());
    }

    @Test
    public void selectSysGitRepositoryList_systemAdminWithoutProjectRole_denied()
    {
        // 系统超管 userId=1；查询该 projectId 下的项目角色时返回空（未被赋予项目角色）
        loginAs(1L);
        when(sysRoleMapper.selectRolePermissionByUserId(isNull(), eq(PROJECT_ID), eq(1L)))
            .thenReturn(Collections.emptyList());

        try
        {
            service.selectSysGitRepositoryList(queryFor(PROJECT_ID));
            fail("expected ServiceException: 系统管理员未被赋予项目角色时不应绕过校验");
        }
        catch (ServiceException expected)
        {
            // 不允许全局 admin 身份绕过项目角色校验
        }
    }

    @Test
    public void deleteSysGitRepositoryByRepositoryId_teamAdminRoleWithoutProjectRole_denied()
    {
        // team.admin(role_id=4) 未被赋予项目角色：即使查询时混入该角色，也不应被当作 manage 通过
        loginAs(9L, 4L);
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(1L)).thenReturn(repository(1L, "api"));

        try
        {
            service.deleteSysGitRepositoryByRepositoryId(1L, true);
            fail("expected ServiceException: team.admin 不是项目角色，不应放行 manage 操作");
        }
        catch (ServiceException expected)
        {
            // 拒绝
        }
        verify(sysGitRepositoryMapper, never()).deleteSysGitRepositoryByRepositoryId(any());
        verify(jGitBareRepositoryHelper, never()).wipeBareRepository(any());
    }

    @Test
    public void selectSysGitRepositoryList_projectIdMissing_rejected()
    {
        loginAs(1L, 6L);
        try
        {
            service.selectSysGitRepositoryList(new SysGitRepository());
            fail("expected ServiceException for missing projectId");
        }
        catch (ServiceException expected)
        {
            // projectId 必填
        }
    }

    // ---------- 新增 / 多工程 slug 唯一性 ----------

    @Test
    public void insertSysGitRepository_manage_createsWithDefaultDiskPath()
    {
        loginAs(1L, 11L); // project.create-by
        when(gitPathValidator.validateSlug("api")).thenReturn("api");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(PROJECT_ID, "api")).thenReturn(null);
        when(gitPathValidator.buildDefaultDiskPath("acme", "app", "api")).thenReturn("/data/git/acme/app/api.git");
        when(sysGitRepositoryMapper.insertSysGitRepository(any())).thenReturn(1);

        SysGitRepository result = service.insertSysGitRepository(newRepositoryRequest("api"));

        assertEquals("/data/git/acme/app/api.git", result.getDiskPath());
        assertEquals("api", result.getSlug());
        verify(jGitBareRepositoryHelper).initBareRepository("/data/git/acme/app/api.git", "master");
        verify(gitPathValidator).ensureParentWritable("/data/git/acme/app/api.git");
    }

    @Test
    public void insertSysGitRepository_duplicateSlugInSameProject_rejected()
    {
        loginAs(1L, 6L);
        when(gitPathValidator.validateSlug("api")).thenReturn("api");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(PROJECT_ID, "api"))
            .thenReturn(repository(1L, "api"));

        try
        {
            service.insertSysGitRepository(newRepositoryRequest("api"));
            fail("expected ServiceException for duplicate slug");
        }
        catch (ServiceException expected)
        {
            // 同项目内 slug 唯一
        }
        verify(jGitBareRepositoryHelper, never()).initBareRepository(any(), any());
        verify(sysGitRepositoryMapper, never()).insertSysGitRepository(any());
    }

    @Test
    public void insertSysGitRepository_distinctSlugsSameProject_bothSucceed()
    {
        loginAs(1L, 6L);
        when(gitPathValidator.validateSlug("api")).thenReturn("api");
        when(gitPathValidator.validateSlug("web")).thenReturn("web");
        when(gitPathValidator.buildDefaultDiskPath("acme", "app", "api")).thenReturn("/data/git/acme/app/api.git");
        when(gitPathValidator.buildDefaultDiskPath("acme", "app", "web")).thenReturn("/data/git/acme/app/web.git");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(eq(PROJECT_ID), anyString())).thenReturn(null);
        when(sysGitRepositoryMapper.insertSysGitRepository(any())).thenReturn(1);

        SysGitRepository first = service.insertSysGitRepository(newRepositoryRequest("api"));
        SysGitRepository second = service.insertSysGitRepository(newRepositoryRequest("web"));

        assertEquals("api", first.getSlug());
        assertEquals("web", second.getSlug());
        assertFalse(first.getDiskPath().equals(second.getDiskPath()));
    }

    @Test
    public void insertSysGitRepository_metadataInsertFails_wipesInitializedDisk()
    {
        loginAs(1L, 6L);
        when(gitPathValidator.validateSlug("api")).thenReturn("api");
        when(gitPathValidator.buildDefaultDiskPath("acme", "app", "api")).thenReturn("/data/git/acme/app/api.git");
        when(sysGitRepositoryMapper.insertSysGitRepository(any())).thenReturn(0);

        try
        {
            service.insertSysGitRepository(newRepositoryRequest("api"));
            fail("expected ServiceException when metadata insert affects 0 rows");
        }
        catch (ServiceException expected)
        {
            // 插入失败
        }
        verify(jGitBareRepositoryHelper).wipeBareRepository("/data/git/acme/app/api.git");
    }

    @Test
    public void insertSysGitRepository_developerRole_denied()
    {
        loginAs(2L, 7L);
        try
        {
            service.insertSysGitRepository(newRepositoryRequest("api"));
            fail("expected ServiceException: develop 不具备 manage 权限");
        }
        catch (ServiceException expected)
        {
            // 拒绝
        }
        verify(sysGitRepositoryMapper, never()).insertSysGitRepository(any());
    }

    // ---------- slug 重命名 ----------

    @Test
    public void updateSysGitRepository_slugRenameOnDefaultPath_movesDiskAndFlagsChange()
    {
        loginAs(1L, 6L);
        SysGitRepository existing = repository(1L, "api");
        existing.setDiskPath("/data/git/acme/app/api.git");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(1L)).thenReturn(existing);
        when(gitPathValidator.validateSlug("api-v2")).thenReturn("api-v2");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(PROJECT_ID, "api-v2")).thenReturn(null);
        when(gitPathValidator.buildDefaultDiskPath("acme", "app", "api")).thenReturn("/data/git/acme/app/api.git");
        when(gitPathValidator.buildDefaultDiskPath("acme", "app", "api-v2")).thenReturn("/data/git/acme/app/api-v2.git");
        when(gitPathValidator.buildLegacyDefaultDiskPath(PROJECT_ID, "api")).thenReturn("/data/git/10/api.git");
        when(sysGitRepositoryMapper.updateSysGitRepository(any())).thenReturn(1);

        SysGitRepository input = new SysGitRepository();
        input.setRepositoryId(1L);
        input.setSlug("api-v2");

        SysGitRepository result = service.updateSysGitRepository(input);

        assertTrue(result.isSlugChanged());
        assertEquals("api-v2", result.getSlug());
        assertEquals("/data/git/acme/app/api-v2.git", result.getDiskPath());
        verify(jGitBareRepositoryHelper).moveBareRepository("/data/git/acme/app/api.git", "/data/git/acme/app/api-v2.git");
    }

    @Test
    public void updateSysGitRepository_slugRenameOnLegacyDefaultPath_movesToNamespacedPath()
    {
        loginAs(1L, 6L);
        SysGitRepository existing = repository(1L, "api");
        existing.setDiskPath("/data/git/10/api.git");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(1L)).thenReturn(existing);
        when(gitPathValidator.validateSlug("api-v2")).thenReturn("api-v2");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(PROJECT_ID, "api-v2")).thenReturn(null);
        when(gitPathValidator.buildDefaultDiskPath("acme", "app", "api")).thenReturn("/data/git/acme/app/api.git");
        when(gitPathValidator.buildDefaultDiskPath("acme", "app", "api-v2")).thenReturn("/data/git/acme/app/api-v2.git");
        when(gitPathValidator.buildLegacyDefaultDiskPath(PROJECT_ID, "api")).thenReturn("/data/git/10/api.git");
        when(sysGitRepositoryMapper.updateSysGitRepository(any())).thenReturn(1);

        SysGitRepository input = new SysGitRepository();
        input.setRepositoryId(1L);
        input.setSlug("api-v2");

        SysGitRepository result = service.updateSysGitRepository(input);

        assertTrue(result.isSlugChanged());
        assertEquals("/data/git/acme/app/api-v2.git", result.getDiskPath());
        verify(jGitBareRepositoryHelper).moveBareRepository("/data/git/10/api.git", "/data/git/acme/app/api-v2.git");
    }

    @Test
    public void updateSysGitRepository_slugRenameOnCustomPath_diskPathUnchanged()
    {
        loginAs(1L, 6L);
        SysGitRepository existing = repository(1L, "api");
        existing.setDiskPath("/opt/custom/api-repo.git");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(1L)).thenReturn(existing);
        when(gitPathValidator.validateSlug("api-v2")).thenReturn("api-v2");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(PROJECT_ID, "api-v2")).thenReturn(null);
        when(gitPathValidator.buildDefaultDiskPath("acme", "app", "api")).thenReturn("/data/git/acme/app/api.git");
        when(gitPathValidator.buildLegacyDefaultDiskPath(PROJECT_ID, "api")).thenReturn("/data/git/10/api.git");
        when(sysGitRepositoryMapper.updateSysGitRepository(any())).thenReturn(1);

        SysGitRepository input = new SysGitRepository();
        input.setRepositoryId(1L);
        input.setSlug("api-v2");

        SysGitRepository result = service.updateSysGitRepository(input);

        assertTrue(result.isSlugChanged());
        assertEquals("/opt/custom/api-repo.git", result.getDiskPath());
        verify(jGitBareRepositoryHelper, never()).moveBareRepository(any(), any());
    }

    @Test
    public void updateSysGitRepository_slugConflict_rejected()
    {
        loginAs(1L, 6L);
        SysGitRepository existing = repository(1L, "api");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(1L)).thenReturn(existing);
        when(gitPathValidator.validateSlug("web")).thenReturn("web");
        SysGitRepository other = repository(2L, "web");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(PROJECT_ID, "web")).thenReturn(other);

        SysGitRepository input = new SysGitRepository();
        input.setRepositoryId(1L);
        input.setSlug("web");

        try
        {
            service.updateSysGitRepository(input);
            fail("expected ServiceException for slug already used by another repository");
        }
        catch (ServiceException expected)
        {
            // 同项目内 slug 唯一
        }
        verify(sysGitRepositoryMapper, never()).updateSysGitRepository(any());
    }

    @Test
    public void updateSysGitRepository_developerRole_denied()
    {
        loginAs(2L, 7L);
        SysGitRepository existing = repository(1L, "api");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(1L)).thenReturn(existing);

        SysGitRepository input = new SysGitRepository();
        input.setRepositoryId(1L);
        input.setSlug("api-v2");

        try
        {
            service.updateSysGitRepository(input);
            fail("expected ServiceException: develop 不具备 manage 权限");
        }
        catch (ServiceException expected)
        {
            // 拒绝
        }
        verify(sysGitRepositoryMapper, never()).updateSysGitRepository(any());
    }

    // ---------- 删除 ----------

    @Test
    public void deleteSysGitRepositoryByRepositoryId_manage_wipesDiskByDefault()
    {
        loginAs(1L, 6L);
        SysGitRepository existing = repository(1L, "api");
        existing.setDiskPath("/data/git/10/api.git");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(1L)).thenReturn(existing);
        when(sysGitRepositoryMapper.deleteSysGitRepositoryByRepositoryId(1L)).thenReturn(1);

        int rows = service.deleteSysGitRepositoryByRepositoryId(1L, true);

        assertEquals(1, rows);
        verify(jGitBareRepositoryHelper).wipeBareRepository("/data/git/10/api.git");
    }

    @Test
    public void deleteSysGitRepositoryByRepositoryId_wipeDiskFalse_keepsDisk()
    {
        loginAs(1L, 11L);
        SysGitRepository existing = repository(1L, "api");
        existing.setDiskPath("/data/git/10/api.git");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(1L)).thenReturn(existing);
        when(sysGitRepositoryMapper.deleteSysGitRepositoryByRepositoryId(1L)).thenReturn(1);

        service.deleteSysGitRepositoryByRepositoryId(1L, false);

        verify(jGitBareRepositoryHelper, never()).wipeBareRepository(any());
    }

    @Test
    public void deleteSysGitRepositoryByRepositoryIds_batchSumsAffectedRows()
    {
        loginAs(1L, 6L);
        SysGitRepository first = repository(1L, "api");
        first.setDiskPath("/data/git/10/api.git");
        SysGitRepository second = repository(2L, "web");
        second.setDiskPath("/data/git/10/web.git");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(1L)).thenReturn(first);
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(2L)).thenReturn(second);
        when(sysGitRepositoryMapper.deleteSysGitRepositoryByRepositoryId(1L)).thenReturn(1);
        when(sysGitRepositoryMapper.deleteSysGitRepositoryByRepositoryId(2L)).thenReturn(1);

        int rows = service.deleteSysGitRepositoryByRepositoryIds(new Long[]{1L, 2L}, true);

        assertEquals(2, rows);
        verify(jGitBareRepositoryHelper).wipeBareRepository(first.getDiskPath());
        verify(jGitBareRepositoryHelper).wipeBareRepository(second.getDiskPath());
    }

    // ---------- 测试夹具 ----------

    private void loginAs(Long userId, Long... projectRoleIds)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName("user" + userId);
        LoginUser loginUser = new LoginUser(userId, null, user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));

        List<SysRole> roles = Arrays.stream(projectRoleIds)
            .map(SysRole::new)
            .collect(Collectors.toList());
        when(sysRoleMapper.selectRolePermissionByUserId(isNull(), eq(PROJECT_ID), eq(userId))).thenReturn(roles);
    }

    private SysGitRepository queryFor(Long projectId)
    {
        SysGitRepository query = new SysGitRepository();
        query.setProjectId(projectId);
        return query;
    }

    private SysGitRepository newRepositoryRequest(String slug)
    {
        SysGitRepository request = new SysGitRepository();
        request.setProjectId(PROJECT_ID);
        request.setName(slug);
        request.setSlug(slug);
        return request;
    }

    private SysGitRepository repository(Long repositoryId, String slug)
    {
        SysGitRepository repository = new SysGitRepository();
        repository.setRepositoryId(repositoryId);
        repository.setProjectId(PROJECT_ID);
        repository.setSlug(slug);
        repository.setName(slug);
        repository.setDefaultBranch("master");
        return repository;
    }

    private SysProject project()
    {
        SysProject project = new SysProject();
        project.setProjectId(PROJECT_ID);
        project.setTeamKey("acme");
        project.setProjectKey("app");
        return project;
    }
}
