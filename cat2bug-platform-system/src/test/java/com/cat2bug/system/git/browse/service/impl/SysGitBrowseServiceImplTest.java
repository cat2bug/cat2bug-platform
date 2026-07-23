package com.cat2bug.system.git.browse.service.impl;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.git.browse.domain.GitBlobContent;
import com.cat2bug.system.git.browse.domain.GitBranchInfo;
import com.cat2bug.system.git.browse.domain.GitCommitDetail;
import com.cat2bug.system.git.browse.domain.GitCommitFileChange;
import com.cat2bug.system.git.browse.domain.GitCommitInfo;
import com.cat2bug.system.git.browse.domain.GitContributorInfo;
import com.cat2bug.system.git.browse.domain.GitTreeEntryInfo;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
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
import org.mockito.ArgumentCaptor;
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
import java.util.Collections;
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
    private SysUserProjectMapper sysUserProjectMapper;

    @Mock
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Spy
    private GitPathValidator gitPathValidator = new GitPathValidator();

    @InjectMocks
    private SysGitBrowseServiceImpl service;

    private Runnable messageCleanup;
    private File bareRepoDir;
    private SysGitRepository repositoryMetadata;

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
        assertNotNull(branches.get(0).getShortId());
        assertEquals("tester", branches.get(0).getAuthorName());
        assertNotNull(branches.get(0).getCommitTime());
    }

    @Test
    public void listBranches_emptyRepository_returnsEmptyList() throws Exception
    {
        initEmptyRepo();
        allowPull();

        assertTrue(service.listBranches(REPOSITORY_ID).isEmpty());
    }

    @Test
    public void listBranches_divergentBranches_reportsAheadAndBehind() throws Exception
    {
        initRepoWithDivergentBranches();
        allowPull();

        List<GitBranchInfo> branches = service.listBranches(REPOSITORY_ID);

        GitBranchInfo feature = findBranch(branches, "feature/demo");
        assertNotNull(feature);
        assertEquals(1, feature.getAhead());
        assertEquals(1, feature.getBehind());
        assertEquals(0, findBranch(branches, "main").getAhead());
        assertEquals(0, findBranch(branches, "main").getBehind());
    }

    @Test
    public void createBranch_manageUser_createsFromDefaultTip() throws Exception
    {
        initRepoWithOneCommit();
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);

        GitBranchInfo branch = service.createBranch(REPOSITORY_ID, "feature/new", null);

        assertEquals("feature/new", branch.getName());
        assertNotNull(branch.getShortId());
        assertTrue(branchExists("feature/new"));
    }

    @Test
    public void createBranch_staleMetaDefault_fallsBackToHead() throws Exception
    {
        initRepoWithOneCommit();
        repositoryMetadata.setDefaultBranch("master");
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);

        GitBranchInfo branch = service.createBranch(REPOSITORY_ID, "feature/from-head", null);

        assertEquals("feature/from-head", branch.getName());
        assertTrue(branchExists("feature/from-head"));
    }

    @Test
    public void createBranch_missingExplicitFromRef_rejected() throws Exception
    {
        initRepoWithOneCommit();
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);

        try
        {
            service.createBranch(REPOSITORY_ID, "feature/bad", "master");
            fail("expected ServiceException: source not found");
        }
        catch (ServiceException expected)
        {
            // explicit missing fromRef must not silently fall back to HEAD
        }
        assertFalse(branchExists("feature/bad"));
    }

    @Test
    public void createBranch_developUserDenied() throws Exception
    {
        initRepoWithOneCommit();
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(false);

        try
        {
            service.createBranch(REPOSITORY_ID, "feature/new", null);
            fail("expected ServiceException: manage required");
        }
        catch (ServiceException expected)
        {
            // develop 用户不得通过 API 创建分支
        }
        assertFalse(branchExists("feature/new"));
    }

    @Test
    public void deleteBranch_manageUser_deletesNonDefaultBranch() throws Exception
    {
        initRepoWithOneCommit();
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);
        service.createBranch(REPOSITORY_ID, "feature/delete", null);

        service.deleteBranch(REPOSITORY_ID, "feature/delete");

        assertFalse(branchExists("feature/delete"));
    }

    @Test
    public void deleteBranch_defaultBranchRejected() throws Exception
    {
        initRepoWithOneCommit();
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);

        try
        {
            service.deleteBranch(REPOSITORY_ID, "main");
            fail("expected ServiceException for default branch");
        }
        catch (ServiceException expected)
        {
            // 默认分支不可删除
        }
        assertTrue(branchExists("main"));
    }

    @Test
    public void setDefaultBranch_manageUser_persistsExistingBranch() throws Exception
    {
        initRepoWithOneCommit();
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);
        service.createBranch(REPOSITORY_ID, "core", null);
        ArgumentCaptor<SysGitRepository> captor = ArgumentCaptor.forClass(SysGitRepository.class);

        service.setDefaultBranch(REPOSITORY_ID, "core");

        verify(sysGitRepositoryMapper).updateSysGitRepository(captor.capture());
        assertEquals("core", captor.getValue().getDefaultBranch());
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
    public void countCommits_returnsFullReachableCountIndependentOfListLimit() throws Exception
    {
        initRepoWithMultiAuthorCommits();
        allowPull();

        List<GitCommitInfo> limited = service.listCommits(REPOSITORY_ID, "main", 2);
        assertEquals(2, limited.size());
        assertEquals(3, service.countCommits(REPOSITORY_ID, "main"));
        assertEquals(3, service.countCommits(REPOSITORY_ID, null));
    }

    @Test
    public void listCommits_emptyRepository_returnsEmptyList() throws Exception
    {
        initEmptyRepo();
        allowPull();

        assertTrue(service.listCommits(REPOSITORY_ID, null, null).isEmpty());
    }

    @Test
    public void listCommits_filterByAuthorEmail_returnsMatchingCommits() throws Exception
    {
        initRepoWithMultiAuthorCommits();
        allowPull();

        List<GitCommitInfo> alice = service.listCommits(REPOSITORY_ID, null, null, "alice@example.com", null, null);

        assertEquals(2, alice.size());
        assertEquals("alice-2", alice.get(0).getMessage());
        assertEquals("alice-1", alice.get(1).getMessage());
    }

    @Test
    public void listCommits_filterBySinceUntil_returnsCommitsInWindow() throws Exception
    {
        initRepoWithDatedCommits();
        allowPull();

        List<GitCommitInfo> mid = service.listCommits(REPOSITORY_ID, null, null, null, "2024-06-15", "2024-06-15");

        assertEquals(1, mid.size());
        assertEquals("mid", mid.get(0).getMessage());
    }

    @Test
    public void listCommits_filterThenLimit_appliesLimitAfterFilter() throws Exception
    {
        initRepoWithMultiAuthorCommits();
        allowPull();

        List<GitCommitInfo> limited = service.listCommits(REPOSITORY_ID, null, 1, "alice@example.com", null, null);

        assertEquals(1, limited.size());
        assertEquals("alice-2", limited.get(0).getMessage());
    }

    @Test
    public void listCommits_sinceAfterUntil_rejected() throws Exception
    {
        initRepoWithOneCommit();
        allowPull();

        try
        {
            service.listCommits(REPOSITORY_ID, null, null, null, "2024-06-20", "2024-06-10");
            fail("expected ServiceException for since after until");
        }
        catch (ServiceException expected)
        {
            // 日期区间非法
        }
    }

    @Test
    public void listBranches_noPullPermission_denied() throws Exception
    {
        initRepoWithOneCommit();
        when(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID)).thenReturn(false);

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
        verify(gitRepositoryAccessService, never()).canPull(eq(USER_ID), eq(REPOSITORY_ID));
    }

    @Test
    public void listContributors_twoDistinctEmails_returnsTwoWithCounts() throws Exception
    {
        initRepoWithMultiAuthorCommits();
        allowPull();
        when(sysUserProjectMapper.selectSysUserListByProjectId(eq(PROJECT_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Collections.emptyList());

        List<GitContributorInfo> contributors = service.listContributors(REPOSITORY_ID, null, null);

        assertEquals(2, contributors.size());
        GitContributorInfo alice = findContributor(contributors, "alice@example.com");
        GitContributorInfo bob = findContributor(contributors, "bob@example.com");
        assertNotNull(alice);
        assertNotNull(bob);
        assertEquals(2, alice.getCommitCount());
        assertEquals(1, bob.getCommitCount());
    }

    @Test
    public void listContributors_emailKeyMergesCaseAndDisplayNameVariants() throws Exception
    {
        initRepoWithMultiAuthorCommits();
        allowPull();
        when(sysUserProjectMapper.selectSysUserListByProjectId(eq(PROJECT_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Collections.emptyList());

        List<GitContributorInfo> contributors = service.listContributors(REPOSITORY_ID, null, null);

        assertEquals(2, contributors.size());
        GitContributorInfo alice = findContributor(contributors, "alice@example.com");
        assertEquals("Alice Latest", alice.getDisplayName());
    }

    @Test
    public void listContributors_noPullPermission_denied() throws Exception
    {
        initRepoWithOneCommit();
        when(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID)).thenReturn(false);

        try
        {
            service.listContributors(REPOSITORY_ID, null, null);
            fail("expected ServiceException: 无 pull 权限");
        }
        catch (ServiceException expected)
        {
            // 拒绝
        }
    }

    @Test
    public void listContributors_emptyRepository_returnsEmptyList() throws Exception
    {
        initEmptyRepo();
        allowPull();
        when(sysUserProjectMapper.selectSysUserListByProjectId(eq(PROJECT_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Collections.emptyList());

        assertTrue(service.listContributors(REPOSITORY_ID, null, null).isEmpty());
    }

    @Test
    public void getCommitDetail_textChange_includesUnifiedPatch() throws Exception
    {
        String commitId = initRepoWithTextModifyCommit();
        allowPull();

        GitCommitDetail detail = service.getCommitDetail(REPOSITORY_ID, commitId);

        assertEquals("update readme\n\nbody line", detail.getFullMessage());
        assertEquals(1, detail.getParents().size());
        assertEquals(1, detail.getStats().getFiles());
        assertEquals(1, detail.getFiles().size());
        GitCommitFileChange readme = detail.getFiles().get(0);
        assertEquals("README.md", readme.getPath());
        assertEquals("MODIFY", readme.getChangeType());
        assertFalse(readme.isBinary());
        assertFalse(readme.isTruncated());
        assertNotNull(readme.getPatch());
        assertTrue(readme.getPatch().contains("hello world"));
    }

    @Test
    public void getCommitDetail_abbreviatedCommitId_resolves() throws Exception
    {
        String fullId = initRepoWithTextModifyCommit();
        allowPull();

        GitCommitDetail detail = service.getCommitDetail(REPOSITORY_ID, fullId.substring(0, 7));

        assertEquals(fullId, detail.getId());
    }

    @Test
    public void getCommitDetail_binaryFile_omitsPatch() throws Exception
    {
        String commitId = initRepoWithBinaryFileCommit();
        allowPull();

        GitCommitDetail detail = service.getCommitDetail(REPOSITORY_ID, commitId);

        assertEquals(1, detail.getFiles().size());
        GitCommitFileChange bin = detail.getFiles().get(0);
        assertEquals("data.bin", bin.getPath());
        assertTrue(bin.isBinary());
        assertFalse(bin.isTruncated());
        assertTrue(bin.getPatch() == null || bin.getPatch().isEmpty());
    }

    @Test
    public void getCommitDetail_oversizedPatch_truncatedWithoutFullPatch() throws Exception
    {
        String commitId = initRepoWithHugeTextPatchCommit();
        allowPull();

        GitCommitDetail detail = service.getCommitDetail(REPOSITORY_ID, commitId);

        assertEquals(1, detail.getFiles().size());
        GitCommitFileChange large = detail.getFiles().get(0);
        assertEquals("large.txt", large.getPath());
        assertTrue(large.isTruncated());
        assertFalse(large.isBinary());
        assertTrue(large.getPatch() == null || large.getPatch().isEmpty());
    }

    @Test
    public void getCommitDetail_noPullPermission_denied() throws Exception
    {
        String commitId = initRepoWithTextModifyCommit();
        when(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID)).thenReturn(false);

        try
        {
            service.getCommitDetail(REPOSITORY_ID, commitId);
            fail("expected ServiceException: 无 pull 权限");
        }
        catch (ServiceException expected)
        {
            // 拒绝
        }
    }

    @Test
    public void getCommitDetail_unknownCommit_notFound() throws Exception
    {
        initRepoWithOneCommit();
        allowPull();

        try
        {
            service.getCommitDetail(REPOSITORY_ID, "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef");
            fail("expected ServiceException for unknown commit");
        }
        catch (ServiceException expected)
        {
            // 提交不存在
        }
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
        when(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID)).thenReturn(true);
    }

    private void mockRepository()
    {
        repositoryMetadata = new SysGitRepository();
        repositoryMetadata.setRepositoryId(REPOSITORY_ID);
        repositoryMetadata.setProjectId(PROJECT_ID);
        repositoryMetadata.setDiskPath(bareRepoDir.getAbsolutePath());
        repositoryMetadata.setDefaultBranch("main");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repositoryMetadata);
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

    /** 第二次提交修改 README，用于 commit detail 文本 diff */
    private String initRepoWithTextModifyCommit() throws Exception
    {
        initRepoWithOneCommit();
        File workDir = temporaryFolder.newFolder("work-mod");
        try (Git git = Git.cloneRepository()
            .setURI(bareRepoDir.getAbsolutePath())
            .setDirectory(workDir)
            .setBranch("main")
            .call())
        {
            Files.write(new File(workDir, "README.md").toPath(), "hello world".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern("README.md").call();
            org.eclipse.jgit.revwalk.RevCommit commit = git.commit()
                .setMessage("update readme\n\nbody line")
                .setAuthor(new PersonIdent("tester", "tester@example.com"))
                .call();
            git.push().call();
            return commit.getId().getName();
        }
    }

    private String initRepoWithBinaryFileCommit() throws Exception
    {
        initRepoWithOneCommit();
        File workDir = temporaryFolder.newFolder("work-bin");
        try (Git git = Git.cloneRepository()
            .setURI(bareRepoDir.getAbsolutePath())
            .setDirectory(workDir)
            .setBranch("main")
            .call())
        {
            byte[] payload = new byte[] { 0x00, 0x01, 0x02, 0x03 };
            Files.write(new File(workDir, "data.bin").toPath(), payload);
            git.add().addFilepattern("data.bin").call();
            org.eclipse.jgit.revwalk.RevCommit commit = git.commit()
                .setMessage("add binary")
                .setAuthor(new PersonIdent("tester", "tester@example.com"))
                .call();
            git.push().call();
            return commit.getId().getName();
        }
    }

    private String initRepoWithHugeTextPatchCommit() throws Exception
    {
        initRepoWithOneCommit();
        File workDir = temporaryFolder.newFolder("work-huge");
        try (Git git = Git.cloneRepository()
            .setURI(bareRepoDir.getAbsolutePath())
            .setDirectory(workDir)
            .setBranch("main")
            .call())
        {
            char[] chars = new char[SysGitBrowseServiceImpl.MAX_COMMIT_PATCH_PER_FILE_BYTES + 4096];
            java.util.Arrays.fill(chars, 'x');
            Files.write(new File(workDir, "large.txt").toPath(), new String(chars).getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern("large.txt").call();
            org.eclipse.jgit.revwalk.RevCommit commit = git.commit()
                .setMessage("add huge file")
                .setAuthor(new PersonIdent("tester", "tester@example.com"))
                .call();
            git.push().call();
            return commit.getId().getName();
        }
    }

    /** bare 仓：Alice 2 次提交、Bob 1 次；用于贡献者聚合 */
    private void initRepoWithMultiAuthorCommits() throws Exception
    {
        bareRepoDir = temporaryFolder.newFolder("multi.git");
        Git.init().setDirectory(bareRepoDir).setBare(true).call().close();

        File workDir = temporaryFolder.newFolder("multi-work");
        try (Git git = Git.init().setDirectory(workDir).setInitialBranch("main").call())
        {
            Files.write(new File(workDir, "README.md").toPath(), "v1".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit()
                .setMessage("alice-1")
                .setAuthor(new PersonIdent("Alice", "alice@example.com"))
                .call();
            Files.write(new File(workDir, "README.md").toPath(), "v2".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit()
                .setMessage("bob-1")
                .setAuthor(new PersonIdent("Bob", "bob@example.com"))
                .call();
            Files.write(new File(workDir, "README.md").toPath(), "v3".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit()
                .setMessage("alice-2")
                .setAuthor(new PersonIdent("Alice Latest", "Alice@Example.com"))
                .call();
            git.push().setRemote(bareRepoDir.getAbsolutePath()).setRefSpecs(
                new org.eclipse.jgit.transport.RefSpec("refs/heads/main:refs/heads/main")).call();
        }

        mockRepository();
    }

    /** bare 仓：三天各一条提交（2024-06-10 / 06-15 / 06-20） */
    private void initRepoWithDatedCommits() throws Exception
    {
        bareRepoDir = temporaryFolder.newFolder("dated.git");
        Git.init().setDirectory(bareRepoDir).setBare(true).call().close();

        java.util.TimeZone tz = java.util.TimeZone.getDefault();
        File workDir = temporaryFolder.newFolder("dated-work");
        try (Git git = Git.init().setDirectory(workDir).setInitialBranch("main").call())
        {
            commitWithWhen(git, workDir, "early.txt", "early",
                    new PersonIdent("A", "a@example.com",
                            java.sql.Timestamp.valueOf("2024-06-10 12:00:00"), tz));
            commitWithWhen(git, workDir, "mid.txt", "mid",
                    new PersonIdent("A", "a@example.com",
                            java.sql.Timestamp.valueOf("2024-06-15 12:00:00"), tz));
            commitWithWhen(git, workDir, "late.txt", "late",
                    new PersonIdent("A", "a@example.com",
                            java.sql.Timestamp.valueOf("2024-06-20 12:00:00"), tz));
            git.push().setRemote(bareRepoDir.getAbsolutePath()).setRefSpecs(
                    new org.eclipse.jgit.transport.RefSpec("refs/heads/main:refs/heads/main")).call();
        }

        mockRepository();
    }

    private static void commitWithWhen(Git git, File workDir, String fileName, String message, PersonIdent when)
            throws Exception
    {
        Files.write(new File(workDir, fileName).toPath(), message.getBytes(StandardCharsets.UTF_8));
        git.add().addFilepattern(fileName).call();
        git.commit().setMessage(message).setAuthor(when).setCommitter(when).call();
    }

    private void initRepoWithDivergentBranches() throws Exception
    {
        initRepoWithOneCommit();
        File workDir = temporaryFolder.newFolder("work-divergent");
        try (Git git = Git.cloneRepository()
                .setURI(bareRepoDir.getAbsolutePath())
                .setDirectory(workDir)
                .setBranch("main")
                .call())
        {
            git.checkout().setCreateBranch(true).setName("feature/demo").call();
            Files.write(new File(workDir, "feature.txt").toPath(), "feature".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern("feature.txt").call();
            git.commit().setMessage("feature commit")
                    .setAuthor(new PersonIdent("tester", "tester@example.com")).call();
            git.push().setRemote("origin").setRefSpecs(
                    new org.eclipse.jgit.transport.RefSpec("refs/heads/feature/demo:refs/heads/feature/demo")).call();

            git.checkout().setName("main").call();
            Files.write(new File(workDir, "main.txt").toPath(), "main".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern("main.txt").call();
            git.commit().setMessage("main commit")
                    .setAuthor(new PersonIdent("tester", "tester@example.com")).call();
            git.push().call();
        }
    }

    private boolean branchExists(String name) throws IOException
    {
        try (org.eclipse.jgit.lib.Repository repository = new org.eclipse.jgit.storage.file.FileRepositoryBuilder()
                .setGitDir(bareRepoDir).setBare().build())
        {
            return repository.exactRef("refs/heads/" + name) != null;
        }
    }

    private static GitBranchInfo findBranch(List<GitBranchInfo> branches, String name)
    {
        for (GitBranchInfo branch : branches)
        {
            if (name.equals(branch.getName()))
            {
                return branch;
            }
        }
        return null;
    }

    private static GitContributorInfo findContributor(List<GitContributorInfo> list, String email)
    {
        for (GitContributorInfo c : list)
        {
            if (email.equalsIgnoreCase(c.getEmail()))
            {
                return c;
            }
        }
        return null;
    }
}
