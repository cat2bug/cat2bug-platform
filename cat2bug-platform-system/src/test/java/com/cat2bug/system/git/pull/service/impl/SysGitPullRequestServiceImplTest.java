package com.cat2bug.system.git.pull.service.impl;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.system.domain.SysGitPullRequest;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.git.browse.domain.GitCommitFileChange;
import com.cat2bug.system.mapper.SysGitPullRequestCommentMapper;
import com.cat2bug.system.mapper.SysGitPullRequestMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.service.git.GitPathValidator;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.RefSpec;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SysGitPullRequestServiceImplTest
{
    private static final Long REPOSITORY_ID = 1L;
    private static final Long PROJECT_ID = 2L;
    private static final Long USER_ID = 3L;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SysGitRepositoryMapper sysGitRepositoryMapper;
    @Mock
    private SysGitPullRequestMapper sysGitPullRequestMapper;
    @Mock
    private SysGitPullRequestCommentMapper sysGitPullRequestCommentMapper;
    @Mock
    private GitRepositoryAccessService gitRepositoryAccessService;
    @Spy
    private GitPathValidator gitPathValidator = new GitPathValidator();
    @InjectMocks
    private SysGitPullRequestServiceImpl service;

    private Runnable messageCleanup;

    @Before
    public void setUp()
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        SysUser user = new SysUser();
        user.setUserId(USER_ID);
        user.setUserName("developer");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser(USER_ID, null, user, java.util.Collections.emptySet()), null,
                java.util.Collections.emptyList()));
        SysGitRepository repository = new SysGitRepository();
        repository.setRepositoryId(REPOSITORY_ID);
        repository.setProjectId(PROJECT_ID);
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repository);
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
    public void merge_developUserDeniedBeforeGitRefIsRead()
    {
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(false);
        try
        {
            service.mergePullRequest(REPOSITORY_ID, 1);
            fail("expected manage permission denial");
        }
        catch (ServiceException expected)
        {
            verify(sysGitPullRequestMapper, never()).selectByRepoAndNumber(any(), any());
        }
    }

    @Test
    public void create_nonPushUserDeniedBeforeBranchLookup()
    {
        when(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID)).thenReturn(false);
        try
        {
            service.createPullRequest(REPOSITORY_ID, new SysGitPullRequest());
            fail("expected push permission denial");
        }
        catch (ServiceException expected)
        {
            verify(sysGitPullRequestMapper, never()).selectMaxNumberByRepo(any());
        }
    }

    @Test
    public void close_authorWithPushPermission_closesOpenPullRequest()
    {
        SysGitPullRequest pullRequest = new SysGitPullRequest();
        pullRequest.setPullRequestId(8L);
        pullRequest.setAuthorId(USER_ID);
        pullRequest.setState("open");
        when(sysGitPullRequestMapper.selectByRepoAndNumber(REPOSITORY_ID, 1)).thenReturn(pullRequest);
        when(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID)).thenReturn(true);

        service.closePullRequest(REPOSITORY_ID, 1);

        verify(sysGitPullRequestMapper).updateStateFields(eq(pullRequest));
    }

    @Test
    public void listFiles_unrelatedHistories_fallsBackToTwoDotDiff() throws Exception
    {
        File bare = createUnrelatedHistoryBareRepository();
        SysGitRepository repository = new SysGitRepository();
        repository.setRepositoryId(REPOSITORY_ID);
        repository.setProjectId(PROJECT_ID);
        repository.setDiskPath(bare.getAbsolutePath());
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repository);
        when(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID)).thenReturn(true);

        SysGitPullRequest pullRequest = new SysGitPullRequest();
        pullRequest.setPullRequestId(9L);
        pullRequest.setSourceBranch("master");
        pullRequest.setTargetBranch("main");
        when(sysGitPullRequestMapper.selectByRepoAndNumber(REPOSITORY_ID, 1)).thenReturn(pullRequest);

        List<GitCommitFileChange> files = service.listFiles(REPOSITORY_ID, 1);

        assertFalse(files.isEmpty());
    }

    private File createUnrelatedHistoryBareRepository() throws Exception
    {
        File bare = temporaryFolder.newFolder("unrelated.git");
        Git.init().setDirectory(bare).setBare(true).call().close();
        File work = temporaryFolder.newFolder("work");
        PersonIdent author = new PersonIdent("tester", "tester@example.com");
        try (Git git = Git.init().setDirectory(work).setInitialBranch("main").call())
        {
            Files.write(new File(work, "main.txt").toPath(), "main".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit().setMessage("main root").setAuthor(author).call();
            git.push().setRemote(bare.getAbsolutePath())
                    .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main")).call();

            git.checkout().setOrphan(true).setName("master").call();
            Files.write(new File(work, "master.txt").toPath(), "master".getBytes(StandardCharsets.UTF_8));
            new File(work, "main.txt").delete();
            git.add().addFilepattern(".").call();
            git.commit().setMessage("master root").setAuthor(author).call();
            git.push().setRemote(bare.getAbsolutePath())
                    .setRefSpecs(new RefSpec("refs/heads/master:refs/heads/master")).call();
        }
        return bare;
    }
}
