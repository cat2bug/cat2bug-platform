package com.cat2bug.system.service.git;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 推送提交作者校验：非推送账号身份的新提交必须被拒绝。
 */
public class GitPushCommitAuthorValidatorTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Runnable messageCleanup;
    private Repository repository;
    private ObjectId seedId;

    @Before
    public void setUp() throws Exception
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        File work = temporaryFolder.newFolder("work");
        Git git = Git.init().setDirectory(work).call();
        repository = git.getRepository();
        PersonIdent demo = person("demo", "demo@example.com");
        RevCommit seed = git.commit().setMessage("seed").setAuthor(demo).setCommitter(demo)
                .setAllowEmpty(true).call();
        seedId = seed.copy();
    }

    @After
    public void tearDown()
    {
        if (repository != null)
        {
            repository.close();
        }
        if (messageCleanup != null)
        {
            messageCleanup.run();
        }
    }

    @Test
    public void rejectsCommit_whenAuthorNotPusher() throws Exception
    {
        PersonIdent foreign = person("t", "t@local");
        ObjectId foreignId;
        try (Git git = Git.wrap(repository))
        {
            RevCommit foreignCommit = git.commit().setMessage("by-t").setAuthor(foreign).setCommitter(foreign)
                    .setAllowEmpty(true).call();
            foreignId = foreignCommit.copy();
            assertTrue("author=" + foreignCommit.getAuthorIdent().getName(),
                    "t".equals(foreignCommit.getAuthorIdent().getName()));
        }

        SysUser pusher = demoUser();
        String reason = GitPushCommitAuthorValidator.findRejectReason(repository, seedId, foreignId, pusher);
        assertNotNull("应拒绝作者 t 的提交", reason);
        assertTrue(reason.contains("t") || reason.contains("author"));
    }



    @Test
    public void acceptsCommit_whenAuthorMatchesPusher() throws Exception
    {
        PersonIdent demo = person("demo", "demo@example.com");
        ObjectId okId;
        try (Git git = Git.wrap(repository))
        {
            RevCommit ok = git.commit().setMessage("by-demo").setAuthor(demo).setCommitter(demo)
                    .setAllowEmpty(true).call();
            okId = ok.copy();
        }

        String reason = GitPushCommitAuthorValidator.findRejectReason(repository, seedId, okId, demoUser());
        assertNull(reason);
    }

    private static SysUser demoUser()
    {
        SysUser pusher = new SysUser();
        pusher.setUserName("demo");
        pusher.setNickName("演示");
        pusher.setEmail("demo@example.com");
        return pusher;
    }

    private static PersonIdent person(String name, String email)
    {
        return new PersonIdent(name, email, new Date(1_700_000_000_000L), TimeZone.getTimeZone("UTC"));
    }
}
