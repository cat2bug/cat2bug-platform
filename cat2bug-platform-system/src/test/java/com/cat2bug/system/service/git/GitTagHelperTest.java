package com.cat2bug.system.service.git;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.git.browse.domain.GitTagInfo;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
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
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link GitTagHelper}：list / annotated create / duplicate / delete。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GitTagHelperTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Spy
    private GitPathValidator gitPathValidator = new GitPathValidator();

    @InjectMocks
    private GitReleaseTagHelper gitReleaseTagHelper;

    private GitTagHelper helper;

    private Runnable messageCleanup;
    private File bareRepoDir;
    private ObjectId headCommit;

    @Before
    public void setUp() throws Exception
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        helper = new GitTagHelper();
        setField(helper, "gitPathValidator", gitPathValidator);
        setField(helper, "gitReleaseTagHelper", gitReleaseTagHelper);
        initRepoWithOneCommit();
    }

    @After
    public void tearDown()
    {
        if (messageCleanup != null)
        {
            messageCleanup.run();
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void listTags_filterByNameSubstring() throws Exception
    {
        PersonIdent tagger = new PersonIdent("t", "t@e.com");
        helper.createAnnotatedTag(bareRepoDir.getAbsolutePath(), "v1.0.0", "one", headCommit, tagger);
        helper.createAnnotatedTag(bareRepoDir.getAbsolutePath(), "v2.0.0", "two", headCommit, tagger);
        helper.createAnnotatedTag(bareRepoDir.getAbsolutePath(), "beta-1", "beta", headCommit, tagger);

        List<GitTagInfo> filtered = helper.listTags(bareRepoDir.getAbsolutePath(), "v1");
        assertEquals(1, filtered.size());
        assertEquals("v1.0.0", filtered.get(0).getName());

        assertEquals(3, helper.listTags(bareRepoDir.getAbsolutePath(), null).size());
    }

    @Test
    public void createAnnotatedTag_writesMessageAndPeelsToCommit() throws Exception
    {
        GitTagInfo created = helper.createAnnotatedTag(
                bareRepoDir.getAbsolutePath(), "v1.0.0", "First stable", headCommit,
                new PersonIdent("admin", "a@e.com"));

        assertEquals("v1.0.0", created.getName());
        assertTrue(created.isAnnotated());
        assertEquals("First stable", created.getMessage());
        assertEquals(headCommit.getName(), created.getCommitId());

        try (Repository repository = openRepo();
             RevWalk walk = new RevWalk(repository))
        {
            Ref ref = repository.exactRef("refs/tags/v1.0.0");
            assertNotNull(ref);
            RevTag tag = walk.parseTag(ref.getObjectId());
            assertEquals("First stable", tag.getFullMessage().trim());
            assertEquals(headCommit, walk.parseCommit(tag.getObject().getId()));
        }
    }

    @Test
    public void createAnnotatedTag_duplicate_rejected() throws Exception
    {
        PersonIdent tagger = new PersonIdent("t", "t@e.com");
        helper.createAnnotatedTag(bareRepoDir.getAbsolutePath(), "v1.0.0", "", headCommit, tagger);
        try
        {
            helper.createAnnotatedTag(bareRepoDir.getAbsolutePath(), "v1.0.0", "again", headCommit, tagger);
            fail("expected duplicate conflict");
        }
        catch (ServiceException expected)
        {
            assertEquals(Integer.valueOf(HttpStatus.CONFLICT), expected.getCode());
        }
    }

    @Test
    public void deleteTag_removesRef() throws Exception
    {
        helper.createAnnotatedTag(bareRepoDir.getAbsolutePath(), "v0.1.0", "", headCommit,
                new PersonIdent("t", "t@e.com"));
        helper.deleteTag(bareRepoDir.getAbsolutePath(), "v0.1.0");
        assertFalse(helper.tagExists(bareRepoDir.getAbsolutePath(), "v0.1.0"));
    }

    private Repository openRepo() throws Exception
    {
        return new FileRepositoryBuilder().setGitDir(bareRepoDir).setBare().build();
    }

    private void initRepoWithOneCommit() throws Exception
    {
        bareRepoDir = temporaryFolder.newFolder("repo.git");
        Git.init().setDirectory(bareRepoDir).setBare(true).call().close();

        File workDir = temporaryFolder.newFolder("work");
        try (Git git = Git.init().setDirectory(workDir).setInitialBranch("main").call())
        {
            Files.write(new File(workDir, "a.txt").toPath(), "a".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit().setMessage("init").setAuthor(new PersonIdent("t", "t@e.com")).call();
            headCommit = git.getRepository().resolve("HEAD");
            git.push().setRemote(bareRepoDir.getAbsolutePath()).setPushAll().call();
        }
    }
}
