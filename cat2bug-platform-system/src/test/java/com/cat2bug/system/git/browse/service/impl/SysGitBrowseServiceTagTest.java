package com.cat2bug.system.git.browse.service.impl;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysGitRelease;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.git.browse.domain.GitTagInfo;
import com.cat2bug.system.git.release.service.ISysGitReleaseService;
import com.cat2bug.system.mapper.SysGitReleaseMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.service.git.GitPathValidator;
import com.cat2bug.system.service.git.GitReleaseTagHelper;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.service.git.GitTagHelper;
import com.cat2bug.system.service.git.GitWebhookEnqueueService;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tags API 服务层：list/filter、annotated create、duplicate、delete、cascade 409。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SysGitBrowseServiceTagTest
{
    private static final Long REPOSITORY_ID = 1L;
    private static final Long PROJECT_ID = 10L;
    private static final Long USER_ID = 42L;
    private static final Long RELEASE_ID = 77L;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Mock
    private SysUserProjectMapper sysUserProjectMapper;

    @Mock
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Mock
    private SysGitReleaseMapper sysGitReleaseMapper;

    @Mock
    private ISysGitReleaseService sysGitReleaseService;

    @Mock
    private GitWebhookEnqueueService gitWebhookEnqueueService;

    @Spy
    private GitPathValidator gitPathValidator = new GitPathValidator();

    @InjectMocks
    private SysGitBrowseServiceImpl service;

    private GitTagHelper gitTagHelper;
    private GitReleaseTagHelper gitReleaseTagHelper;

    private Runnable messageCleanup;
    private File bareRepoDir;
    private ObjectId headCommit;

    @Before
    public void setUp() throws Exception
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        loginAs(USER_ID);
        gitReleaseTagHelper = new GitReleaseTagHelper();
        setField(gitReleaseTagHelper, "gitPathValidator", gitPathValidator);
        gitTagHelper = new GitTagHelper();
        setField(gitTagHelper, "gitPathValidator", gitPathValidator);
        setField(gitTagHelper, "gitReleaseTagHelper", gitReleaseTagHelper);
        setField(service, "gitTagHelper", gitTagHelper);
        initRepoWithOneCommit();
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

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void listTags_pullAllowed_filtersAndAttachesReleaseId() throws Exception
    {
        allowPull();
        gitTagHelper.createAnnotatedTag(bareRepoDir.getAbsolutePath(), "v1.0.0", "stable", headCommit,
                new PersonIdent("t", "t@e.com"));
        gitTagHelper.createAnnotatedTag(bareRepoDir.getAbsolutePath(), "v2.0.0", "", headCommit,
                new PersonIdent("t", "t@e.com"));
        createLightweightTag("legacy");

        SysGitRelease release = new SysGitRelease();
        release.setReleaseId(RELEASE_ID);
        release.setTagName("v1.0.0");
        when(sysGitReleaseMapper.selectSysGitReleaseListByRepositoryId(REPOSITORY_ID, false))
                .thenReturn(Collections.singletonList(release));

        List<GitTagInfo> all = service.listTags(REPOSITORY_ID, null);
        assertEquals(3, all.size());
        GitTagInfo v1 = findTag(all, "v1.0.0");
        assertNotNull(v1);
        assertTrue(v1.isAnnotated());
        assertEquals("stable", v1.getMessage());
        assertEquals(RELEASE_ID, v1.getReleaseId());
        assertNull(findTag(all, "v2.0.0").getReleaseId());
        GitTagInfo legacy = findTag(all, "legacy");
        assertFalse(legacy.isAnnotated());
        assertEquals("", legacy.getMessage());

        List<GitTagInfo> filtered = service.listTags(REPOSITORY_ID, "V1");
        assertEquals(1, filtered.size());
        assertEquals("v1.0.0", filtered.get(0).getName());
    }

    @Test
    public void createTag_manageUser_createsAnnotatedAndEnqueuesWebhook() throws Exception
    {
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);
        when(sysGitReleaseMapper.selectSysGitReleaseListByRepositoryId(REPOSITORY_ID, false))
                .thenReturn(Collections.emptyList());

        GitTagInfo created = service.createTag(REPOSITORY_ID, "v2.0.0", "First stable", "main", null);

        assertEquals("v2.0.0", created.getName());
        assertTrue(created.isAnnotated());
        assertEquals("First stable", created.getMessage());
        assertTrue(gitTagHelper.tagExists(bareRepoDir.getAbsolutePath(), "v2.0.0"));
        verify(gitWebhookEnqueueService).onTagCreated(eq(REPOSITORY_ID), eq(USER_ID), eq("v2.0.0"), any(ObjectId.class));
    }

    @Test
    public void createTag_duplicate_rejected() throws Exception
    {
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);
        when(sysGitReleaseMapper.selectSysGitReleaseListByRepositoryId(REPOSITORY_ID, false))
                .thenReturn(Collections.emptyList());
        service.createTag(REPOSITORY_ID, "v1.0.0", "", "main", null);

        try
        {
            service.createTag(REPOSITORY_ID, "v1.0.0", "again", "main", null);
            fail("expected duplicate");
        }
        catch (ServiceException expected)
        {
            assertEquals(Integer.valueOf(HttpStatus.CONFLICT), expected.getCode());
        }
        verify(gitWebhookEnqueueService).onTagCreated(eq(REPOSITORY_ID), eq(USER_ID), eq("v1.0.0"), any(ObjectId.class));
    }

    @Test
    public void deleteTag_withoutRelease_deletesTagOnly() throws Exception
    {
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);
        gitTagHelper.createAnnotatedTag(bareRepoDir.getAbsolutePath(), "v0.1.0", "", headCommit,
                new PersonIdent("t", "t@e.com"));
        when(sysGitReleaseMapper.selectSysGitReleaseByRepositoryIdAndTagName(REPOSITORY_ID, "v0.1.0"))
                .thenReturn(null);

        service.deleteTag(REPOSITORY_ID, "v0.1.0", null);

        assertFalse(gitTagHelper.tagExists(bareRepoDir.getAbsolutePath(), "v0.1.0"));
        verify(sysGitReleaseService, never()).deleteRelease(anyLong(), anyLong());
        verify(gitWebhookEnqueueService).onTagDeleted(REPOSITORY_ID, USER_ID, "v0.1.0");
    }

    @Test
    public void deleteTag_withRelease_withoutCascade_conflict() throws Exception
    {
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);
        gitTagHelper.createAnnotatedTag(bareRepoDir.getAbsolutePath(), "v1.0.0", "", headCommit,
                new PersonIdent("t", "t@e.com"));
        SysGitRelease release = new SysGitRelease();
        release.setReleaseId(RELEASE_ID);
        release.setTagName("v1.0.0");
        when(sysGitReleaseMapper.selectSysGitReleaseByRepositoryIdAndTagName(REPOSITORY_ID, "v1.0.0"))
                .thenReturn(release);

        try
        {
            service.deleteTag(REPOSITORY_ID, "v1.0.0", false);
            fail("expected 409 cascade required");
        }
        catch (ServiceException expected)
        {
            assertEquals(Integer.valueOf(HttpStatus.CONFLICT), expected.getCode());
        }
        assertTrue(gitTagHelper.tagExists(bareRepoDir.getAbsolutePath(), "v1.0.0"));
        verify(sysGitReleaseService, never()).deleteRelease(anyLong(), anyLong());
        verify(gitWebhookEnqueueService, never()).onTagDeleted(anyLong(), anyLong(), any());
    }

    @Test
    public void deleteTag_withCascade_deletesReleaseThenTag() throws Exception
    {
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);
        gitTagHelper.createAnnotatedTag(bareRepoDir.getAbsolutePath(), "v1.0.0", "", headCommit,
                new PersonIdent("t", "t@e.com"));
        SysGitRelease release = new SysGitRelease();
        release.setReleaseId(RELEASE_ID);
        release.setTagName("v1.0.0");
        when(sysGitReleaseMapper.selectSysGitReleaseByRepositoryIdAndTagName(REPOSITORY_ID, "v1.0.0"))
                .thenReturn(release);

        service.deleteTag(REPOSITORY_ID, "v1.0.0", true);

        verify(sysGitReleaseService).deleteRelease(REPOSITORY_ID, RELEASE_ID);
        assertFalse(gitTagHelper.tagExists(bareRepoDir.getAbsolutePath(), "v1.0.0"));
        verify(gitWebhookEnqueueService).onTagDeleted(REPOSITORY_ID, USER_ID, "v1.0.0");
    }

    @Test
    public void createTag_developUserDenied() throws Exception
    {
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(false);
        try
        {
            service.createTag(REPOSITORY_ID, "v9.0.0", "", "main", null);
            fail("expected manage required");
        }
        catch (ServiceException expected)
        {
            // denied
        }
        assertFalse(gitTagHelper.tagExists(bareRepoDir.getAbsolutePath(), "v9.0.0"));
        verify(gitWebhookEnqueueService, never()).onTagCreated(anyLong(), anyLong(), any(), any());
    }

    private void createLightweightTag(String name) throws Exception
    {
        try (Repository repository = new FileRepositoryBuilder().setGitDir(bareRepoDir).setBare().build())
        {
            RefUpdate update = repository.updateRef("refs/tags/" + name);
            update.setNewObjectId(headCommit);
            update.disableRefLog();
            update.update();
        }
    }

    private static GitTagInfo findTag(List<GitTagInfo> tags, String name)
    {
        for (GitTagInfo tag : tags)
        {
            if (name.equals(tag.getName()))
            {
                return tag;
            }
        }
        return null;
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

    private void allowPull()
    {
        when(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID)).thenReturn(true);
    }

    private void initRepoWithOneCommit() throws Exception
    {
        bareRepoDir = temporaryFolder.newFolder("repo.git");
        Git.init().setDirectory(bareRepoDir).setBare(true).call().close();

        File workDir = temporaryFolder.newFolder("work");
        try (Git git = Git.init().setDirectory(workDir).setInitialBranch("main").call())
        {
            Files.write(new File(workDir, "README.md").toPath(), "hi".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit().setMessage("init").setAuthor(new PersonIdent("tester", "tester@example.com")).call();
            headCommit = git.getRepository().resolve("HEAD");
            git.push().setRemote(bareRepoDir.getAbsolutePath()).setPushAll().call();
        }

        SysGitRepository repo = new SysGitRepository();
        repo.setRepositoryId(REPOSITORY_ID);
        repo.setProjectId(PROJECT_ID);
        repo.setDiskPath(bareRepoDir.getAbsolutePath());
        repo.setDefaultBranch("main");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repo);
    }
}
