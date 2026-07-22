package com.cat2bug.system.service.impl;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.git.browse.domain.GitLanguageShare;
import com.cat2bug.system.git.browse.domain.GitProjectOverview;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.service.git.GitRepoLanguageScanner;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SysGitRepositoryServiceImpl#selectProjectOverview(Long)} 单测
 * （OpenSpec {@code git-repo-project-overview} 任务 1.4）：鉴权门与仓库列表一致，
 * 空可见集短路返回全零 + 空语言列表（不触发语言扫描），有可见仓时委派
 * {@link GitRepoLanguageScanner} 并原样透传聚合计数。
 *
 * <p>「多用户关注合计」「open PR 过滤」两个统计口径由真实 SQL 覆盖，见
 * {@link com.cat2bug.system.mapper.SysGitRepositoryMapperOverviewTest}；本测试聚焦
 * Service 层的鉴权门、空集短路与语言扫描装配逻辑。</p>
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SysGitRepositoryServiceImplOverviewTest
{
    private static final Long PROJECT_ID = 10L;
    private static final Long USER_ID = 42L;

    @Mock
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private GitRepoLanguageScanner gitRepoLanguageScanner;

    @InjectMocks
    private SysGitRepositoryServiceImpl service;

    private Runnable messageCleanup;

    @Before
    public void setUp()
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        loginAs(USER_ID);
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

    @Test
    public void selectProjectOverview_noProjectRole_deniedWithForbidden()
    {
        // 未被赋予任何项目角色（manage / develop / tester）→ 无 pull 权限
        when(sysRoleMapper.selectRolePermissionByUserId(eq(null), eq(PROJECT_ID), eq(USER_ID)))
            .thenReturn(Collections.emptyList());

        try
        {
            service.selectProjectOverview(PROJECT_ID);
            fail("expected ServiceException：无 pull 权限");
        }
        catch (ServiceException expected)
        {
            assertEquals(Integer.valueOf(HttpStatus.FORBIDDEN), expected.getCode());
        }
        verify(sysGitRepositoryMapper, never()).selectProjectOverviewCounts(any());
        verify(gitRepoLanguageScanner, never()).scan(any());
    }

    @Test
    public void selectProjectOverview_emptyVisibleSet_returnsZerosAndEmptyLanguagesWithoutScanning()
    {
        allowPull();
        GitProjectOverview zeroCounts = new GitProjectOverview();
        zeroCounts.setRepoCount(0L);
        zeroCounts.setOpenPrCount(0L);
        zeroCounts.setCollectCount(0L);
        when(sysGitRepositoryMapper.selectProjectOverviewCounts(PROJECT_ID)).thenReturn(zeroCounts);

        GitProjectOverview result = service.selectProjectOverview(PROJECT_ID);

        assertEquals(0L, result.getRepoCount());
        assertEquals(0L, result.getOpenPrCount());
        assertEquals(0L, result.getCollectCount());
        assertNotNull(result.getLanguages());
        assertTrue(result.getLanguages().isEmpty());
        verify(sysGitRepositoryMapper, never()).selectRepositoriesForOverviewScan(any());
        verify(gitRepoLanguageScanner, never()).scan(any());
    }

    @Test
    public void selectProjectOverview_authorizedWithVisibleRepos_delegatesToLanguageScannerAndPassesThroughCounts()
    {
        allowPull();
        GitProjectOverview counts = new GitProjectOverview();
        counts.setRepoCount(2L);
        counts.setOpenPrCount(3L);
        counts.setCollectCount(5L);
        when(sysGitRepositoryMapper.selectProjectOverviewCounts(PROJECT_ID)).thenReturn(counts);

        SysGitRepository repoA = new SysGitRepository();
        repoA.setRepositoryId(1L);
        SysGitRepository repoB = new SysGitRepository();
        repoB.setRepositoryId(2L);
        List<SysGitRepository> visibleRepos = List.of(repoA, repoB);
        when(sysGitRepositoryMapper.selectRepositoriesForOverviewScan(PROJECT_ID)).thenReturn(visibleRepos);

        List<GitLanguageShare> languages = List.of(new GitLanguageShare("Java", 70), new GitLanguageShare("其他", 30));
        when(gitRepoLanguageScanner.scan(visibleRepos)).thenReturn(languages);

        GitProjectOverview result = service.selectProjectOverview(PROJECT_ID);

        assertSame(counts, result);
        assertEquals(2L, result.getRepoCount());
        assertEquals(3L, result.getOpenPrCount());
        assertEquals(5L, result.getCollectCount());
        assertEquals(languages, result.getLanguages());

        ArgumentCaptor<List<SysGitRepository>> scanArg = ArgumentCaptor.forClass(List.class);
        verify(gitRepoLanguageScanner).scan(scanArg.capture());
        assertEquals(visibleRepos, scanArg.getValue());
    }

    @Test
    public void selectProjectOverview_projectIdNull_throws()
    {
        try
        {
            service.selectProjectOverview(null);
            fail("expected ServiceException：projectId 必填");
        }
        catch (ServiceException expected)
        {
            // 校验通过异常类型即可，消息由 i18n 提供
        }
    }

    // ---------- 测试夹具 ----------

    private void loginAs(Long userId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName("user" + userId);
        LoginUser loginUser = new LoginUser(userId, null, user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    private void allowPull()
    {
        SysRole developRole = new SysRole(7L);
        when(sysRoleMapper.selectRolePermissionByUserId(eq(null), eq(PROJECT_ID), eq(USER_ID)))
            .thenReturn(List.of(developRole));
    }
}
