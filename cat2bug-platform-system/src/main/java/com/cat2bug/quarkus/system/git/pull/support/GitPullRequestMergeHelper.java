package com.cat2bug.quarkus.system.git.pull.support;

import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.Merger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;

/**
 * 在 bare repository 上执行不依赖工作区的三路合并。
 */
public final class GitPullRequestMergeHelper
{
    private GitPullRequestMergeHelper()
    {
    }

    public static MergeOutcome merge(
            Repository repository, String sourceBranch, String targetBranch, PersonIdent committer) throws IOException
    {
        Ref sourceRef = repository.exactRef(Constants.R_HEADS + sourceBranch);
        Ref targetRef = repository.exactRef(Constants.R_HEADS + targetBranch);
        if (sourceRef == null || targetRef == null || sourceRef.getObjectId() == null || targetRef.getObjectId() == null)
        {
            throw new IOException("Pull request branch does not exist");
        }
        ObjectId sourceId = sourceRef.getObjectId();
        ObjectId targetId = targetRef.getObjectId();

        try (RevWalk walk = new RevWalk(repository))
        {
            RevCommit source = walk.parseCommit(sourceId);
            RevCommit target = walk.parseCommit(targetId);
            if (walk.isMergedInto(source, target))
            {
                return MergeOutcome.success(targetId.getName());
            }
            if (walk.isMergedInto(target, source))
            {
                updateTargetRef(repository, targetBranch, targetId, sourceId);
                return MergeOutcome.success(sourceId.getName());
            }
        }

        Merger merger = MergeStrategy.RECURSIVE.newMerger(repository, true);
        if (!merger.merge(targetId, sourceId))
        {
            return MergeOutcome.conflict();
        }
        ObjectId resultTree = merger.getResultTreeId();
        if (resultTree == null)
        {
            return MergeOutcome.conflict();
        }

        ObjectId mergeCommit;
        try (ObjectInserter inserter = repository.newObjectInserter())
        {
            CommitBuilder builder = new CommitBuilder();
            builder.setTreeId(resultTree);
            builder.setParentIds(targetId, sourceId);
            builder.setAuthor(committer);
            builder.setCommitter(committer);
            builder.setMessage("Merge branch '" + sourceBranch + "' into " + targetBranch);
            mergeCommit = inserter.insert(builder);
            inserter.flush();
        }
        updateTargetRef(repository, targetBranch, targetId, mergeCommit);
        return MergeOutcome.success(mergeCommit.getName());
    }

    private static void updateTargetRef(
            Repository repository, String targetBranch, ObjectId expectedOld, ObjectId newId) throws IOException
    {
        RefUpdate update = repository.updateRef(Constants.R_HEADS + targetBranch);
        update.setExpectedOldObjectId(expectedOld);
        update.setNewObjectId(newId);
        update.disableRefLog();
        RefUpdate.Result result = update.update();
        if (result != RefUpdate.Result.FAST_FORWARD && result != RefUpdate.Result.NEW
                && result != RefUpdate.Result.NO_CHANGE)
        {
            throw new IOException("Target branch changed during merge: " + result);
        }
    }

    public static final class MergeOutcome
    {
        private final boolean conflict;
        private final String targetSha;

        private MergeOutcome(boolean conflict, String targetSha)
        {
            this.conflict = conflict;
            this.targetSha = targetSha;
        }

        public boolean isConflict()
        {
            return conflict;
        }

        public String getTargetSha()
        {
            return targetSha;
        }

        private static MergeOutcome conflict()
        {
            return new MergeOutcome(true, null);
        }

        private static MergeOutcome success(String targetSha)
        {
            return new MergeOutcome(false, targetSha);
        }
    }
}
