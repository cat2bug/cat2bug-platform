package com.cat2bug.system.service.git;

import com.cat2bug.system.domain.SysGitPullRequest;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.SysGitRepositoryWebhook;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.mapper.SysGitPullRequestMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysGitRepositoryWebhookMapper;
import com.cat2bug.system.mapper.SysUserMapper;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 将 Git/PR 事件映射为 Webhook 投递任务（异步 HTTP，失败不影响主流程）。
 */
@Service
public class GitWebhookEnqueueService
{
    private static final Logger log = LoggerFactory.getLogger(GitWebhookEnqueueService.class);

    private final SysGitRepositoryWebhookMapper webhookMapper;
    private final SysGitRepositoryMapper repositoryMapper;
    private final SysGitPullRequestMapper pullRequestMapper;
    private final SysUserMapper sysUserMapper;
    private final GitWebhookPayloadBuilder payloadBuilder;
    private final GitWebhookDeliveryService deliveryService;

    public GitWebhookEnqueueService(SysGitRepositoryWebhookMapper webhookMapper,
            SysGitRepositoryMapper repositoryMapper,
            SysGitPullRequestMapper pullRequestMapper,
            SysUserMapper sysUserMapper,
            GitWebhookPayloadBuilder payloadBuilder,
            GitWebhookDeliveryService deliveryService)
    {
        this.webhookMapper = webhookMapper;
        this.repositoryMapper = repositoryMapper;
        this.pullRequestMapper = pullRequestMapper;
        this.sysUserMapper = sysUserMapper;
        this.payloadBuilder = payloadBuilder;
        this.deliveryService = deliveryService;
    }

    public void onReceivePackSuccessful(Long repositoryId, Long pusherUserId, Repository git,
            Collection<ReceiveCommand> commands)
    {
        try
        {
            SysGitRepository repo = repositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
            if (repo == null)
            {
                return;
            }
            SysUser sender = loadUser(pusherUserId);
            List<SysGitRepositoryWebhook> hooks = webhookMapper.selectByRepositoryId(repositoryId);
            if (hooks.isEmpty())
            {
                return;
            }
            for (ReceiveCommand cmd : commands)
            {
                if (cmd.getResult() != ReceiveCommand.Result.OK)
                {
                    continue;
                }
                String ref = cmd.getRefName();
                ObjectId oldId = cmd.getOldId();
                ObjectId newId = cmd.getNewId();
                if (ref.startsWith("refs/tags/"))
                {
                    dispatchTagEvents(repo, sender, git, hooks, ref, oldId, newId);
                }
                else if (ref.startsWith("refs/heads/"))
                {
                    dispatchBranchPush(repo, sender, git, hooks, ref, oldId, newId);
                    maybeSynchronizePullRequests(repo, sender, hooks, ref);
                }
            }
        }
        catch (Exception e)
        {
            log.warn("Webhook receive-pack 入队失败：repositoryId={} {}", repositoryId, e.toString());
        }
    }

    public void onPullRequestOpened(Long repositoryId, SysGitPullRequest pr, Long actorUserId)
    {
        enqueuePullRequest(repositoryId, pr, actorUserId, "opened", false);
    }

    public void onPullRequestSynchronize(Long repositoryId, SysGitPullRequest pr, Long actorUserId)
    {
        enqueuePullRequest(repositoryId, pr, actorUserId, "synchronize", false);
    }

    public void onPullRequestClosed(Long repositoryId, SysGitPullRequest pr, Long actorUserId, boolean merged)
    {
        enqueuePullRequest(repositoryId, pr, actorUserId, "closed", merged);
    }

    public void enqueuePing(Long repositoryId, Long webhookId)
    {
        SysGitRepository repo = repositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        SysGitRepositoryWebhook hook = webhookMapper.selectByRepositoryIdAndWebhookId(repositoryId, webhookId);
        if (repo == null || hook == null || !hook.isActiveFlag())
        {
            return;
        }
        try
        {
            String body = payloadBuilder.buildPing(repo, null);
            deliveryService.deliverAsync(hook.getWebhookId(), GitWebhookEventTypes.GITHUB_PING, body);
        }
        catch (Exception e)
        {
            log.warn("Webhook ping 入队失败：webhookId={} {}", webhookId, e.toString());
        }
    }

    private void dispatchBranchPush(SysGitRepository repo, SysUser sender, Repository git,
            List<SysGitRepositoryWebhook> hooks, String ref, ObjectId oldId, ObjectId newId) throws Exception
    {
        String body = payloadBuilder.buildPush(repo, sender, git, ref, oldId, newId);
        for (SysGitRepositoryWebhook hook : hooks)
        {
            if (GitWebhookPayloadBuilder.subscribes(hook, GitWebhookEventTypes.SUBSCRIPTION_PUSH))
            {
                deliveryService.deliverAsync(hook.getWebhookId(), GitWebhookEventTypes.GITHUB_PUSH, body);
            }
        }
    }

    private void dispatchTagEvents(SysGitRepository repo, SysUser sender, Repository git,
            List<SysGitRepositoryWebhook> hooks, String ref, ObjectId oldId, ObjectId newId) throws Exception
    {
        boolean created = ObjectId.zeroId().equals(oldId) && !ObjectId.zeroId().equals(newId);
        boolean deleted = !ObjectId.zeroId().equals(oldId) && ObjectId.zeroId().equals(newId);
        if (created)
        {
            String body = payloadBuilder.buildCreateTag(repo, sender, ref, newId);
            for (SysGitRepositoryWebhook hook : hooks)
            {
                if (GitWebhookPayloadBuilder.subscribes(hook, GitWebhookEventTypes.SUBSCRIPTION_TAG))
                {
                    deliveryService.deliverAsync(hook.getWebhookId(), GitWebhookEventTypes.GITHUB_CREATE, body);
                }
            }
        }
        else if (deleted)
        {
            String body = payloadBuilder.buildDeleteTag(repo, sender, ref);
            for (SysGitRepositoryWebhook hook : hooks)
            {
                if (GitWebhookPayloadBuilder.subscribes(hook, GitWebhookEventTypes.SUBSCRIPTION_TAG))
                {
                    deliveryService.deliverAsync(hook.getWebhookId(), GitWebhookEventTypes.GITHUB_DELETE, body);
                }
            }
        }
        else
        {
            String body = payloadBuilder.buildPush(repo, sender, git, ref, oldId, newId);
            for (SysGitRepositoryWebhook hook : hooks)
            {
                if (GitWebhookPayloadBuilder.subscribes(hook, GitWebhookEventTypes.SUBSCRIPTION_TAG))
                {
                    deliveryService.deliverAsync(hook.getWebhookId(), GitWebhookEventTypes.GITHUB_PUSH, body);
                }
            }
        }
    }

    private void maybeSynchronizePullRequests(SysGitRepository repo, SysUser sender,
            List<SysGitRepositoryWebhook> hooks, String ref)
    {
        if (!ref.startsWith("refs/heads/"))
        {
            return;
        }
        String branch = ref.substring("refs/heads/".length());
        List<SysGitPullRequest> open = pullRequestMapper.selectOpenBySourceBranch(repo.getRepositoryId(), branch);
        if (open.isEmpty())
        {
            return;
        }
        for (SysGitPullRequest pr : open)
        {
            try
            {
                String body = payloadBuilder.buildPullRequest(repo, sender, pr, "synchronize", false);
                for (SysGitRepositoryWebhook hook : hooks)
                {
                    if (GitWebhookPayloadBuilder.subscribes(hook, GitWebhookEventTypes.SUBSCRIPTION_PULL_REQUEST))
                    {
                        deliveryService.deliverAsync(hook.getWebhookId(), GitWebhookEventTypes.GITHUB_PULL_REQUEST,
                                body);
                    }
                }
            }
            catch (Exception e)
            {
                log.warn("Webhook PR synchronize 入队失败：pr={} {}", pr.getNumber(), e.toString());
            }
        }
    }

    private void enqueuePullRequest(Long repositoryId, SysGitPullRequest pr, Long actorUserId, String action,
            boolean merged)
    {
        try
        {
            SysGitRepository repo = repositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
            if (repo == null)
            {
                return;
            }
            SysUser sender = loadUser(actorUserId);
            String body = payloadBuilder.buildPullRequest(repo, sender, pr, action, merged);
            List<SysGitRepositoryWebhook> hooks = webhookMapper.selectByRepositoryId(repositoryId);
            for (SysGitRepositoryWebhook hook : hooks)
            {
                if (GitWebhookPayloadBuilder.subscribes(hook, GitWebhookEventTypes.SUBSCRIPTION_PULL_REQUEST))
                {
                    deliveryService.deliverAsync(hook.getWebhookId(), GitWebhookEventTypes.GITHUB_PULL_REQUEST, body);
                }
            }
        }
        catch (Exception e)
        {
            log.warn("Webhook PR 入队失败：repositoryId={} action={} {}", repositoryId, action, e.toString());
        }
    }

    private SysUser loadUser(Long userId)
    {
        if (userId == null)
        {
            return null;
        }
        return sysUserMapper.selectUserById(null, null, userId);
    }
}
