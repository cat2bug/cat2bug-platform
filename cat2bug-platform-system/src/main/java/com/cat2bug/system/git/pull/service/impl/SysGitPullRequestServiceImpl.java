package com.cat2bug.system.git.pull.service.impl;
import jakarta.inject.Inject;

import jakarta.enterprise.context.ApplicationScoped;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.DateUtils;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.framework.security.SecurityUtils;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.common.domain.SysGitPullRequest;
import com.cat2bug.common.domain.SysGitPullRequestComment;
import com.cat2bug.common.domain.SysGitRepository;
import com.cat2bug.system.git.browse.domain.GitCommitFileChange;
import com.cat2bug.system.git.browse.domain.GitCommitInfo;
import com.cat2bug.system.git.pull.service.ISysGitPullRequestService;
import com.cat2bug.system.git.pull.support.GitPullRequestMergeHelper;
import com.cat2bug.system.mapper.SysGitPullRequestCommentMapper;
import com.cat2bug.system.mapper.SysGitPullRequestMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.service.git.GitPathValidator;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.service.git.GitWebhookEnqueueService;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import jakarta.transaction.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Git Pull Request 服务。Git refs 是提交内容真相，数据库仅保存协作状态和讨论。
 */
@ApplicationScoped
public class SysGitPullRequestServiceImpl implements ISysGitPullRequestService
{
    private static final String STATE_OPEN = "open";
    private static final String STATE_CLOSED = "closed";
    private static final String STATE_MERGED = "merged";
    private static final int MAX_PATCH_PER_FILE_BYTES = 256 * 1024;
    private static final int MAX_PATCH_CUMULATIVE_BYTES = 3 * 512 * 1024;
    private static final int MAX_PATCH_FILE_COUNT = 500;

    @Inject
    private SysGitRepositoryMapper sysGitRepositoryMapper;
    @Inject
    private SysGitPullRequestMapper sysGitPullRequestMapper;
    @Inject
    private SysGitPullRequestCommentMapper sysGitPullRequestCommentMapper;
    @Inject
    private GitRepositoryAccessService gitRepositoryAccessService;
    @Inject
    private GitPathValidator gitPathValidator;
    @Inject
    private GitWebhookEnqueueService gitWebhookEnqueueService;

    @Override
    @Transactional
    public SysGitPullRequest createPullRequest(Long repositoryId, SysGitPullRequest input)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPush(repo.getRepositoryId());
        validateCreate(input);
        String source = normalizeBranch(input.getSourceBranch());
        String target = normalizeBranch(input.getTargetBranch());
        if (source.equals(target))
        {
            throw badRequest("git.pull.source-target-same");
        }
        String sourceSha;
        String targetSha;
        try (Repository git = openRepository(repo))
        {
            sourceSha = requireBranchTip(git, source).getName();
            targetSha = requireBranchTip(git, target).getName();
        }
        catch (IOException e)
        {
            throw readFailed(e);
        }

        SysGitPullRequest row = new SysGitPullRequest();
        Date now = DateUtils.getNowDate();
        row.setRepositoryId(repositoryId);
        row.setTitle(input.getTitle().trim());
        row.setBody(input.getBody());
        row.setSourceBranch(source);
        row.setTargetBranch(target);
        row.setSourceSha(sourceSha);
        row.setTargetSha(targetSha);
        row.setState(STATE_OPEN);
        row.setAuthorId(SecurityUtils.getUserId());
        row.setCreateBy(SecurityUtils.getUsername());
        row.setCreateTime(now);
        for (int attempt = 0; attempt < 3; attempt++)
        {
            row.setNumber(nextNumber(repositoryId));
            try
            {
                if (sysGitPullRequestMapper.insert(row) <= 0)
                {
                    throw new ApiException(MessageUtils.message("git.pull.create-failed"), HttpStatus.ERROR);
                }
                gitWebhookEnqueueService.onPullRequestOpened(repositoryId, row, SecurityUtils.getUserId());
                return row;
            }
            catch (org.apache.ibatis.exceptions.PersistenceException ignore)
            {
                // 编号唯一约束与重新读取 max(number) 共同处理并发创建。
            }
        }
        throw new ApiException(MessageUtils.message("git.pull.create-conflict"), HttpStatus.CONFLICT);
    }

    @Override
    public List<SysGitPullRequest> listPullRequests(Long repositoryId, String state)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getRepositoryId());
        if (StringUtils.isNotBlank(state) && !isState(state))
        {
            throw badRequest("git.pull.state-invalid");
        }
        return sysGitPullRequestMapper.selectListByRepo(repositoryId, state);
    }

    @Override
    public SysGitPullRequest getPullRequest(Long repositoryId, Integer number)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getRepositoryId());
        return requirePullRequest(repositoryId, number);
    }

    @Override
    @Transactional
    public void closePullRequest(Long repositoryId, Integer number)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        SysGitPullRequest pullRequest = requirePullRequest(repositoryId, number);
        Long userId = SecurityUtils.getUserId();
        if (!(userId.equals(pullRequest.getAuthorId()) && gitRepositoryAccessService.canPush(userId, repo.getRepositoryId()))
                && !gitRepositoryAccessService.canManage(userId, repo.getRepositoryId()))
        {
            throw forbidden();
        }
        requireOpen(pullRequest);
        pullRequest.setState(STATE_CLOSED);
        pullRequest.setClosedAt(DateUtils.getNowDate());
        pullRequest.setUpdateBy(SecurityUtils.getUsername());
        pullRequest.setUpdateTime(DateUtils.getNowDate());
        sysGitPullRequestMapper.updateStateFields(pullRequest);
        gitWebhookEnqueueService.onPullRequestClosed(repositoryId, pullRequest, userId, false);
    }

    @Override
    public List<GitCommitInfo> listCommits(Long repositoryId, Integer number)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getRepositoryId());
        SysGitPullRequest pullRequest = requirePullRequest(repositoryId, number);
        List<GitCommitInfo> result = new ArrayList<>();
        try (Repository git = openRepository(repo); RevWalk walk = new RevWalk(git))
        {
            RevCommit source = walk.parseCommit(requireBranchTip(git, pullRequest.getSourceBranch()));
            RevCommit target = walk.parseCommit(requireBranchTip(git, pullRequest.getTargetBranch()));
            walk.markStart(source);
            walk.markUninteresting(target);
            for (RevCommit commit : walk)
            {
                result.add(toCommitInfo(commit));
            }
            return result;
        }
        catch (IOException e)
        {
            throw readFailed(e);
        }
    }

    @Override
    public List<GitCommitFileChange> listFiles(Long repositoryId, Integer number)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getRepositoryId());
        SysGitPullRequest pullRequest = requirePullRequest(repositoryId, number);
        try (Repository git = openRepository(repo); RevWalk walk = new RevWalk(git))
        {
            RevCommit source = walk.parseCommit(requireBranchTip(git, pullRequest.getSourceBranch()));
            RevCommit target = walk.parseCommit(requireBranchTip(git, pullRequest.getTargetBranch()));
            walk.reset();
            walk.setRevFilter(RevFilter.MERGE_BASE);
            walk.markStart(source);
            walk.markStart(target);
            RevCommit mergeBase = walk.next();
            // Prefer three-dot (merge-base..source). Unrelated histories have no merge-base;
            // fall back to two-dot (target..source) so the files tab still renders a usable diff.
            RevCommit oldCommit = mergeBase != null ? walk.parseCommit(mergeBase) : target;
            return buildFileChanges(git, oldCommit, source);
        }
        catch (IOException e)
        {
            throw readFailed(e);
        }
    }

    @Override
    public List<SysGitPullRequestComment> listComments(Long repositoryId, Integer number)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getRepositoryId());
        return sysGitPullRequestCommentMapper.selectListByPullRequestId(
                requirePullRequest(repositoryId, number).getPullRequestId());
    }

    @Override
    @Transactional
    public SysGitPullRequestComment createComment(
            Long repositoryId, Integer number, SysGitPullRequestComment input)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPush(repo.getRepositoryId());
        SysGitPullRequest pullRequest = requirePullRequest(repositoryId, number);
        if (input == null || StringUtils.isBlank(input.getBody()))
        {
            throw badRequest("git.pull.comment-required");
        }
        if (input.getParentId() != null)
        {
            boolean parentExists = false;
            for (SysGitPullRequestComment existing : sysGitPullRequestCommentMapper
                    .selectListByPullRequestId(pullRequest.getPullRequestId()))
            {
                if (input.getParentId().equals(existing.getCommentId()))
                {
                    parentExists = true;
                    break;
                }
            }
            if (!parentExists)
            {
                throw badRequest("git.pull.comment-parent-invalid");
            }
        }
        Date now = DateUtils.getNowDate();
        input.setPullRequestId(pullRequest.getPullRequestId());
        input.setUserId(SecurityUtils.getUserId());
        input.setBody(input.getBody().trim());
        input.setCreateTime(now);
        input.setUpdateTime(now);
        sysGitPullRequestCommentMapper.insert(input);
        return input;
    }

    @Override
    @Transactional
    public SysGitPullRequest mergePullRequest(Long repositoryId, Integer number)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertManage(repo.getRepositoryId());
        SysGitPullRequest pullRequest = requirePullRequest(repositoryId, number);
        requireOpen(pullRequest);
        try (Repository git = openRepository(repo))
        {
            PersonIdent identity = new PersonIdent(
                    StringUtils.defaultIfBlank(SecurityUtils.getUsername(), "Cat2Bug"),
                    "noreply@cat2bug.local");
            GitPullRequestMergeHelper.MergeOutcome outcome = GitPullRequestMergeHelper.merge(
                    git, pullRequest.getSourceBranch(), pullRequest.getTargetBranch(), identity);
            if (outcome.isConflict())
            {
                throw new ApiException(MessageUtils.message("git.pull.merge-conflict"), HttpStatus.CONFLICT);
            }
            pullRequest.setState(STATE_MERGED);
            pullRequest.setTargetSha(outcome.getTargetSha());
            pullRequest.setSourceSha(requireBranchTip(git, pullRequest.getSourceBranch()).getName());
            pullRequest.setMergedBy(SecurityUtils.getUserId());
            pullRequest.setMergedAt(DateUtils.getNowDate());
            pullRequest.setUpdateBy(SecurityUtils.getUsername());
            pullRequest.setUpdateTime(DateUtils.getNowDate());
            sysGitPullRequestMapper.updateStateFields(pullRequest);
            gitWebhookEnqueueService.onPullRequestClosed(repositoryId, pullRequest, SecurityUtils.getUserId(), true);
            return pullRequest;
        }
        catch (ApiException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw readFailed(e);
        }
    }

    private List<GitCommitFileChange> buildFileChanges(Repository git, RevCommit base, RevCommit source)
            throws IOException
    {
        List<GitCommitFileChange> result = new ArrayList<>();
        try (ObjectReader reader = git.newObjectReader())
        {
            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            oldTree.reset(reader, base.getTree());
            CanonicalTreeParser newTree = new CanonicalTreeParser();
            newTree.reset(reader, source.getTree());
            try (DiffFormatter headers = new DiffFormatter(DisabledOutputStream.INSTANCE))
            {
                headers.setRepository(git);
                headers.setDetectRenames(true);
                List<DiffEntry> entries = headers.scan(oldTree, newTree);
                int cumulative = 0;
                int attached = 0;
                for (DiffEntry entry : entries)
                {
                    GitCommitFileChange change = new GitCommitFileChange();
                    change.setChangeType(entry.getChangeType().name());
                    change.setPath(normalizePath(entry.getChangeType() == DiffEntry.ChangeType.DELETE
                            ? entry.getOldPath() : entry.getNewPath()));
                    if (entry.getChangeType() == DiffEntry.ChangeType.RENAME
                            || entry.getChangeType() == DiffEntry.ChangeType.COPY)
                    {
                        change.setOldPath(normalizePath(entry.getOldPath()));
                    }
                    FileHeader header = headers.toFileHeader(entry);
                    applyStats(header, change);
                    if (header.getPatchType() == FileHeader.PatchType.BINARY
                            || header.getPatchType() == FileHeader.PatchType.GIT_BINARY)
                    {
                        change.setBinary(true);
                    }
                    else if (attached >= MAX_PATCH_FILE_COUNT)
                    {
                        change.setTruncated(true);
                    }
                    else
                    {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        try (DiffFormatter patch = new DiffFormatter(out))
                        {
                            patch.setRepository(git);
                            patch.setDetectRenames(true);
                            patch.format(entry);
                        }
                        byte[] bytes = out.toByteArray();
                        if (!isValidUtf8(bytes))
                        {
                            change.setBinary(true);
                        }
                        else if (bytes.length > MAX_PATCH_PER_FILE_BYTES || cumulative + bytes.length > MAX_PATCH_CUMULATIVE_BYTES)
                        {
                            change.setTruncated(true);
                        }
                        else
                        {
                            change.setPatch(new String(bytes, StandardCharsets.UTF_8));
                            cumulative += bytes.length;
                            attached++;
                        }
                    }
                    result.add(change);
                }
            }
        }
        return result;
    }

    private static void applyStats(FileHeader header, GitCommitFileChange change)
    {
        int additions = 0;
        int deletions = 0;
        for (org.eclipse.jgit.diff.Edit edit : header.toEditList())
        {
            additions += edit.getLengthB();
            deletions += edit.getLengthA();
        }
        change.setAdditions(additions);
        change.setDeletions(deletions);
    }

    private static GitCommitInfo toCommitInfo(RevCommit commit)
    {
        GitCommitInfo info = new GitCommitInfo();
        info.setId(commit.getId().getName());
        info.setShortId(commit.getId().abbreviate(7).name());
        info.setMessage(commit.getShortMessage());
        PersonIdent author = commit.getAuthorIdent();
        if (author != null)
        {
            info.setAuthorName(author.getName());
            info.setAuthorEmail(author.getEmailAddress());
            info.setCommitTime(author.getWhen());
        }
        return info;
    }

    private int nextNumber(Long repositoryId)
    {
        Integer max = sysGitPullRequestMapper.selectMaxNumberByRepo(repositoryId);
        return (max == null ? 0 : max) + 1;
    }

    private SysGitRepository requireRepo(Long repositoryId)
    {
        SysGitRepository repo = repositoryId == null ? null
                : sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repo == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.not-found"), HttpStatus.NOT_FOUND);
        }
        return repo;
    }

    private SysGitPullRequest requirePullRequest(Long repositoryId, Integer number)
    {
        SysGitPullRequest result = number == null ? null
                : sysGitPullRequestMapper.selectByRepoAndNumber(repositoryId, number);
        if (result == null)
        {
            throw new ApiException(MessageUtils.message("git.pull.not-found"), HttpStatus.NOT_FOUND);
        }
        return result;
    }

    private Repository openRepository(SysGitRepository repo)
    {
        if (StringUtils.isBlank(repo.getDiskPath()))
        {
            throw new ApiException(MessageUtils.message("git.repo.browse.disk-missing"), HttpStatus.NOT_FOUND);
        }
        try
        {
            return new FileRepositoryBuilder()
                    .setGitDir(new File(gitPathValidator.resolveStoredDiskPath(repo.getDiskPath()))).setBare().build();
        }
        catch (IOException e)
        {
            throw readFailed(e);
        }
    }

    private ObjectId requireBranchTip(Repository git, String branch) throws IOException
    {
        ObjectId id = git.resolve(Constants.R_HEADS + branch);
        if (id == null)
        {
            throw new ApiException(MessageUtils.message("git.pull.branch-not-found"), HttpStatus.NOT_FOUND);
        }
        return id;
    }

    private void assertPull(Long repositoryId)
    {
        if (!gitRepositoryAccessService.canPull(SecurityUtils.getUserId(), repositoryId))
        {
            throw forbidden();
        }
    }

    private void assertPush(Long repositoryId)
    {
        if (!gitRepositoryAccessService.canPush(SecurityUtils.getUserId(), repositoryId))
        {
            throw forbidden();
        }
    }

    private void assertManage(Long repositoryId)
    {
        if (!gitRepositoryAccessService.canManage(SecurityUtils.getUserId(), repositoryId))
        {
            throw forbidden();
        }
    }

    private static void validateCreate(SysGitPullRequest input)
    {
        if (input == null || StringUtils.isBlank(input.getTitle()) || StringUtils.isBlank(input.getSourceBranch())
                || StringUtils.isBlank(input.getTargetBranch()))
        {
            throw badRequest("git.pull.required");
        }
    }

    private static String normalizeBranch(String branch)
    {
        String value = branch.trim();
        if (!Repository.isValidRefName(Constants.R_HEADS + value) || value.startsWith(Constants.R_REFS))
        {
            throw badRequest("git.pull.branch-invalid");
        }
        return value;
    }

    private static boolean isState(String state)
    {
        return STATE_OPEN.equals(state) || STATE_CLOSED.equals(state) || STATE_MERGED.equals(state);
    }

    private static void requireOpen(SysGitPullRequest pullRequest)
    {
        if (!STATE_OPEN.equals(pullRequest.getState()))
        {
            throw badRequest("git.pull.not-open");
        }
    }

    private static String normalizePath(String path)
    {
        return path == null || DiffEntry.DEV_NULL.equals(path) ? null : path.replaceFirst("^/", "");
    }

    private static boolean isValidUtf8(byte[] bytes)
    {
        try
        {
            StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes));
            return true;
        }
        catch (CharacterCodingException e)
        {
            return false;
        }
    }

    private static ApiException forbidden()
    {
        return new ApiException(MessageUtils.message("no-permission"), HttpStatus.FORBIDDEN);
    }

    private static ApiException badRequest(String messageKey)
    {
        return new ApiException(MessageUtils.message(messageKey), HttpStatus.BAD_REQUEST);
    }

    private static ApiException readFailed(Exception cause)
    {
        return new ApiException(MessageUtils.message("git.repo.browse.read-failed"), HttpStatus.ERROR);
    }
}
