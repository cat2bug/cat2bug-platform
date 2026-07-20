package com.cat2bug.system.service.git;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.MessageUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 校验 receive-pack 引入的新提交：author/committer 必须与当前推送账号一致。
 */
public final class GitPushCommitAuthorValidator
{
    private GitPushCommitAuthorValidator()
    {
    }

    /**
     * @return 拒绝原因；{@code null} 表示通过
     */
    public static String findRejectReason(Repository repository, ObjectId oldId, ObjectId newId, SysUser pusher)
            throws IOException
    {
        if (pusher == null)
        {
            return MessageUtils.message("git.repo.push.author-required");
        }
        if (newId == null || ObjectId.zeroId().equals(newId))
        {
            return null;
        }

        try (RevWalk walk = new RevWalk(repository))
        {
            walk.setRetainBody(true);
            walk.markStart(walk.parseCommit(newId));
            if (oldId != null && !ObjectId.zeroId().equals(oldId))
            {
                walk.markUninteresting(walk.parseCommit(oldId));
            }
            else
            {
                markExistingTipsUninteresting(repository, walk, newId);
            }

            for (RevCommit commit : walk)
            {
                if (!GitCommitIdentityMatcher.matches(commit.getAuthorIdent(), pusher))
                {
                    return MessageUtils.message("git.repo.push.author-not-pusher",
                            GitCommitIdentityMatcher.formatPerson(commit.getAuthorIdent()),
                            shortId(commit),
                            formatExpected(pusher));
                }
                if (!GitCommitIdentityMatcher.matches(commit.getCommitterIdent(), pusher))
                {
                    return MessageUtils.message("git.repo.push.committer-not-pusher",
                            GitCommitIdentityMatcher.formatPerson(commit.getCommitterIdent()),
                            shortId(commit),
                            formatExpected(pusher));
                }
            }
        }
        return null;
    }

    private static void markExistingTipsUninteresting(Repository repository, RevWalk walk, ObjectId newId)
            throws IOException
    {
        List<Ref> refs = new ArrayList<>();
        refs.addAll(repository.getRefDatabase().getRefsByPrefix(Constants.R_HEADS));
        refs.addAll(repository.getRefDatabase().getRefsByPrefix(Constants.R_TAGS));
        for (Ref ref : refs)
        {
            ObjectId objectId = ref.getObjectId();
            if (objectId == null || objectId.equals(newId))
            {
                continue;
            }
            try
            {
                walk.markUninteresting(walk.parseCommit(objectId));
            }
            catch (IOException ignored)
            {
                // tag 可能指向非 commit，跳过
            }
        }
    }

    private static String shortId(RevCommit commit)
    {
        return commit.getName().substring(0, 7);
    }

    private static String formatExpected(SysUser user)
    {
        String name = user.getUserName() != null ? user.getUserName() : "?";
        String email = user.getEmail() != null ? user.getEmail() : "";
        return name + " <" + email + ">";
    }
}
