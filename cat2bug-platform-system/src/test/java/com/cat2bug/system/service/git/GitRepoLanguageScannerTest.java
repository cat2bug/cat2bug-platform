package com.cat2bug.system.service.git;

import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.git.browse.domain.GitLanguageShare;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * {@link GitRepoLanguageScanner} 单测（OpenSpec {@code git-repo-list-overview-aside} 任务 2.4）：
 * 覆盖文件名/扩展名到语言的映射、Top 9 +「其他」的合并归并算法，以及基于真实 bare 仓（JGit）的
 * 端到端扫描（跳过 {@code node_modules} 子树、单仓失败不影响整体成功）。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GitRepoLanguageScannerTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private GitPathValidator gitPathValidator;

    @InjectMocks
    private GitRepoLanguageScanner scanner;

    // ---------- mapPathToLanguage ----------

    @Test
    public void mapPathToLanguage_javaExtension_returnsJava()
    {
        assertEquals("Java", GitRepoLanguageScanner.mapPathToLanguage("Main.java"));
    }

    @Test
    public void mapPathToLanguage_dockerfileFilename_returnsDocker()
    {
        assertEquals("Docker", GitRepoLanguageScanner.mapPathToLanguage("Dockerfile"));
    }

    @Test
    public void mapPathToLanguage_dockerfileFilenameCaseInsensitive_returnsDocker()
    {
        assertEquals("Docker", GitRepoLanguageScanner.mapPathToLanguage("DOCKERFILE"));
    }

    @Test
    public void mapPathToLanguage_unknownExtension_returnsOther()
    {
        assertEquals(GitRepoLanguageScanner.OTHER_LANGUAGE_NAME,
            GitRepoLanguageScanner.mapPathToLanguage("notes.unknown"));
    }

    @Test
    public void mapPathToLanguage_noExtension_returnsOther()
    {
        assertEquals(GitRepoLanguageScanner.OTHER_LANGUAGE_NAME,
            GitRepoLanguageScanner.mapPathToLanguage("LICENSE"));
    }

    @Test
    public void mapPathToLanguage_nullFileName_returnsOther()
    {
        assertEquals(GitRepoLanguageScanner.OTHER_LANGUAGE_NAME, GitRepoLanguageScanner.mapPathToLanguage(null));
    }

    @Test
    public void mapPathToLanguage_emptyFileName_returnsOther()
    {
        assertEquals(GitRepoLanguageScanner.OTHER_LANGUAGE_NAME, GitRepoLanguageScanner.mapPathToLanguage(""));
    }

    // ---------- buildLanguageShares ----------

    @Test
    public void buildLanguageShares_emptyInput_returnsEmptyList()
    {
        assertTrue(GitRepoLanguageScanner.buildLanguageShares(new LinkedHashMap<>()).isEmpty());
        assertTrue(GitRepoLanguageScanner.buildLanguageShares(null).isEmpty());
    }

    @Test
    public void buildLanguageShares_dominantLanguage_getsHighestPercent()
    {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Java", 90L);
        counts.put("JavaScript", 10L);

        List<GitLanguageShare> result = GitRepoLanguageScanner.buildLanguageShares(counts);

        assertEquals(2, result.size());
        assertEquals("Java", result.get(0).getName());
        assertEquals(90, result.get(0).getPercent());
        assertEquals("JavaScript", result.get(1).getName());
        assertEquals(10, result.get(1).getPercent());
        assertEquals(100, sumPercent(result));
    }

    @Test
    public void buildLanguageShares_unmappedExtensionAlreadyRolledIntoOther_mergesWithBucket()
    {
        // 调用方在扫描阶段已把未知扩展名归为「其他」桶，这里验证该预累计值参与合并与百分比计算
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Java", 3L);
        counts.put(GitRepoLanguageScanner.OTHER_LANGUAGE_NAME, 1L);

        List<GitLanguageShare> result = GitRepoLanguageScanner.buildLanguageShares(counts);

        assertEquals(2, result.size());
        assertEquals("Java", result.get(0).getName());
        assertEquals(75, result.get(0).getPercent());
        assertEquals(GitRepoLanguageScanner.OTHER_LANGUAGE_NAME, result.get(1).getName());
        assertEquals(25, result.get(1).getPercent());
    }

    @Test
    public void buildLanguageShares_moreThanTopNineLanguages_tailMergesIntoOtherBucket()
    {
        Map<String, Long> counts = new LinkedHashMap<>();
        // 11 种不同语言，每种文件数递减，确保排序稳定：Lang00 最多，Lang10 最少
        for (int i = 0; i <= 10; i++)
        {
            counts.put(String.format("Lang%02d", i), (long) (100 - i));
        }

        List<GitLanguageShare> result = GitRepoLanguageScanner.buildLanguageShares(counts);

        // Top 9 + 合并后的「其他」= 10 条
        assertEquals(GitRepoLanguageScanner.TOP_LANGUAGE_COUNT + 1, result.size());
        for (int i = 0; i < GitRepoLanguageScanner.TOP_LANGUAGE_COUNT; i++)
        {
            assertEquals(String.format("Lang%02d", i), result.get(i).getName());
        }
        GitLanguageShare last = result.get(result.size() - 1);
        assertEquals(GitRepoLanguageScanner.OTHER_LANGUAGE_NAME, last.getName());
        assertEquals(100, sumPercent(result));
    }

    @Test
    public void buildLanguageShares_percentagesAlwaysSumToHundred()
    {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Java", 7L);
        counts.put("JavaScript", 3L);
        counts.put("Go", 3L);
        counts.put(GitRepoLanguageScanner.OTHER_LANGUAGE_NAME, 2L);

        List<GitLanguageShare> result = GitRepoLanguageScanner.buildLanguageShares(counts);

        assertEquals(100, sumPercent(result));
    }

    private static int sumPercent(List<GitLanguageShare> shares)
    {
        int sum = 0;
        for (GitLanguageShare share : shares)
        {
            sum += share.getPercent();
        }
        return sum;
    }

    // ---------- scan()：真实 bare 仓（JGit） ----------

    @Test
    public void scan_realRepository_countsFilesAndSkipsNodeModulesSubtree() throws Exception
    {
        when(gitPathValidator.resolveStoredDiskPath(anyString())).thenAnswer(inv -> inv.getArgument(0));

        File bareRepoDir = initRepoWithLanguageFiles();
        SysGitRepository repository = new SysGitRepository();
        repository.setRepositoryId(1L);
        repository.setDiskPath(bareRepoDir.getAbsolutePath());
        repository.setDefaultBranch("main");

        List<GitLanguageShare> result = scanner.scan(Collections.singletonList(repository));

        assertEquals(4, result.size());
        assertEquals("Docker", result.get(0).getName());
        assertEquals(25, result.get(0).getPercent());
        assertEquals("Java", result.get(1).getName());
        assertEquals(25, result.get(1).getPercent());
        assertEquals("JavaScript", result.get(2).getName());
        assertEquals(25, result.get(2).getPercent());
        assertEquals(GitRepoLanguageScanner.OTHER_LANGUAGE_NAME, result.get(3).getName());
        assertEquals(25, result.get(3).getPercent());
        assertEquals(100, sumPercent(result));
    }

    @Test
    public void scan_emptyRepositoryList_returnsEmptyList()
    {
        assertTrue(scanner.scan(Collections.emptyList()).isEmpty());
        assertTrue(scanner.scan(null).isEmpty());
    }

    @Test
    public void scan_emptyBareRepository_noDefaultBranchTip_returnsEmptyList() throws Exception
    {
        when(gitPathValidator.resolveStoredDiskPath(anyString())).thenAnswer(inv -> inv.getArgument(0));

        File bareRepoDir = temporaryFolder.newFolder("empty.git");
        Git.init().setDirectory(bareRepoDir).setBare(true).call().close();

        SysGitRepository repository = new SysGitRepository();
        repository.setRepositoryId(1L);
        repository.setDiskPath(bareRepoDir.getAbsolutePath());
        repository.setDefaultBranch("main");

        assertTrue(scanner.scan(Collections.singletonList(repository)).isEmpty());
    }

    @Test
    public void scan_partialFailure_badDiskPathSkippedOtherRepoStillScanned() throws Exception
    {
        when(gitPathValidator.resolveStoredDiskPath(anyString())).thenAnswer(inv -> inv.getArgument(0));

        File goodRepoDir = initRepoWithSingleJavaFile();
        SysGitRepository goodRepository = new SysGitRepository();
        goodRepository.setRepositoryId(1L);
        goodRepository.setDiskPath(goodRepoDir.getAbsolutePath());
        goodRepository.setDefaultBranch("main");

        SysGitRepository badRepository = new SysGitRepository();
        badRepository.setRepositoryId(2L);
        badRepository.setDiskPath(new File(temporaryFolder.getRoot(), "does-not-exist.git").getAbsolutePath());
        badRepository.setDefaultBranch("main");

        List<GitLanguageShare> result = scanner.scan(Arrays.asList(badRepository, goodRepository));

        assertEquals(1, result.size());
        assertEquals("Java", result.get(0).getName());
        assertEquals(100, result.get(0).getPercent());
    }

    @Test
    public void scan_diskPathResolutionThrows_repositorySkippedWithoutPropagating() throws Exception
    {
        File goodRepoDir = initRepoWithSingleJavaFile();
        SysGitRepository goodRepository = new SysGitRepository();
        goodRepository.setRepositoryId(1L);
        goodRepository.setDiskPath(goodRepoDir.getAbsolutePath());
        goodRepository.setDefaultBranch("main");

        SysGitRepository badRepository = new SysGitRepository();
        badRepository.setRepositoryId(2L);
        badRepository.setDiskPath("bad-path");
        badRepository.setDefaultBranch("main");

        when(gitPathValidator.resolveStoredDiskPath(goodRepoDir.getAbsolutePath())).thenReturn(goodRepoDir.getAbsolutePath());
        when(gitPathValidator.resolveStoredDiskPath("bad-path"))
            .thenThrow(new com.cat2bug.common.exception.ServiceException("i18n:git.repo.path-required"));

        List<GitLanguageShare> result = scanner.scan(Arrays.asList(badRepository, goodRepository));

        assertEquals(1, result.size());
        assertEquals("Java", result.get(0).getName());
    }

    // ---------- 测试夹具 ----------

    /** bare 仓：Main.java、app.js、Dockerfile、notes.unknown（其他），并含被跳过的 node_modules 子树 */
    private File initRepoWithLanguageFiles() throws Exception
    {
        File bareRepoDir = temporaryFolder.newFolder("lang.git");
        Git.init().setDirectory(bareRepoDir).setBare(true).call().close();

        File workDir = temporaryFolder.newFolder("lang-work");
        try (Git git = Git.init().setDirectory(workDir).setInitialBranch("main").call())
        {
            Files.write(new File(workDir, "Main.java").toPath(), "class Main {}".getBytes(StandardCharsets.UTF_8));
            Files.write(new File(workDir, "app.js").toPath(), "console.log(1);".getBytes(StandardCharsets.UTF_8));
            Files.write(new File(workDir, "Dockerfile").toPath(), "FROM openjdk".getBytes(StandardCharsets.UTF_8));
            Files.write(new File(workDir, "notes.unknown").toPath(), "n/a".getBytes(StandardCharsets.UTF_8));

            File nodeModulesDir = new File(workDir, "node_modules/dep");
            nodeModulesDir.mkdirs();
            Files.write(new File(nodeModulesDir, "index.js").toPath(), "module.exports = {};".getBytes(StandardCharsets.UTF_8));

            git.add().addFilepattern(".").call();
            git.commit()
                .setMessage("init")
                .setAuthor(new PersonIdent("tester", "tester@example.com"))
                .call();
            git.push().setRemote(bareRepoDir.getAbsolutePath()).setRefSpecs(
                new RefSpec("refs/heads/main:refs/heads/main")).call();
        }
        return bareRepoDir;
    }

    private File initRepoWithSingleJavaFile() throws Exception
    {
        File bareRepoDir = temporaryFolder.newFolder("single-java-" + System.nanoTime() + ".git");
        Git.init().setDirectory(bareRepoDir).setBare(true).call().close();

        File workDir = temporaryFolder.newFolder("single-java-work-" + System.nanoTime());
        try (Git git = Git.init().setDirectory(workDir).setInitialBranch("main").call())
        {
            Files.write(new File(workDir, "Main.java").toPath(), "class Main {}".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit()
                .setMessage("init")
                .setAuthor(new PersonIdent("tester", "tester@example.com"))
                .call();
            git.push().setRemote(bareRepoDir.getAbsolutePath()).setRefSpecs(
                new RefSpec("refs/heads/main:refs/heads/main")).call();
        }
        return bareRepoDir;
    }
}
