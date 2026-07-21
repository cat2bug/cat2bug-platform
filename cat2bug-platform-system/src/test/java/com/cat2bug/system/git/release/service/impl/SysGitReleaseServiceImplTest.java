package com.cat2bug.system.git.release.service.impl;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysGitRelease;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.mapper.SysGitReleaseAssetMapper;
import com.cat2bug.system.mapper.SysGitReleaseMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.service.git.GitReleaseTagHelper;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 发行版权限矩阵、草稿可见性、tag 绑定冲突（OpenSpec git-repo-releases 2.5）。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SysGitReleaseServiceImplTest
{
    private static final Long REPOSITORY_ID = 1L;
    private static final Long PROJECT_ID = 10L;
    private static final Long USER_ID = 42L;
    private static final Long RELEASE_ID = 100L;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Mock
    private SysGitReleaseMapper sysGitReleaseMapper;

    @Mock
    private SysGitReleaseAssetMapper sysGitReleaseAssetMapper;

    @Mock
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Mock
    private GitReleaseTagHelper gitReleaseTagHelper;

    @InjectMocks
    private SysGitReleaseServiceImpl service;

    private Runnable messageCleanup;
    private File bareRepoDir;
    private String headCommit;

    @Before
    public void setUp() throws Exception
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        loginAs(USER_ID);
        initBareRepoWithOneCommit();
        mockRepository();
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
    public void listReleases_pullUser_excludesDrafts()
    {
        when(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID)).thenReturn(true);
        when(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID)).thenReturn(false);

        service.listReleases(REPOSITORY_ID);

        verify(sysGitReleaseMapper).selectSysGitReleaseListByRepositoryId(REPOSITORY_ID, true);
    }

    @Test
    public void listReleases_manageUser_includesDrafts()
    {
        when(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID)).thenReturn(true);
        when(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID)).thenReturn(true);

        service.listReleases(REPOSITORY_ID);

        verify(sysGitReleaseMapper).selectSysGitReleaseListByRepositoryId(REPOSITORY_ID, false);
    }

    @Test
    public void createRelease_pullOnlyUser_denied()
    {
        when(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID)).thenReturn(false);

        SysGitRelease body = newReleaseBody("v1.0.0");
        try
        {
            service.createRelease(REPOSITORY_ID, body);
            fail("expected ServiceException: manage required");
        }
        catch (ServiceException expected)
        {
            // develop / pull-only 不可创建
        }
        verify(sysGitReleaseMapper, never()).insertSysGitRelease(any());
    }

    @Test
    public void getRelease_draft_pullOnlyUser_denied()
    {
        when(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID)).thenReturn(true);
        when(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID)).thenReturn(false);

        SysGitRelease draft = new SysGitRelease();
        draft.setReleaseId(RELEASE_ID);
        draft.setRepositoryId(REPOSITORY_ID);
        draft.setDraft(1);
        when(sysGitReleaseMapper.selectSysGitReleaseByReleaseId(RELEASE_ID)).thenReturn(draft);

        try
        {
            service.getRelease(REPOSITORY_ID, RELEASE_ID);
            fail("expected ServiceException: draft hidden");
        }
        catch (ServiceException expected)
        {
            // 草稿对 pull-only 不可见
        }
    }

    @Test
    public void createRelease_manageUser_persistsAndTags() throws Exception
    {
        when(gitRepositoryAccessService.canManage(USER_ID, PROJECT_ID)).thenReturn(true);
        when(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID)).thenReturn(true);
        when(sysGitReleaseMapper.selectSysGitReleaseByRepositoryIdAndTagName(eq(REPOSITORY_ID), eq("v2.0.0")))
                .thenReturn(null);

        ObjectId commitId = ObjectId.fromString(headCommit);
        when(gitReleaseTagHelper.resolveTargetCommit(anyString(), any(), any(), any())).thenReturn(commitId);
        when(gitReleaseTagHelper.ensureTagAtCommit(anyString(), eq("v2.0.0"), eq(commitId))).thenReturn(headCommit);

        ArgumentCaptor<SysGitRelease> insertCaptor = ArgumentCaptor.forClass(SysGitRelease.class);
        when(sysGitReleaseMapper.insertSysGitRelease(insertCaptor.capture())).thenAnswer(inv -> {
            SysGitRelease r = inv.getArgument(0);
            r.setReleaseId(RELEASE_ID);
            return 1;
        });

        SysGitRelease published = new SysGitRelease();
        published.setReleaseId(RELEASE_ID);
        published.setRepositoryId(REPOSITORY_ID);
        published.setDraft(0);
        published.setTagName("v2.0.0");
        when(sysGitReleaseMapper.selectSysGitReleaseByReleaseId(RELEASE_ID)).thenReturn(published);
        when(sysGitReleaseAssetMapper.selectSysGitReleaseAssetListByReleaseId(RELEASE_ID))
                .thenReturn(Collections.emptyList());

        SysGitRelease created = service.createRelease(REPOSITORY_ID, newReleaseBody("v2.0.0"));

        assertEquals(RELEASE_ID, created.getReleaseId());
        assertEquals(headCommit, insertCaptor.getValue().getTargetCommitId());
        assertEquals(Integer.valueOf(0), insertCaptor.getValue().getDraft());
    }

    // ---------- fixtures ----------

    private static SysGitRelease newReleaseBody(String tag)
    {
        SysGitRelease body = new SysGitRelease();
        body.setTagName(tag);
        body.setTitle("Release " + tag);
        body.setDraft(0);
        body.setTargetRef("main");
        return body;
    }

    private void loginAs(Long userId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName("user" + userId);
        LoginUser loginUser = new LoginUser(userId, null, user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    private void mockRepository()
    {
        SysGitRepository repo = new SysGitRepository();
        repo.setRepositoryId(REPOSITORY_ID);
        repo.setProjectId(PROJECT_ID);
        repo.setDiskPath(bareRepoDir.getAbsolutePath());
        repo.setDefaultBranch("main");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repo);
    }

    private void initBareRepoWithOneCommit() throws Exception
    {
        bareRepoDir = temporaryFolder.newFolder("repo.git");
        Git.init().setDirectory(bareRepoDir).setBare(true).call().close();

        File workDir = temporaryFolder.newFolder("work");
        try (Git git = Git.init().setDirectory(workDir).setInitialBranch("main").call())
        {
            Files.write(new File(workDir, "README.md").toPath(), "hi".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit().setMessage("init").setAuthor(new PersonIdent("t", "t@e.com")).call();
            headCommit = git.getRepository().resolve("HEAD").getName();
            git.push().setRemote(bareRepoDir.getAbsolutePath()).setPushAll().call();
        }
    }
}
