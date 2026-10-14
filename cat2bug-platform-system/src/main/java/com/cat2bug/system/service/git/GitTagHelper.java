package com.cat2bug.system.service.git;
import jakarta.inject.Inject;

import jakarta.enterprise.context.ApplicationScoped;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.git.browse.domain.GitTagInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 仓库 Tags 页 JGit 操作：列出/创建 annotated/删除 tag。
 * 名称校验与目标 commit 解析复用 {@link GitReleaseTagHelper}；发行版路径的轻量 tag 创建保持不变。
 */
@ApplicationScoped
public class GitTagHelper
{
    @Inject
    private GitPathValidator gitPathValidator;

    @Inject
    private GitReleaseTagHelper gitReleaseTagHelper;

    /**
     * 列出 {@code refs/tags/*}；可选按名称子串过滤（不区分大小写）。
     */
    public List<GitTagInfo> listTags(String diskPath, String nameFilter) throws IOException
    {
        List<GitTagInfo> result = new ArrayList<>();
        try (Repository repository = openRepository(diskPath);
             RevWalk walk = new RevWalk(repository))
        {
            List<Ref> refs = repository.getRefDatabase().getRefsByPrefix(Constants.R_TAGS);
            String filter = StringUtils.isNotBlank(nameFilter)
                    ? nameFilter.trim().toLowerCase(Locale.ROOT) : null;
            for (Ref ref : refs)
            {
                String shortName = Repository.shortenRefName(ref.getName());
                if (filter != null && !shortName.toLowerCase(Locale.ROOT).contains(filter))
                {
                    continue;
                }
                result.add(toTagInfo(repository, walk, ref, shortName));
            }
        }
        result.sort(Comparator.comparing(GitTagInfo::getName));
        return result;
    }

    /**
     * 创建 annotated tag；名称已存在则拒绝。{@code message} 允许空串。
     *
     * @return 新建标签信息（不含 releaseId）
     */
    public GitTagInfo createAnnotatedTag(
            String diskPath,
            String tagName,
            String message,
            ObjectId commitId,
            PersonIdent tagger) throws IOException
    {
        gitReleaseTagHelper.validateTagName(tagName);
        String trimmed = tagName.trim();
        String tagMessage = message != null ? message : "";
        try (Repository repository = openRepository(diskPath);
             RevWalk walk = new RevWalk(repository);
             Git git = Git.wrap(repository))
        {
            String refName = Constants.R_TAGS + trimmed;
            if (repository.exactRef(refName) != null)
            {
                throw new ApiException(MessageUtils.message("git.repo.tag.already-exists"), HttpStatus.CONFLICT);
            }
            RevCommit commit = walk.parseCommit(commitId);
            PersonIdent ident = tagger != null ? tagger : new PersonIdent("Cat2Bug", "noreply@cat2bug.local");
            try
            {
                git.tag()
                        .setName(trimmed)
                        .setObjectId(commit)
                        .setAnnotated(true)
                        .setMessage(tagMessage)
                        .setTagger(ident)
                        .call();
            }
            catch (ApiException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new ApiException(MessageUtils.message("git.repo.tag.create-failed"), HttpStatus.ERROR);
            }
            Ref created = repository.exactRef(refName);
            if (created == null)
            {
                throw new ApiException(MessageUtils.message("git.repo.tag.create-failed"), HttpStatus.ERROR);
            }
            return toTagInfo(repository, walk, created, trimmed);
        }
    }

    /**
     * 删除 {@code refs/tags/<name>}；不存在则 404。
     *
     * @return 删除前 tip commit id（用于 webhook），找不到 tip 时仍删除成功则返回 null
     */
    public ObjectId deleteTag(String diskPath, String tagName) throws IOException
    {
        gitReleaseTagHelper.validateTagName(tagName);
        String trimmed = tagName.trim();
        try (Repository repository = openRepository(diskPath))
        {
            String refName = Constants.R_TAGS + trimmed;
            Ref existing = repository.exactRef(refName);
            if (existing == null)
            {
                throw new ApiException(MessageUtils.message("git.repo.tag.not-found"), HttpStatus.NOT_FOUND);
            }
            ObjectId tip = peelToCommit(repository, existing);
            RefUpdate update = repository.updateRef(refName);
            update.setForceUpdate(true);
            update.disableRefLog();
            RefUpdate.Result result = update.delete();
            if (result != RefUpdate.Result.FORCED && result != RefUpdate.Result.FAST_FORWARD)
            {
                // JGit delete 成功常见为 FORCED；部分环境返回 NO_CHANGE 以外的失败态再拒绝
                if (repository.exactRef(refName) != null)
                {
                    throw new ApiException(MessageUtils.message("git.repo.tag.delete-failed"), HttpStatus.ERROR);
                }
            }
            return tip;
        }
    }

    public boolean tagExists(String diskPath, String tagName) throws IOException
    {
        if (StringUtils.isBlank(tagName))
        {
            return false;
        }
        try (Repository repository = openRepository(diskPath))
        {
            return repository.exactRef(Constants.R_TAGS + tagName.trim()) != null;
        }
    }

    public ObjectId resolveTargetCommit(
            String diskPath,
            String targetCommitId,
            String targetRef,
            String defaultBranch) throws IOException
    {
        return gitReleaseTagHelper.resolveTargetCommit(diskPath, targetCommitId, targetRef, defaultBranch);
    }

    public void validateTagName(String tagName)
    {
        gitReleaseTagHelper.validateTagName(tagName);
    }

    private static GitTagInfo toTagInfo(Repository repository, RevWalk walk, Ref ref, String shortName)
            throws IOException
    {
        GitTagInfo info = new GitTagInfo(shortName);
        info.setMessage("");
        Ref peeled = repository.peel(ref);
        ObjectId peeledId = peeled.getPeeledObjectId();
        boolean annotated = peeledId != null;
        info.setAnnotated(annotated);
        if (annotated)
        {
            try
            {
                RevObject obj = walk.parseAny(ref.getObjectId());
                if (obj instanceof RevTag)
                {
                    RevTag tag = (RevTag) obj;
                    String full = tag.getFullMessage();
                    info.setMessage(full != null ? full.replaceAll("\\s+$", "") : "");
                }
            }
            catch (IOException ignore)
            {
                // 消息读失败时仍返回结构字段
            }
        }
        ObjectId commitId = peeledId != null ? peeledId : ref.getObjectId();
        if (commitId != null)
        {
            try
            {
                RevCommit commit = walk.parseCommit(commitId);
                info.setCommitId(commit.getId().getName());
                info.setShortId(commit.getId().abbreviate(7).name());
                info.setSubject(commit.getShortMessage());
                PersonIdent author = commit.getAuthorIdent();
                if (author != null)
                {
                    info.setAuthorName(author.getName());
                    info.setAuthorEmail(author.getEmailAddress());
                    info.setCommitTime(author.getWhen());
                }
            }
            catch (IOException ignore)
            {
                info.setCommitId(commitId.getName());
                info.setShortId(commitId.abbreviate(7).name());
            }
        }
        return info;
    }

    private static ObjectId peelToCommit(Repository repository, Ref ref) throws IOException
    {
        Ref peeled = repository.peel(ref);
        ObjectId commitId = peeled.getPeeledObjectId();
        if (commitId != null)
        {
            return commitId;
        }
        return ref.getObjectId();
    }

    private Repository openRepository(String diskPath) throws IOException
    {
        if (StringUtils.isBlank(diskPath))
        {
            throw new ApiException(MessageUtils.message("git.repo.browse.disk-missing"), HttpStatus.NOT_FOUND);
        }
        File gitDir = new File(gitPathValidator.resolveStoredDiskPath(diskPath));
        if (!gitDir.exists() || !gitDir.isDirectory())
        {
            throw new ApiException(MessageUtils.message("git.repo.browse.disk-missing"), HttpStatus.NOT_FOUND);
        }
        return new FileRepositoryBuilder().setGitDir(gitDir).setBare().build();
    }
}
