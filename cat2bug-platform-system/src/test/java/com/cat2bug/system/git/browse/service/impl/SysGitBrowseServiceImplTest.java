package com.cat2bug.system.git.browse.service.impl;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.git.browse.domain.GitBlobContent;
import com.cat2bug.system.git.browse.domain.GitBranchInfo;
import com.cat2bug.system.git.browse.domain.GitCommitInfo;
import com.cat2bug.system.git.browse.domain.GitTreeEntryInfo;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.service.git.GitPathValidator;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SysGitBrowseServiceImpl} 单测：分支/目录/文件内容/提交记录均基于 JGit 只读读取真实 bare 仓，
 * 且 pull 权限一律委托 {@link GitRepositoryAccessService#canPull}（与 Smart HTTP 共用同一份角色矩阵，
 * 系统管理员无目标项目角色同样被拒绝，见 design.md D3）。对应 OpenSpec {@code project-git-repositories}
 * 任务 4.1。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SysGitBrowseServiceImplTest
{
    private static final Long REPOSITORY_ID = 1L;
    private static final Long PROJECT_ID = 10L;
    private static final Long USER_ID = 42L;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Mock
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Spy
    private GitPathValidator gitPathValidator = new GitPathValidator();

    @InjectMocks
    private SysGitBrowseServiceImpl service;

    private Runnable messageCleanup;
    private File bareRepoDir;

    @Before
    public void setUp() throws Exception
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
    public void listBranches_pullAllowed_returnsBranchesWithDefaultFlag() throws Exception
    {
        initRepoWithOneCommit();
        allowPull();

        List<GitBranchInfo> branches = service.listBranches(REPOSITORY_ID);

        assertEquals(1, branches.size());
        assertEquals("main", branches.get(0).getName());
        assertTrue(branches.get(0).isDefault());
    }

    @Test
    public void listTree_rootDirectory_listsFilesSortedDirsFirst() throws Exception
    {
        initRepoWithOneCommit();
        allowPull();

        List<GitTreeEntryInfo> entries = service.listTree(REPOSITORY_ID, null, null);

        assertEquals(2, entries.size());
        assertEquals(GitTreeEntryInfo.TYPE_TREE, entries.get(0).getType());
        assertEquals("src", entries.get(0).getName());
        assertEquals("init", entries.get(0).getLastCommitMessage());
        assertEquals(GitTreeEntryInfo.TYPE_BLOB, entries.get(1).getType());
        assertEquals("README.md", entries.get(1).getName());
        assertEquals("init", entries.get(1).getLastCommitMessage());
        assertNotNull(entries.get(1).getLastCommitTime());
    }

    @Test
    public void listTree_subDirectory_listsNestedFile() throws Exception
    {
        initRepoWithOneCommit();
        allowPull();

        List<GitTreeEntryInfo> entries = service.listTree(REPOSITORY_ID, null, "src");

        assertEquals(1, entries.size());
        assertEquals("main.txt", entries.get(0).getName());
        assertEquals("src/main.txt", entries.get(0).getPath());
        assertEquals("init", entries.get(0).getLastCommitMessage());
        assertNotNull(entries.get(0).getLastCommitTime());
    }

    @Test
    public void readBlob_textFile_returnsContent() throws Exception
    {
        initRepoWithOneCommit();
        allowPull();

        GitBlobContent blob = service.readBlob(REPOSITORY_ID, null, "README.md");

        assertFalse(blob.isBinary());
        assertEquals("hello", blob.getContent());
        assertFalse(blob.isTruncated());
    }

    @Test
    public void readBlob_missingPath_notFound() throws Exception
    {
        initRepoWithOneCommit();
        allowPull();

        try
        {
            service.readBlob(REPOSITORY_ID, null, "missing.txt");
            fail("expected ServiceException for missing file");
        }
        catch (ServiceException expected)
        {
            // 文件不存在
        }
    }

    @Test
    public void listCommits_returnsCommitsNewestFirst() throws Exception
    {
        initRepoWithOneCommit();
        allowPull();

        List<GitCommitInfo> commits = service.listCommits(REPOSITORY_ID, null, null);

        assertEquals(1, commits.size());
        assertEquals("init", commits.get(0).getMessage());
        assertEquals("tester", commits.get(0).getAuthorName());
    }

    @Test
    public void listCommits_emptyRepository_returnsEmptyList() throws Exception
    {
        initEmptyRepo();
        allowPull();

        assertTrue(service.listCommits(REPOSITORY_ID, null, null).isEmpty());
    }

    @Test
    public void listBranches_noPullPermission_denied() throws Exception
    {
        initRepoWithOneCommit();
        when(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID)).thenReturn(false);

        try
        {
            service.listBranches(REPOSITORY_ID);
            fail("expected ServiceException: 无 pull 权限");
        }
        catch (ServiceException expected)
        {
            // 拒绝，不因系统管理员/团队管理员身份绕过
        }
        verify(sysGitRepositoryMapper).selectSysGitRepositoryByRepositoryId(REPOSITORY_ID);
    }

    @Test
    public void listTree_repositoryNotFound_rejected()
    {
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(null);

        try
        {
            service.listTree(REPOSITORY_ID, null, null);
            fail("expected ServiceException for missing repository");
        }
        catch (ServiceException expected)
        {
            // 仓库不存在
        }
        verify(gitRepositoryAccessService, never()).canPull(eq(USER_ID), eq(PROJECT_ID));
    }

    // ---------- 测试夹具 ----------

    private void loginAs(Long userId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName("user" + userId);
        LoginUser loginUser = new LoginUser(userId, null, user, java.util.Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, java.util.Collections.emptyList()));
    }

    private void allowPull()
    {
        when(gitRepositoryAccessService.canPull(USER_ID, PROJECT_ID)).thenReturn(true);
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

    private void initEmptyRepo() throws IOException, org.eclipse.jgit.api.errors.GitAPIException
    {
        bareRepoDir = temporaryFolder.newFolder("empty.git");
        Git.init().setDirectory(bareRepoDir).setBare(true).call().close();
        mockRepository();
    }

    /** 初始化一个 bare 仓：先在工作区提交一次，再 push 到 bare 仓，模拟真实仓库磁盘布局 */
    private void initRepoWithOneCommit() throws Exception
    {
        bareRepoDir = temporaryFolder.newFolder("repo.git");
        Git.init().setDirectory(bareRepoDir).setBare(true).call().close();

        File workDir = temporaryFolder.newFolder("work");
        try (Git git = Git.init().setDirectory(workDir).setInitialBranch("main").call())
        {
            Files.write(new File(workDir, "README.md").toPath(), "hello".getBytes(StandardCharsets.UTF_8));
            File srcDir = new File(workDir, "src");
            srcDir.mkdirs();
            Files.write(new File(srcDir, "main.txt").toPath(), "content".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit()
                .setMessage("init")
                .setAuthor(new PersonIdent("tester", "tester@example.com"))
                .call();
            git.push().setRemote(bareRepoDir.getAbsolutePath()).setRefSpecs(
                new org.eclipse.jgit.transport.RefSpec("refs/heads/main:refs/heads/main")).call();
        }

        mockRepository();
    }
}
