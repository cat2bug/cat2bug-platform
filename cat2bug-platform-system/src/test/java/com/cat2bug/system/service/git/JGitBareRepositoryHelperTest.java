package com.cat2bug.system.service.git;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link JGitBareRepositoryHelper#cloneBareRepository}：本地源仓 bare clone 与失败清理前置。
 */
public class JGitBareRepositoryHelperTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final JGitBareRepositoryHelper helper = new JGitBareRepositoryHelper();

    private Runnable messageCleanup;
    private File sourceWorkTree;

    @Before
    public void setUp() throws Exception
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        sourceWorkTree = temporaryFolder.newFolder("source-work");
        try (Git git = Git.init().setDirectory(sourceWorkTree).setInitialBranch("main").call())
        {
            File readme = new File(sourceWorkTree, "README.md");
            Files.write(readme.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern("README.md").call();
            PersonIdent ident = new PersonIdent("tester", "tester@example.com");
            git.commit().setAuthor(ident).setCommitter(ident).setMessage("init").call();
        }
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
    public void cloneBareRepository_fromLocalFileUrl_createsValidBareRepo() throws Exception
    {
        File target = new File(temporaryFolder.getRoot(), "imported.git");
        String sourceUrl = sourceWorkTree.toURI().toString();

        helper.cloneBareRepository(sourceUrl, target.getAbsolutePath(), null, null);

        assertTrue(helper.isValidBareRepository(target.getAbsolutePath()));
        assertEquals("main", helper.resolveDefaultBranch(target.getAbsolutePath()));
    }

    @Test
    public void cloneBareRepository_invalidSource_leavesNoUsableBare() throws Exception
    {
        File target = new File(temporaryFolder.getRoot(), "broken.git");
        try
        {
            helper.cloneBareRepository("https://127.0.0.1:1/does-not-exist.git",
                target.getAbsolutePath(), null, null);
            fail("expected ServiceException for unreachable remote");
        }
        catch (ServiceException expected)
        {
            // 网络/clone 失败
        }
        assertFalse(helper.isValidBareRepository(target.getAbsolutePath()));
    }
}
