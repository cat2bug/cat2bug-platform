package com.cat2bug.system.service.git;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link GitPathValidator} 单测：slug 格式、默认路径拼装、绝对路径规范化/穿越拒绝/黑名单、父目录可写性。
 * 对应 tasks 2.6「path validation」验收项。
 */
public class GitPathValidatorTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final GitPathValidator validator = new GitPathValidator();

    private Runnable messageCleanup;

    @Before
    public void setUp()
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
    }

    @After
    public void tearDown()
    {
        if (messageCleanup != null)
        {
            messageCleanup.run();
        }
    }

    @Test
    public void validateSlug_validValues_returnedTrimmed()
    {
        assertEquals("api", validator.validateSlug(" api "));
        assertEquals("api-v2", validator.validateSlug("api-v2"));
        assertEquals("api_v2", validator.validateSlug("api_v2"));
        assertEquals("a.b", validator.validateSlug("a.b"));
        assertEquals("a", validator.validateSlug("a"));
    }

    @Test
    public void validateSlug_blank_rejected()
    {
        assertThrowsServiceException(() -> validator.validateSlug(""));
        assertThrowsServiceException(() -> validator.validateSlug(null));
    }

    @Test
    public void validateSlug_traversalOrSeparators_rejected()
    {
        assertThrowsServiceException(() -> validator.validateSlug(".."));
        assertThrowsServiceException(() -> validator.validateSlug("a/b"));
        assertThrowsServiceException(() -> validator.validateSlug("../etc"));
    }

    @Test
    public void validateSlug_leadingOrTrailingHyphen_rejected()
    {
        assertThrowsServiceException(() -> validator.validateSlug("-abc"));
        assertThrowsServiceException(() -> validator.validateSlug("abc-"));
    }

    @Test
    public void buildDefaultDiskPath_composesFromConfiguredGitRoot()
    {
        withGitRoot(temporaryFolder.getRoot().getAbsolutePath(), () -> {
            String expected = Paths.get(Cat2BugConfig.getGitRootPath(), "5", "api.git").toString();
            assertEquals(expected, validator.buildDefaultDiskPath(5L, "api"));
        });
    }

    @Test
    public void buildDefaultDiskPath_distinctSlugsUnderSameProject_produceDistinctPaths()
    {
        withGitRoot(temporaryFolder.getRoot().getAbsolutePath(), () -> {
            String pathA = validator.buildDefaultDiskPath(5L, "api");
            String pathB = validator.buildDefaultDiskPath(5L, "web");
            assertFalse(pathA.equals(pathB));
        });
    }

    @Test
    public void resolveStoredDiskPath_absolutePath_normalized()
    {
        String raw = temporaryFolder.getRoot().getAbsolutePath() + File.separator + "." + File.separator + "repo.git";
        String resolved = validator.resolveStoredDiskPath(raw);
        assertEquals(Paths.get(raw).normalize().toString(), resolved);
        assertTrue(Paths.get(resolved).isAbsolute());
    }

    @Test
    public void resolveStoredDiskPath_relativePath_absolutized()
    {
        String resolved = validator.resolveStoredDiskPath("relative/repo.git");
        assertTrue(Paths.get(resolved).isAbsolute());
        assertTrue(resolved.endsWith("relative" + File.separator + "repo.git")
            || resolved.endsWith("relative/repo.git"));
    }

    @Test
    public void resolveStoredDiskPath_blank_rejected()
    {
        assertThrowsServiceException(() -> validator.resolveStoredDiskPath(""));
        assertThrowsServiceException(() -> validator.resolveStoredDiskPath(null));
    }

    @Test
    public void normalizeAndValidateAbsolutePath_validAbsolutePath_normalized()
    {
        String raw = temporaryFolder.getRoot().getAbsolutePath() + File.separator + "repo.git";
        String normalized = validator.normalizeAndValidateAbsolutePath(raw);
        assertEquals(Paths.get(raw).normalize().toString(), normalized);
    }

    @Test
    public void normalizeAndValidateAbsolutePath_relativePath_rejected()
    {
        assertThrowsServiceException(() -> validator.normalizeAndValidateAbsolutePath("relative/path/repo.git"));
    }

    @Test
    public void normalizeAndValidateAbsolutePath_blank_rejected()
    {
        assertThrowsServiceException(() -> validator.normalizeAndValidateAbsolutePath(""));
        assertThrowsServiceException(() -> validator.normalizeAndValidateAbsolutePath(null));
    }

    @Test
    public void normalizeAndValidateAbsolutePath_containsDotDotSegment_rejected()
    {
        String raw = temporaryFolder.getRoot().getAbsolutePath() + "/../etc/repo.git";
        assertThrowsServiceException(() -> validator.normalizeAndValidateAbsolutePath(raw));
    }

    @Test
    public void normalizeAndValidateAbsolutePath_denyListRoot_rejected()
    {
        assertThrowsServiceException(() -> validator.normalizeAndValidateAbsolutePath("/etc/cat2bug-git.git"));
        assertThrowsServiceException(() -> validator.normalizeAndValidateAbsolutePath("/System/cat2bug-git.git"));
        assertThrowsServiceException(() -> validator.normalizeAndValidateAbsolutePath("/"));
    }

    @Test
    public void ensureParentWritable_createsMissingParentDirectory()
    {
        File target = new File(temporaryFolder.getRoot(), "nested/repo.git");
        assertFalse(target.getParentFile().exists());
        validator.ensureParentWritable(target.getAbsolutePath());
        assertTrue(target.getParentFile().exists());
        assertTrue(target.getParentFile().canWrite());
    }

    @Test
    public void ensureParentWritable_nullParent_rejected()
    {
        // 单段路径（如 "repo.git" 的绝对形式 "/repo.git"）父目录是根目录，可写性由文件系统决定；
        // 这里构造一个真正没有父路径概念的输入来触发防御性分支。
        assertThrowsServiceException(() -> validator.ensureParentWritable(""));
    }

    private void withGitRoot(String profile, Runnable action)
    {
        Cat2BugConfig config = new Cat2BugConfig();
        config.setProfile(profile);
        Cat2BugConfig.Git git = new Cat2BugConfig.Git();
        git.setRoot("git");
        config.setGit(git);
        action.run();
    }

    private void assertThrowsServiceException(Runnable action)
    {
        try
        {
            action.run();
            fail("expected ServiceException");
        }
        catch (ServiceException expected)
        {
            // 期望的校验失败
        }
    }
}
