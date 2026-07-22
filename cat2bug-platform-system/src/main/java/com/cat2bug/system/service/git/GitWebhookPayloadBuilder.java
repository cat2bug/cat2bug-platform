package com.cat2bug.system.service.git;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysGitPullRequest;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.SysGitRepositoryWebhook;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub 风格 Webhook JSON payload 构建。
 */
@Component
public class GitWebhookPayloadBuilder
{
    private static final int MAX_COMMITS = 20;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String buildPing(SysGitRepository repo, SysUser sender) throws JsonProcessingException
    {
        Map<String, Object> root = baseEnvelope(GitWebhookEventTypes.GITHUB_PING, repo, sender);
        root.put("zen", "Cat2Bug webhook ping");
        return objectMapper.writeValueAsString(root);
    }

    public String buildPush(SysGitRepository repo, SysUser sender, Repository git,
            String ref, ObjectId before, ObjectId after) throws IOException
    {
        Map<String, Object> root = baseEnvelope(GitWebhookEventTypes.GITHUB_PUSH, repo, sender);
        root.put("ref", ref);
        root.put("before", objectIdName(before));
        root.put("after", objectIdName(after));
        root.put("created", isZeroId(before) && !isZeroId(after));
        root.put("deleted", !isZeroId(before) && isZeroId(after));
        root.put("forced", false);
        List<Map<String, Object>> commits = listCommits(git, before, after);
        boolean truncated = commits.size() > MAX_COMMITS;
        if (truncated)
        {
            commits = commits.subList(0, MAX_COMMITS);
        }
        root.put("commits", commits);
        root.put("head_commit", commits.isEmpty() ? null : commits.get(commits.size() - 1));
        if (truncated)
        {
            root.put("truncated", true);
        }
        return objectMapper.writeValueAsString(root);
    }

    public String buildCreateTag(SysGitRepository repo, SysUser sender, String ref, ObjectId after)
            throws JsonProcessingException
    {
        Map<String, Object> root = baseEnvelope(GitWebhookEventTypes.GITHUB_CREATE, repo, sender);
        root.put("ref", shortRef(ref));
        root.put("ref_type", "tag");
        root.put("master_branch", repo.getDefaultBranch());
        root.put("description", null);
        root.put("pusher_type", "user");
        return objectMapper.writeValueAsString(root);
    }

    public String buildDeleteTag(SysGitRepository repo, SysUser sender, String ref)
            throws JsonProcessingException
    {
        Map<String, Object> root = baseEnvelope(GitWebhookEventTypes.GITHUB_DELETE, repo, sender);
        root.put("ref", shortRef(ref));
        root.put("ref_type", "tag");
        root.put("pusher_type", "user");
        return objectMapper.writeValueAsString(root);
    }

    public String buildPullRequest(SysGitRepository repo, SysUser sender, SysGitPullRequest pr,
            String action, boolean merged) throws JsonProcessingException
    {
        Map<String, Object> root = baseEnvelope(GitWebhookEventTypes.GITHUB_PULL_REQUEST, repo, sender);
        root.put("action", action);
        Map<String, Object> pullRequest = new LinkedHashMap<>();
        pullRequest.put("number", pr.getNumber());
        pullRequest.put("title", pr.getTitle());
        pullRequest.put("body", pr.getBody());
        pullRequest.put("state", prStateLabel(pr.getState()));
        pullRequest.put("merged", merged);
        pullRequest.put("head", branchRef(pr.getSourceBranch(), pr.getSourceSha()));
        pullRequest.put("base", branchRef(pr.getTargetBranch(), pr.getTargetSha()));
        root.put("pull_request", pullRequest);
        return objectMapper.writeValueAsString(root);
    }

    public static List<String> parseSubscriptionEvents(String eventsJson)
    {
        if (StringUtils.isEmpty(eventsJson))
        {
            return List.of();
        }
        try
        {
            return new ObjectMapper().readValue(eventsJson, new TypeReference<List<String>>() { });
        }
        catch (JsonProcessingException e)
        {
            return List.of();
        }
    }

    public static String serializeSubscriptionEvents(List<String> events) throws JsonProcessingException
    {
        return new ObjectMapper().writeValueAsString(events);
    }

    public static boolean subscribes(SysGitRepositoryWebhook row, String subscription)
    {
        if (row == null || !row.isActiveFlag())
        {
            return false;
        }
        return parseSubscriptionEvents(row.getEvents()).contains(subscription);
    }

    private Map<String, Object> baseEnvelope(String eventName, SysGitRepository repo, SysUser sender)
    {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("repository", repositoryBlock(repo));
        root.put("sender", senderBlock(sender));
        return root;
    }

    private Map<String, Object> repositoryBlock(SysGitRepository repo)
    {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("id", repo.getRepositoryId());
        block.put("name", repo.getSlug());
        String fullName = fullName(repo);
        block.put("full_name", fullName);
        block.put("html_url", htmlUrl(repo));
        block.put("clone_url", repo.getCloneUrl());
        block.put("default_branch", repo.getDefaultBranch());
        block.put("private", true);
        return block;
    }

    private static String fullName(SysGitRepository repo)
    {
        if (StringUtils.isNotEmpty(repo.getTeamKey()) && StringUtils.isNotEmpty(repo.getProjectKey())
                && StringUtils.isNotEmpty(repo.getSlug()))
        {
            return repo.getTeamKey() + "/" + repo.getProjectKey() + "/" + repo.getSlug();
        }
        return repo.getSlug();
    }

    private static String htmlUrl(SysGitRepository repo)
    {
        String clone = repo.getCloneUrl();
        if (clone == null)
        {
            return null;
        }
        if (clone.endsWith(".git"))
        {
            return clone.substring(0, clone.length() - 4);
        }
        return clone;
    }

    private Map<String, Object> senderBlock(SysUser sender)
    {
        Map<String, Object> block = new LinkedHashMap<>();
        if (sender == null)
        {
            block.put("login", "ghost");
            block.put("id", 0);
            return block;
        }
        block.put("id", sender.getUserId());
        block.put("login", StringUtils.defaultIfBlank(sender.getUserName(), "user"));
        block.put("name", sender.getNickName());
        block.put("email", sender.getEmail());
        return block;
    }

    private static Map<String, Object> branchRef(String branch, String sha)
    {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("ref", "refs/heads/" + branch);
        ref.put("sha", sha);
        return ref;
    }

    private static String prStateLabel(String state)
    {
        if ("merged".equals(state))
        {
            return "closed";
        }
        return state;
    }

    private List<Map<String, Object>> listCommits(Repository git, ObjectId before, ObjectId after)
            throws IOException
    {
        List<Map<String, Object>> result = new ArrayList<>();
        if (isZeroId(after) || git == null)
        {
            return result;
        }
        try (RevWalk walk = new RevWalk(git))
        {
            RevCommit head = walk.parseCommit(after);
            walk.markStart(head);
            if (!isZeroId(before))
            {
                try
                {
                    walk.markUninteresting(walk.parseCommit(before));
                }
                catch (IOException ignore)
                {
                    // before 不可解析时仍尽量列出 head 链
                }
            }
            for (RevCommit commit : walk)
            {
                result.add(commitEntry(commit));
                if (result.size() > MAX_COMMITS)
                {
                    break;
                }
            }
        }
        // RevWalk 从 head 向 before 走，顺序为 newest-first；GitHub push 为 oldest-first
        result.sort((a, b) -> {
            String idA = (String) a.get("id");
            String idB = (String) b.get("id");
            return idA.compareTo(idB);
        });
        return result;
    }

    private Map<String, Object> commitEntry(RevCommit commit)
    {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", commit.getId().getName());
        PersonIdent author = commit.getAuthorIdent();
        if (author != null)
        {
            entry.put("message", commit.getShortMessage());
            entry.put("timestamp", iso8601(author.getWhen()));
            Map<String, Object> authorBlock = new LinkedHashMap<>();
            authorBlock.put("name", author.getName());
            authorBlock.put("email", author.getEmailAddress());
            entry.put("author", authorBlock);
        }
        return entry;
    }

    private static String iso8601(Date when)
    {
        if (when == null)
        {
            return null;
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(when);
    }

    private static String objectIdName(ObjectId id)
    {
        if (id == null || ObjectId.zeroId().equals(id))
        {
            return ObjectId.zeroId().getName();
        }
        return id.getName();
    }

    private static boolean isZeroId(ObjectId id)
    {
        return id == null || ObjectId.zeroId().equals(id);
    }

    private static String shortRef(String ref)
    {
        if (ref == null)
        {
            return null;
        }
        if (ref.startsWith("refs/tags/"))
        {
            return ref.substring("refs/tags/".length());
        }
        if (ref.startsWith("refs/heads/"))
        {
            return ref.substring("refs/heads/".length());
        }
        return ref;
    }
}
