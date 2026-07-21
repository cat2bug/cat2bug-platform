package com.cat2bug.system.service.git;

import com.cat2bug.common.exception.ServiceException;
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
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * {@link GitReleaseTagHelper}：缺失 tag 创建、已有 tag 绑定、指向错误 commit 拒绝。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GitReleaseTagHelperTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Spy
    private GitPathValidator gitPathValidator = new GitPathValidator();

    @InjectMocks
    private GitReleaseTagHelper helper;

    private Runnable messageCleanup;
    private File bareRepoDir;
    private ObjectId commitA;
    private ObjectId commitB;

    @Before
    public void setUp() throws Exception
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        initRepoWithTwoCommits();
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
    public void ensureTagAtCommit_missingTag_createsLightweightTag() throws Exception
    {
        String bound = helper.ensureTagAtCommit(bareRepoDir.getAbsolutePath(), "v1.0.0", commitA);
        assertEquals(commitA.getName(), bound);
    }

    @Test
    public void ensureTagAtCommit_existingTagSameCommit_binds() throws Exception
    {
        helper.ensureTagAtCommit(bareRepoDir.getAbsolutePath(), "v1.0.0", commitA);
        String bound = helper.ensureTagAtCommit(bareRepoDir.getAbsolutePath(), "v1.0.0", commitA);
        assertEquals(commitA.getName(), bound);
    }

    @Test
    public void ensureTagAtCommit_existingTagWrongCommit_rejected() throws Exception
    {
        createTagManually("v1.0.0", commitA);
        try
        {
            helper.ensureTagAtCommit(bareRepoDir.getAbsolutePath(), "v1.0.0", commitB);
            fail("expected ServiceException for tag commit conflict");
        }
        catch (ServiceException expected)
        {
            // tag 已指向 commitA，拒绝绑定 commitB
        }
    }

    private void createTagManually(String tagName, ObjectId commitId) throws Exception
    {
        try (Repository repository = openRepo())
        {
            RefUpdate update = repository.updateRef("refs/tags/" + tagName);
            update.setNewObjectId(commitId);
            update.disableRefLog();
            update.update();
        }
    }

    private Repository openRepo() throws Exception
    {
        return new FileRepositoryBuilder().setGitDir(bareRepoDir).setBare().build();
    }

    private void initRepoWithTwoCommits() throws Exception
    {
        bareRepoDir = temporaryFolder.newFolder("repo.git");
        Git.init().setDirectory(bareRepoDir).setBare(true).call().close();

        File workDir = temporaryFolder.newFolder("work");
        try (Git git = Git.init().setDirectory(workDir).setInitialBranch("main").call())
        {
            Files.write(new File(workDir, "a.txt").toPath(), "a".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit().setMessage("first").setAuthor(new PersonIdent("t", "t@e.com")).call();
            commitA = git.getRepository().resolve("HEAD");

            Files.write(new File(workDir, "b.txt").toPath(), "b".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit().setMessage("second").setAuthor(new PersonIdent("t", "t@e.com")).call();
            commitB = git.getRepository().resolve("HEAD");

            git.push().setRemote(bareRepoDir.getAbsolutePath()).setPushAll().call();
        }
    }
}
