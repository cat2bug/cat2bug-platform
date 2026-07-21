package com.cat2bug.system.git.pull.support;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class GitPullRequestMergeHelperTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void merge_divergedCleanHistories_createsMergeCommitAndAdvancesTarget() throws Exception
    {
        File bare = createDivergedRepository(false);
        try (org.eclipse.jgit.lib.Repository repository = new FileRepositoryBuilder().setGitDir(bare).setBare().build())
        {
            String before = repository.resolve("refs/heads/main").getName();
            GitPullRequestMergeHelper.MergeOutcome outcome = GitPullRequestMergeHelper.merge(
                    repository, "feature", "main", new PersonIdent("merger", "merger@example.com"));

            assertFalse(outcome.isConflict());
            assertNotEquals(before, outcome.getTargetSha());
            assertTrue(repository.resolve("refs/heads/main").getName().equals(outcome.getTargetSha()));
        }
    }

    @Test
    public void merge_conflictingHistories_doesNotAdvanceTarget() throws Exception
    {
        File bare = createDivergedRepository(true);
        try (org.eclipse.jgit.lib.Repository repository = new FileRepositoryBuilder().setGitDir(bare).setBare().build())
        {
            String before = repository.resolve("refs/heads/main").getName();
            GitPullRequestMergeHelper.MergeOutcome outcome = GitPullRequestMergeHelper.merge(
                    repository, "feature", "main", new PersonIdent("merger", "merger@example.com"));

            assertTrue(outcome.isConflict());
            assertTrue(before.equals(repository.resolve("refs/heads/main").getName()));
        }
    }

    private File createDivergedRepository(boolean conflict) throws Exception
    {
        File bare = temporaryFolder.newFolder("repo.git");
        Git.init().setDirectory(bare).setBare(true).call().close();
        File work = temporaryFolder.newFolder("work");
        try (Git git = Git.init().setDirectory(work).setInitialBranch("main").call())
        {
            File shared = new File(work, "shared.txt");
            Files.write(shared.toPath(), "base".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern(".").call();
            git.commit().setMessage("base").setAuthor(new PersonIdent("tester", "tester@example.com")).call();
            git.checkout().setCreateBranch(true).setName("feature").call();
            Files.write(shared.toPath(), (conflict ? "feature" : "base").getBytes(StandardCharsets.UTF_8));
            if (!conflict)
            {
                Files.write(new File(work, "feature.txt").toPath(), "feature".getBytes(StandardCharsets.UTF_8));
            }
            git.add().addFilepattern(".").call();
            git.commit().setMessage("feature").setAuthor(new PersonIdent("tester", "tester@example.com")).call();
            git.push().setRemote(bare.getAbsolutePath()).setRefSpecs(
                    new org.eclipse.jgit.transport.RefSpec("refs/heads/feature:refs/heads/feature")).call();
            git.checkout().setName("main").call();
            Files.write(shared.toPath(), (conflict ? "main" : "base").getBytes(StandardCharsets.UTF_8));
            if (!conflict)
            {
                Files.write(new File(work, "main.txt").toPath(), "main".getBytes(StandardCharsets.UTF_8));
            }
            git.add().addFilepattern(".").call();
            git.commit().setMessage("main").setAuthor(new PersonIdent("tester", "tester@example.com")).call();
            git.push().setRemote(bare.getAbsolutePath()).setRefSpecs(
                    new org.eclipse.jgit.transport.RefSpec("refs/heads/main:refs/heads/main")).call();
        }
        return bare;
    }
}
