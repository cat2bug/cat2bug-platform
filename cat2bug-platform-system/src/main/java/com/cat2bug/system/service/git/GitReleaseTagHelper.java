package com.cat2bug.system.service.git;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.StringUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 发行版相关 JGit 操作：解析 commit/ref、在 bare 仓确保/绑定 tag（不 fork 系统 git）。
 */
@Component
public class GitReleaseTagHelper
{
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("[^\\s~^:?*\\[\\\\]+");

    @Autowired
    private GitPathValidator gitPathValidator;

    /**
     * 解析目标 commit：优先 {@code targetCommitId}，其次 {@code targetRef}，最后 {@code defaultBranch}。
     */
    public ObjectId resolveTargetCommit(
            String diskPath,
            String targetCommitId,
            String targetRef,
            String defaultBranch) throws IOException
    {
        try (Repository repository = openRepository(diskPath))
        {
            if (StringUtils.isNotBlank(targetCommitId))
            {
                ObjectId id = repository.resolve(targetCommitId.trim());
                if (id == null)
                {
                    throw new ServiceException(MessageUtils.message("git.release.commit-not-found"), HttpStatus.BAD_REQUEST);
                }
                return id;
            }
            if (StringUtils.isNotBlank(targetRef))
            {
                ObjectId id = repository.resolve(targetRef.trim());
                if (id == null)
                {
                    throw new ServiceException(MessageUtils.message("git.release.ref-not-found"), HttpStatus.BAD_REQUEST);
                }
                return id;
            }
            if (StringUtils.isNotBlank(defaultBranch))
            {
                ObjectId id = repository.resolve(defaultBranch.trim());
                if (id == null)
                {
                    throw new ServiceException(MessageUtils.message("git.release.ref-not-found"), HttpStatus.BAD_REQUEST);
                }
                return id;
            }
            throw new ServiceException(MessageUtils.message("git.release.target-required"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * tag 不存在则在 {@code commitId} 创建轻量 tag；已存在则必须指向同一 commit，否则拒绝。
     *
     * @return 绑定后的 commit id（完整 40 位十六进制）
     */
    public String ensureTagAtCommit(String diskPath, String tagName, ObjectId commitId) throws IOException
    {
        validateTagName(tagName);
        try (Repository repository = openRepository(diskPath))
        {
            String refName = Constants.R_TAGS + tagName;
            Ref existing = repository.findRef(refName);
            if (existing == null)
            {
                RefUpdate update = repository.updateRef(refName);
                update.setNewObjectId(commitId);
                update.disableRefLog();
                RefUpdate.Result result = update.update();
                if (result != RefUpdate.Result.NEW && result != RefUpdate.Result.FORCED)
                {
                    throw new ServiceException(MessageUtils.message("git.release.tag-create-failed"), HttpStatus.ERROR);
                }
                return commitId.getName();
            }
            ObjectId tagTip = peelToCommit(repository, existing);
            if (tagTip == null || !tagTip.equals(commitId))
            {
                throw new ServiceException(MessageUtils.message("git.release.tag-commit-conflict"), HttpStatus.CONFLICT);
            }
            return commitId.getName();
        }
    }

    /**
     * 读取指定 commit 的短消息；找不到或读失败时返回 {@code null}。
     */
    public String readCommitShortMessage(String diskPath, String commitIdHex) throws IOException
    {
        if (StringUtils.isBlank(commitIdHex))
        {
            return null;
        }
        try (Repository repository = openRepository(diskPath))
        {
            ObjectId id = repository.resolve(commitIdHex.trim());
            if (id == null)
            {
                return null;
            }
            try (RevWalk walk = new RevWalk(repository))
            {
                RevCommit commit = walk.parseCommit(id);
                return commit != null ? commit.getShortMessage() : null;
            }
        }
    }

    public void validateTagName(String tagName)
    {
        if (StringUtils.isBlank(tagName))
        {
            throw new ServiceException(MessageUtils.message("git.release.tag-required"), HttpStatus.BAD_REQUEST);
        }
        String trimmed = tagName.trim();
        if (trimmed.startsWith(".") || trimmed.endsWith(".") || trimmed.contains(".."))
        {
            throw new ServiceException(MessageUtils.message("git.release.tag-invalid"), HttpStatus.BAD_REQUEST);
        }
        if (!TAG_NAME_PATTERN.matcher(trimmed).matches())
        {
            throw new ServiceException(MessageUtils.message("git.release.tag-invalid"), HttpStatus.BAD_REQUEST);
        }
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
            throw new ServiceException(MessageUtils.message("git.repo.browse.disk-missing"), HttpStatus.NOT_FOUND);
        }
        File gitDir = new File(gitPathValidator.resolveStoredDiskPath(diskPath));
        if (!gitDir.exists() || !gitDir.isDirectory())
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.disk-missing"), HttpStatus.NOT_FOUND);
        }
        return new FileRepositoryBuilder().setGitDir(gitDir).setBare().build();
    }
}
