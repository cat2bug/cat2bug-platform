package com.cat2bug.system.service.git;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.MessageUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PreReceiveHook;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceivePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

/**
 * receive-pack 前置钩子：拒绝 author/committer 与当前推送账号不一致的新提交。
 *
 * <p>推送账号已通过项目角色鉴权，因此「与推送人一致」同时保证「提交身份是项目成员」。
 * 本地 {@code user.name}/{@code user.email} 配成非成员（如 {@code t}）时会被拒绝。</p>
 */
public final class GitReceivePackCommitAuthorHook implements PreReceiveHook
{
    private static final Logger log = LoggerFactory.getLogger(GitReceivePackCommitAuthorHook.class);

    private final SysUser pusher;

    public GitReceivePackCommitAuthorHook(SysUser pusher)
    {
        this.pusher = pusher;
    }

    @Override
    public void onPreReceive(ReceivePack rp, Collection<ReceiveCommand> commands)
    {
        if (pusher == null)
        {
            for (ReceiveCommand cmd : commands)
            {
                if (cmd.getResult() == ReceiveCommand.Result.NOT_ATTEMPTED)
                {
                    cmd.setResult(ReceiveCommand.Result.REJECTED_OTHER_REASON,
                            MessageUtils.message("git.repo.push.author-required"));
                }
            }
            return;
        }

        Repository repository = rp.getRepository();
        for (ReceiveCommand cmd : commands)
        {
            if (cmd.getResult() != ReceiveCommand.Result.NOT_ATTEMPTED)
            {
                continue;
            }
            if (cmd.getType() == ReceiveCommand.Type.DELETE)
            {
                continue;
            }
            try
            {
                String rejection = GitPushCommitAuthorValidator.findRejectReason(
                        repository, cmd.getOldId(), cmd.getNewId(), pusher);
                if (rejection != null)
                {
                    cmd.setResult(ReceiveCommand.Result.REJECTED_OTHER_REASON, rejection);
                }
            }
            catch (IOException e)
            {
                log.warn("校验提交作者失败：ref={}, err={}", cmd.getRefName(), e.toString());
                cmd.setResult(ReceiveCommand.Result.REJECTED_OTHER_REASON,
                        MessageUtils.message("git.repo.push.author-check-failed"));
            }
        }
    }
}
