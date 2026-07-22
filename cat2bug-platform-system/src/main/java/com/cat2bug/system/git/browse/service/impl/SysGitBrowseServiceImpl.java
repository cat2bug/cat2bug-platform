package com.cat2bug.system.git.browse.service.impl;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.file.FileUtils;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.git.browse.domain.GitBlobContent;
import com.cat2bug.system.git.browse.domain.GitBranchInfo;
import com.cat2bug.system.git.browse.domain.GitCommitChangeStats;
import com.cat2bug.system.git.browse.domain.GitCommitDetail;
import com.cat2bug.system.git.browse.domain.GitCommitFileChange;
import com.cat2bug.system.git.browse.domain.GitCommitInfo;
import com.cat2bug.system.git.browse.domain.GitCommitParentRef;
import com.cat2bug.system.git.browse.domain.GitContributorInfo;
import com.cat2bug.system.git.browse.domain.GitTreeEntryInfo;
import com.cat2bug.system.git.browse.service.ISysGitBrowseService;
import com.cat2bug.system.git.browse.support.GitContributorSupport;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.service.git.GitPathValidator;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.archive.ArchiveFormats;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 项目 Git 代码仓库浏览服务实现：基于 JGit 直接读取 bare 仓磁盘对象，不 fork 系统 {@code git} 进程。
 *
 * <p>仓库元数据（projectId / disk_path / defaultBranch）由并行落地的 {@link SysGitRepositoryMapper}
 * （任务 2.x CRUD）提供，本类不重复维护领域模型。权限判定复用 Smart HTTP（任务 3.x）落地的
 * {@link GitRepositoryAccessService#canPull(Long, Long)}，与 push/force-push 校验共用同一份角色矩阵，
 * 保证「系统管理员若无目标项目角色同样被拒绝」在 browse / smart-http 两处结论一致（无绕过）。</p>
 */
@Service
public class SysGitBrowseServiceImpl implements ISysGitBrowseService
{
    /** 文件内容预览上限（字节），超出则截断并标记 truncated */
    private static final int MAX_BLOB_PREVIEW_BYTES = 2 * 1024 * 1024;

    /** 二进制探测扫描的字节数（沿用 git 的 NUL 字节启发式） */
    private static final int BINARY_DETECT_SCAN_BYTES = 8000;

    private static final int DEFAULT_COMMIT_LIMIT = 50;

    private static final int MAX_COMMIT_LIMIT = 200;

    /**
     * 贡献者聚合：自 ref tip 起最多扫描的提交数（OpenSpec design §4，可后续提高到 2000）。
     */
    static final int CONTRIBUTOR_SCAN_COMMIT_LIMIT = 1000;

    /** 贡献者 API 默认返回条数 */
    static final int DEFAULT_CONTRIBUTOR_RETURN_LIMIT = 100;

    /** 贡献者 API 最大返回条数 */
    static final int MAX_CONTRIBUTOR_RETURN_LIMIT = 100;

    /** 单文件 unified patch 上限（字节） */
    static final int MAX_COMMIT_PATCH_PER_FILE_BYTES = 256 * 1024;

    /** 单次 commit detail 响应累计 patch 上限（字节） */
    static final int MAX_COMMIT_PATCH_CUMULATIVE_BYTES = (3 * 512 * 1024);

    /** 附带完整 patch 的文件数量软上限 */
    static final int MAX_COMMIT_PATCH_FILE_COUNT = 500;

    private static final AtomicBoolean ARCHIVE_FORMATS_REGISTERED = new AtomicBoolean(false);

    @Autowired
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Autowired
    private SysUserProjectMapper sysUserProjectMapper;

    @Autowired
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Autowired
    private GitPathValidator gitPathValidator;

    @Override
    public List<GitBranchInfo> listBranches(Long repositoryId)
    {
        return listBranches(repositoryId, null);
    }

    @Override
    public List<GitBranchInfo> listBranches(Long repositoryId, String nameFilter)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getProjectId());
        List<GitBranchInfo> result = new ArrayList<>();
        try (Repository repository = openRepository(repo.getDiskPath()))
        {
            // 默认分支以元数据（sys_git_repository.default_branch）为权威来源，与
            // listTree/readBlob/listCommits 在未显式指定 ref 时的解析口径保持一致；
            // 裸仓库的物理 HEAD 符号引用未必随 push 同步更新，不作为判定依据。
            String currentBranch = StringUtils.isNotBlank(repo.getDefaultBranch())
                    ? repo.getDefaultBranch() : resolveHeadBranch(repository);
            ObjectId defaultTip = StringUtils.isNotBlank(currentBranch)
                    ? repository.resolve(Constants.R_HEADS + currentBranch) : null;
            List<Ref> refs = repository.getRefDatabase().getRefsByPrefix(Constants.R_HEADS);
            for (Ref ref : refs)
            {
                String shortName = Repository.shortenRefName(ref.getName());
                if (StringUtils.isNotBlank(nameFilter)
                        && !shortName.toLowerCase(Locale.ROOT).contains(nameFilter.trim().toLowerCase(Locale.ROOT)))
                {
                    continue;
                }
                GitBranchInfo branch = new GitBranchInfo(shortName, shortName.equals(currentBranch));
                enrichBranchTip(repository, branch, ref.getObjectId(), defaultTip);
                result.add(branch);
            }
            result.sort(Comparator.comparing(GitBranchInfo::getName));
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.read-failed"), HttpStatus.ERROR);
        }
        return result;
    }

    @Override
    public GitBranchInfo createBranch(Long repositoryId, String name, String fromRef)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertManage(repo.getProjectId());
        String branchName = normalizeBranchName(name);
        try (Repository repository = openRepository(repo.getDiskPath()))
        {
            String source = StringUtils.isNotBlank(fromRef) ? fromRef.trim() : repo.getDefaultBranch();
            boolean usedMetaDefault = StringUtils.isBlank(fromRef);
            if (StringUtils.isBlank(source))
            {
                throw new ServiceException(MessageUtils.message("git.repo.branch.source-required"), HttpStatus.BAD_REQUEST);
            }
            ObjectId sourceId = repository.resolve(source);
            // Metadata default / symbolic HEAD may still name master while only main exists.
            if (sourceId == null && usedMetaDefault)
            {
                sourceId = resolveFallbackBranchTip(repository);
            }
            if (sourceId == null)
            {
                throw new ServiceException(MessageUtils.message("git.repo.branch.source-not-found"), HttpStatus.BAD_REQUEST);
            }
            String refName = Constants.R_HEADS + branchName;
            if (repository.exactRef(refName) != null)
            {
                throw new ServiceException(MessageUtils.message("git.repo.branch.already-exists"), HttpStatus.CONFLICT);
            }
            RefUpdate update = repository.updateRef(refName);
            update.setNewObjectId(sourceId);
            update.disableRefLog();
            RefUpdate.Result updateResult = update.update();
            if (updateResult != RefUpdate.Result.NEW)
            {
                throw new ServiceException(MessageUtils.message("git.repo.branch.create-failed"), HttpStatus.ERROR);
            }
            GitBranchInfo branch = new GitBranchInfo(branchName, branchName.equals(repo.getDefaultBranch()));
            enrichBranchTip(repository, branch, sourceId,
                    StringUtils.isNotBlank(repo.getDefaultBranch())
                            ? repository.resolve(Constants.R_HEADS + repo.getDefaultBranch()) : null);
            return branch;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.branch.create-failed"), HttpStatus.ERROR);
        }
    }

    @Override
    public void deleteBranch(Long repositoryId, String name)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertManage(repo.getProjectId());
        String branchName = normalizeBranchName(name);
        if (branchName.equals(repo.getDefaultBranch()))
        {
            throw new ServiceException(MessageUtils.message("git.repo.branch.delete-default"), HttpStatus.BAD_REQUEST);
        }
        try (Repository repository = openRepository(repo.getDiskPath()))
        {
            String refName = Constants.R_HEADS + branchName;
            if (repository.exactRef(refName) == null)
            {
                throw new ServiceException(MessageUtils.message("git.repo.branch.not-found"), HttpStatus.NOT_FOUND);
            }
            RefUpdate update = repository.updateRef(refName);
            update.setForceUpdate(true);
            update.disableRefLog();
            RefUpdate.Result updateResult = update.delete();
            if (updateResult != RefUpdate.Result.FORCED)
            {
                throw new ServiceException(MessageUtils.message("git.repo.branch.delete-failed"), HttpStatus.ERROR);
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.branch.delete-failed"), HttpStatus.ERROR);
        }
    }

    @Override
    public void setDefaultBranch(Long repositoryId, String name)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertManage(repo.getProjectId());
        String branchName = normalizeBranchName(name);
        try (Repository repository = openRepository(repo.getDiskPath()))
        {
            if (repository.exactRef(Constants.R_HEADS + branchName) == null)
            {
                throw new ServiceException(MessageUtils.message("git.repo.branch.not-found"), HttpStatus.NOT_FOUND);
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.read-failed"), HttpStatus.ERROR);
        }
        repo.setDefaultBranch(branchName);
        sysGitRepositoryMapper.updateSysGitRepository(repo);
    }

    @Override
    public List<GitTreeEntryInfo> listTree(Long repositoryId, String ref, String path)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getProjectId());
        try (Repository repository = openRepository(repo.getDiskPath()))
        {
            ObjectId commitId = resolveCommitish(repository, ref, repo.getDefaultBranch());
            if (commitId == null)
            {
                return new ArrayList<>();
            }
            try (RevWalk revWalk = new RevWalk(repository))
            {
                RevCommit commit = revWalk.parseCommit(commitId);
                List<GitTreeEntryInfo> entries = listTreeEntries(repository, commit.getTree(), normalizePath(path));
                enrichLastCommits(repository, commit, entries);
                return entries;
            }
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.read-failed"), HttpStatus.ERROR);
        }
    }

    @Override
    public GitBlobContent readBlob(Long repositoryId, String ref, String path)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getProjectId());
        String normalizedPath = normalizePath(path);
        if (StringUtils.isBlank(normalizedPath))
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.path-required"), HttpStatus.BAD_REQUEST);
        }
        try (Repository repository = openRepository(repo.getDiskPath()))
        {
            ObjectId commitId = resolveCommitish(repository, ref, repo.getDefaultBranch());
            if (commitId == null)
            {
                throw new ServiceException(MessageUtils.message("git.repo.browse.empty-repository"), HttpStatus.NOT_FOUND);
            }
            try (RevWalk revWalk = new RevWalk(repository))
            {
                RevCommit commit = revWalk.parseCommit(commitId);
                RevTree tree = commit.getTree();
                try (TreeWalk treeWalk = TreeWalk.forPath(repository, normalizedPath, tree))
                {
                    if (treeWalk == null || treeWalk.isSubtree())
                    {
                        throw new ServiceException(MessageUtils.message("git.repo.browse.file-not-found"), HttpStatus.NOT_FOUND);
                    }
                    ObjectId blobId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(blobId);
                    return readBlobContent(normalizedPath, loader);
                }
            }
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.read-failed"), HttpStatus.ERROR);
        }
    }

    @Override
    public List<GitCommitInfo> listCommits(Long repositoryId, String ref, Integer limit)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getProjectId());
        int max = (limit != null && limit > 0) ? Math.min(limit, MAX_COMMIT_LIMIT) : DEFAULT_COMMIT_LIMIT;
        List<GitCommitInfo> result = new ArrayList<>();
        try (Repository repository = openRepository(repo.getDiskPath()))
        {
            ObjectId startId = resolveCommitish(repository, ref, repo.getDefaultBranch());
            if (startId == null)
            {
                return result;
            }
            try (RevWalk revWalk = new RevWalk(repository))
            {
                revWalk.markStart(revWalk.parseCommit(startId));
                int count = 0;
                for (RevCommit commit : revWalk)
                {
                    if (count >= max)
                    {
                        break;
                    }
                    result.add(toCommitInfo(commit));
                    count++;
                }
            }
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.read-failed"), HttpStatus.ERROR);
        }
        return result;
    }

    @Override
    public GitCommitDetail getCommitDetail(Long repositoryId, String commitId)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getProjectId());
        if (StringUtils.isBlank(commitId))
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.commit-not-found"), HttpStatus.NOT_FOUND);
        }
        try (Repository repository = openRepository(repo.getDiskPath()))
        {
            ObjectId objectId = repository.resolve(commitId.trim());
            if (objectId == null)
            {
                throw new ServiceException(MessageUtils.message("git.repo.browse.commit-not-found"),
                        HttpStatus.NOT_FOUND);
            }
            try (RevWalk revWalk = new RevWalk(repository))
            {
                RevCommit commit = revWalk.parseCommit(objectId);
                return toCommitDetail(repository, revWalk, commit);
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.read-failed"), HttpStatus.ERROR);
        }
    }

    @Override
    public List<GitContributorInfo> listContributors(Long repositoryId, String ref, Integer limit)
    {
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getProjectId());
        int returnMax = (limit != null && limit > 0)
                ? Math.min(limit, MAX_CONTRIBUTOR_RETURN_LIMIT) : DEFAULT_CONTRIBUTOR_RETURN_LIMIT;
        List<SysUser> projectMembers = sysUserProjectMapper.selectSysUserListByProjectId(
                repo.getProjectId(), new SysUser());
        LoginUser loginUser = safeLoginUser();

        Map<String, MutableContributor> buckets = new LinkedHashMap<>();
        try (Repository repository = openRepository(repo.getDiskPath()))
        {
            ObjectId startId = resolveCommitish(repository, ref, repo.getDefaultBranch());
            if (startId == null)
            {
                return new ArrayList<>();
            }
            try (RevWalk revWalk = new RevWalk(repository))
            {
                revWalk.markStart(revWalk.parseCommit(startId));
                int scanned = 0;
                for (RevCommit commit : revWalk)
                {
                    if (scanned >= CONTRIBUTOR_SCAN_COMMIT_LIMIT)
                    {
                        break;
                    }
                    PersonIdent author = commit.getAuthorIdent();
                    String authorName = author != null ? author.getName() : null;
                    String authorEmail = author != null ? author.getEmailAddress() : null;
                    String key = GitContributorSupport.aggregationKey(authorName, authorEmail);
                    MutableContributor bucket = buckets.computeIfAbsent(key, k -> new MutableContributor());
                    bucket.record(authorName, authorEmail);
                    scanned++;
                }
            }
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.read-failed"), HttpStatus.ERROR);
        }

        List<GitContributorInfo> aggregated = new ArrayList<>();
        for (MutableContributor bucket : buckets.values())
        {
            GitContributorInfo info = bucket.toInfo(projectMembers, loginUser);
            aggregated.add(info);
        }
        aggregated.sort(Comparator
                .comparingInt(GitContributorInfo::getCommitCount).reversed()
                .thenComparing(GitContributorInfo::getDisplayName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        if (aggregated.size() <= returnMax)
        {
            return aggregated;
        }
        return new ArrayList<>(aggregated.subList(0, returnMax));
    }

    @Override
    public void writeArchive(Long repositoryId, String ref, String format, HttpServletResponse response)
            throws IOException
    {
        ensureArchiveFormatsRegistered();
        SysGitRepository repo = requireRepo(repositoryId);
        assertPull(repo.getProjectId());

        // JGit ArchiveFormats 注册名为 tgz / zip；对外 API 仍接受 tar.gz
        String jgitFormat = normalizeArchiveFormat(format);
        String extension = "tgz".equals(jgitFormat) ? "tar.gz" : "zip";
        String contentType = "tgz".equals(jgitFormat)
                ? "application/gzip"
                : "application/zip";

        try (Repository repository = openRepository(repo.getDiskPath()))
        {
            ObjectId commitId = resolveCommitish(repository, ref, repo.getDefaultBranch());
            if (commitId == null)
            {
                throw new ServiceException(MessageUtils.message("git.repo.browse.empty-repository"),
                        HttpStatus.NOT_FOUND);
            }
            String label = archiveLabel(ref, commitId);
            String slug = StringUtils.isNotBlank(repo.getSlug()) ? repo.getSlug().trim() : "source";
            String fileName = slug + "-" + label + "." + extension;
            String prefix = slug + "-" + label + "/";

            response.setContentType(contentType);
            FileUtils.setAttachmentResponseHeader(response, fileName);
            try (OutputStream out = response.getOutputStream();
                 Git git = Git.wrap(repository))
            {
                git.archive()
                        .setTree(commitId)
                        .setFormat(jgitFormat)
                        .setPrefix(prefix)
                        .setOutputStream(out)
                        .call();
                out.flush();
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.archive-failed"), HttpStatus.ERROR);
        }
    }

    private static void ensureArchiveFormatsRegistered()
    {
        if (ARCHIVE_FORMATS_REGISTERED.compareAndSet(false, true))
        {
            ArchiveFormats.registerAll();
        }
    }

    /**
     * 将 API 的 format 参数映射为 JGit {@link ArchiveFormats} 注册名（zip / tgz）。
     */
    private static String normalizeArchiveFormat(String format)
    {
        String f = format == null ? "" : format.trim().toLowerCase(Locale.ROOT);
        if ("zip".equals(f))
        {
            return "zip";
        }
        if ("tar.gz".equals(f) || "tgz".equals(f) || "targz".equals(f))
        {
            return "tgz";
        }
        throw new ServiceException(MessageUtils.message("git.repo.browse.archive-format-invalid"),
                HttpStatus.BAD_REQUEST);
    }

    private static String archiveLabel(String ref, ObjectId commitId)
    {
        if (StringUtils.isNotBlank(ref))
        {
            String trimmed = ref.trim();
            int slash = trimmed.lastIndexOf('/');
            if (slash >= 0 && slash < trimmed.length() - 1)
            {
                trimmed = trimmed.substring(slash + 1);
            }
            if (trimmed.matches("[0-9a-fA-F]{7,40}"))
            {
                return trimmed.length() > 7 ? trimmed.substring(0, 7).toLowerCase(Locale.ROOT) : trimmed.toLowerCase(Locale.ROOT);
            }
            return trimmed.replaceAll("[\\\\/:*?\"<>|\\s]+", "-");
        }
        return commitId.getName().substring(0, 7);
    }

    private static LoginUser safeLoginUser()
    {
        try
        {
            return SecurityUtils.getLoginUser();
        }
        catch (Exception ignore)
        {
            return null;
        }
    }

    private static final class MutableContributor
    {
        private int commitCount;

        private String displayName;

        private String email;

        void record(String authorName, String authorEmail)
        {
            commitCount++;
            if (commitCount == 1)
            {
                if (StringUtils.isNotBlank(authorName))
                {
                    displayName = authorName.trim();
                }
                if (StringUtils.isNotBlank(authorEmail))
                {
                    email = authorEmail.trim();
                }
            }
        }

        GitContributorInfo toInfo(List<SysUser> projectMembers, LoginUser loginUser)
        {
            GitContributorInfo info = new GitContributorInfo();
            info.setCommitCount(commitCount);
            info.setDisplayName(StringUtils.isNotBlank(displayName) ? displayName : "?");
            info.setEmail(StringUtils.isNotBlank(email) ? email : null);

            SysUser matched = GitContributorSupport.matchProjectMember(projectMembers, displayName, email);
            if (matched == null && loginUser != null && loginUser.getUser() != null)
            {
                matched = GitContributorSupport.matchCurrentUser(loginUser.getUser(), displayName);
            }
            if (matched != null)
            {
                info.setUserId(matched.getUserId());
                info.setAvatar(matched.getAvatar());
            }
            return info;
        }
    }

    // ---------------------------------------------------------------------
    // 权限 / 装配
    // ---------------------------------------------------------------------

    /** 无 pull 权限（含未持有目标项目角色的系统管理员）直接拒绝，不做任何绕过 */
    private void assertPull(Long projectId)
    {
        if (!gitRepositoryAccessService.canPull(SecurityUtils.getUserId(), projectId))
        {
            throw new ServiceException(MessageUtils.message("no-permission"), HttpStatus.FORBIDDEN);
        }
    }

    private void assertManage(Long projectId)
    {
        if (!gitRepositoryAccessService.canManage(SecurityUtils.getUserId(), projectId))
        {
            throw new ServiceException(MessageUtils.message("no-permission"), HttpStatus.FORBIDDEN);
        }
    }

    private SysGitRepository requireRepo(Long repositoryId)
    {
        SysGitRepository repo = repositoryId == null
                ? null : sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repo == null)
        {
            throw new ServiceException(MessageUtils.message("git.repo.not-found"), HttpStatus.NOT_FOUND);
        }
        return repo;
    }

    // ---------------------------------------------------------------------
    // JGit
    // ---------------------------------------------------------------------

    private static String normalizeBranchName(String name)
    {
        if (StringUtils.isBlank(name))
        {
            throw new ServiceException(MessageUtils.message("git.repo.branch.name-required"), HttpStatus.BAD_REQUEST);
        }
        String branchName = name.trim();
        if (branchName.length() > 100)
        {
            throw new ServiceException(MessageUtils.message("git.repo.branch.name-too-long"), HttpStatus.BAD_REQUEST);
        }
        if (branchName.startsWith(Constants.R_REFS) || !Repository.isValidRefName(Constants.R_HEADS + branchName))
        {
            throw new ServiceException(MessageUtils.message("git.repo.branch.name-invalid"), HttpStatus.BAD_REQUEST);
        }
        return branchName;
    }

    private static void enrichBranchTip(
            Repository repository, GitBranchInfo branch, ObjectId tipId, ObjectId defaultTip) throws IOException
    {
        if (tipId == null)
        {
            return;
        }
        try (RevWalk walk = new RevWalk(repository))
        {
            RevCommit tip = walk.parseCommit(tipId);
            branch.setShortId(tip.getId().abbreviate(7).name());
            PersonIdent author = tip.getAuthorIdent();
            if (author != null)
            {
                branch.setAuthorName(author.getName());
                branch.setAuthorEmail(author.getEmailAddress());
                branch.setCommitTime(author.getWhen());
            }
        }
        if (defaultTip != null)
        {
            branch.setAhead(countCommitsReachableFromFirstButNotSecond(repository, tipId, defaultTip));
            branch.setBehind(countCommitsReachableFromFirstButNotSecond(repository, defaultTip, tipId));
        }
    }

    private static int countCommitsReachableFromFirstButNotSecond(
            Repository repository, ObjectId first, ObjectId second) throws IOException
    {
        try (RevWalk walk = new RevWalk(repository))
        {
            walk.markStart(walk.parseCommit(first));
            walk.markUninteresting(walk.parseCommit(second));
            int count = 0;
            for (RevCommit ignored : walk)
            {
                count++;
            }
            return count;
        }
    }

    private static String resolveHeadBranch(Repository repository)
    {
        try
        {
            return repository.getBranch();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /**
     * When create omits fromRef and metadata default is stale, prefer HEAD then any local branch tip.
     */
    private static ObjectId resolveFallbackBranchTip(Repository repository) throws IOException
    {
        ObjectId head = repository.resolve(Constants.HEAD);
        if (head != null)
        {
            return head;
        }
        List<Ref> refs = repository.getRefDatabase().getRefsByPrefix(Constants.R_HEADS);
        for (Ref ref : refs)
        {
            if (ref != null && ref.getObjectId() != null)
            {
                return ref.getObjectId();
            }
        }
        return null;
    }

    private Repository openRepository(String diskPath)
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
        try
        {
            return new FileRepositoryBuilder().setGitDir(gitDir).setBare().build();
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.browse.read-failed"), HttpStatus.ERROR);
        }
    }

    private static ObjectId resolveCommitish(Repository repo, String ref, String defaultBranch) throws IOException
    {
        String target = StringUtils.isNotBlank(ref) ? ref.trim() : defaultBranch;
        if (StringUtils.isBlank(target))
        {
            target = Constants.HEAD;
        }
        ObjectId id = repo.resolve(target);
        if (id == null && !Constants.HEAD.equals(target))
        {
            id = repo.resolve(Constants.HEAD);
        }
        return id;
    }

    private List<GitTreeEntryInfo> listTreeEntries(Repository repo, RevTree tree, String normalizedPath) throws IOException
    {
        TreeWalk treeWalk;
        if (StringUtils.isBlank(normalizedPath))
        {
            treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(false);
        }
        else
        {
            treeWalk = TreeWalk.forPath(repo, normalizedPath, tree);
            if (treeWalk == null)
            {
                throw new ServiceException(MessageUtils.message("git.repo.browse.path-not-found"), HttpStatus.NOT_FOUND);
            }
            if (!treeWalk.isSubtree())
            {
                treeWalk.close();
                throw new ServiceException(MessageUtils.message("git.repo.browse.not-a-directory"), HttpStatus.BAD_REQUEST);
            }
            treeWalk.enterSubtree();
            treeWalk.setRecursive(false);
        }
        try
        {
            List<GitTreeEntryInfo> result = new ArrayList<>();
            while (treeWalk.next())
            {
                result.add(toTreeEntry(repo, treeWalk, normalizedPath));
            }
            result.sort(Comparator
                    .comparing((GitTreeEntryInfo e) -> GitTreeEntryInfo.TYPE_TREE.equals(e.getType()) ? 0 : 1)
                    .thenComparing(GitTreeEntryInfo::getName));
            return result;
        }
        finally
        {
            treeWalk.close();
        }
    }

    private static GitTreeEntryInfo toTreeEntry(Repository repo, TreeWalk treeWalk, String parentPath) throws IOException
    {
        GitTreeEntryInfo entry = new GitTreeEntryInfo();
        String name = treeWalk.getNameString();
        entry.setName(name);
        entry.setPath(StringUtils.isBlank(parentPath) ? name : parentPath + "/" + name);
        entry.setObjectId(treeWalk.getObjectId(0).getName());
        boolean isTree = (treeWalk.getRawMode(0) & FileMode.TYPE_MASK) == FileMode.TYPE_TREE;
        entry.setType(isTree ? GitTreeEntryInfo.TYPE_TREE : GitTreeEntryInfo.TYPE_BLOB);
        if (!isTree)
        {
            try
            {
                entry.setSize(repo.open(treeWalk.getObjectId(0)).getSize());
            }
            catch (IOException ignore)
            {
                // 忽略：大小仅用于展示，读取失败不影响目录列举
            }
        }
        return entry;
    }

    /**
     * 为目录条目填充「该路径最近一次变更」信息，供详情页文件列表展示。
     * <p>使用 PathFilter + ANY_DIFF 查找最近改动该路径的提交；仓库仅有根提交（无 parent）时
     * ANY_DIFF 常匹配失败，此时若路径存在于当前提交树中，则回退为当前提交本身。</p>
     */
    private static void enrichLastCommits(Repository repo, RevCommit start, List<GitTreeEntryInfo> entries)
    {
        if (start == null || entries == null || entries.isEmpty())
        {
            return;
        }
        ObjectId startId = start.getId();
        for (GitTreeEntryInfo entry : entries)
        {
            if (entry == null || StringUtils.isBlank(entry.getPath()))
            {
                continue;
            }
            try
            {
                RevCommit commit = findLatestCommitForPath(repo, startId, entry.getPath());
                if (commit == null)
                {
                    continue;
                }
                entry.setLastCommitMessage(commit.getShortMessage());
                entry.setLastCommitShortId(commit.getId().abbreviate(7).name());
                PersonIdent author = commit.getAuthorIdent();
                if (author != null)
                {
                    entry.setLastCommitAuthor(author.getName());
                    entry.setLastCommitTime(author.getWhen());
                }
            }
            catch (IOException ignore)
            {
                // 最近提交仅用于展示
            }
        }
    }

    private static RevCommit findLatestCommitForPath(Repository repo, ObjectId startId, String path)
            throws IOException
    {
        try (RevWalk walk = new RevWalk(repo))
        {
            RevCommit start = walk.parseCommit(startId);
            walk.markStart(start);
            walk.setTreeFilter(AndTreeFilter.create(PathFilter.create(path), TreeFilter.ANY_DIFF));
            RevCommit found = walk.next();
            if (found != null)
            {
                return found;
            }
            // 根提交或 ANY_DIFF 未命中：路径仍在该提交树中则视为该提交引入
            try (TreeWalk tw = TreeWalk.forPath(repo, path, start.getTree()))
            {
                if (tw != null)
                {
                    return start;
                }
            }
            return null;
        }
    }

    private static GitBlobContent readBlobContent(String path, ObjectLoader loader) throws IOException
    {
        long size = loader.getSize();
        byte[] preview;
        try (InputStream in = loader.openStream())
        {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int total = 0;
            int read;
            while (total < MAX_BLOB_PREVIEW_BYTES && (read = in.read(chunk)) != -1)
            {
                int toWrite = Math.min(read, MAX_BLOB_PREVIEW_BYTES - total);
                buffer.write(chunk, 0, toWrite);
                total += toWrite;
                if (toWrite < read)
                {
                    break;
                }
            }
            preview = buffer.toByteArray();
        }

        GitBlobContent result = new GitBlobContent();
        result.setPath(path);
        result.setSize(size);
        result.setTruncated(size > preview.length);
        boolean binary = isLikelyBinary(preview);
        result.setBinary(binary);
        if (!binary)
        {
            result.setContent(new String(preview, StandardCharsets.UTF_8));
        }
        return result;
    }

    private static boolean isLikelyBinary(byte[] bytes)
    {
        int limit = Math.min(bytes.length, BINARY_DETECT_SCAN_BYTES);
        for (int i = 0; i < limit; i++)
        {
            if (bytes[i] == 0)
            {
                return true;
            }
        }
        return false;
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

    private GitCommitDetail toCommitDetail(Repository repository, RevWalk revWalk, RevCommit commit)
            throws IOException
    {
        GitCommitDetail detail = new GitCommitDetail();
        detail.setId(commit.getId().getName());
        detail.setShortId(commit.getId().abbreviate(7).name());
        detail.setFullMessage(commit.getFullMessage());

        PersonIdent author = commit.getAuthorIdent();
        if (author != null)
        {
            detail.setAuthorName(author.getName());
            detail.setAuthorEmail(author.getEmailAddress());
            detail.setCommitTime(author.getWhen());
        }
        PersonIdent committer = commit.getCommitterIdent();
        if (committer != null)
        {
            detail.setCommitterName(committer.getName());
            detail.setCommitterEmail(committer.getEmailAddress());
        }

        for (int i = 0; i < commit.getParentCount(); i++)
        {
            RevCommit parent = revWalk.parseCommit(commit.getParent(i).getId());
            GitCommitParentRef parentRef = new GitCommitParentRef();
            parentRef.setId(parent.getId().getName());
            parentRef.setShortId(parent.getId().abbreviate(7).name());
            detail.getParents().add(parentRef);
        }

        List<GitCommitFileChange> fileChanges = buildCommitFileChanges(repository, revWalk, commit);
        detail.setFiles(fileChanges);

        GitCommitChangeStats stats = new GitCommitChangeStats();
        stats.setFiles(fileChanges.size());
        int additions = 0;
        int deletions = 0;
        for (GitCommitFileChange fileChange : fileChanges)
        {
            additions += fileChange.getAdditions();
            deletions += fileChange.getDeletions();
        }
        stats.setAdditions(additions);
        stats.setDeletions(deletions);
        detail.setStats(stats);
        return detail;
    }

    private List<GitCommitFileChange> buildCommitFileChanges(Repository repository, RevWalk revWalk, RevCommit commit)
            throws IOException
    {
        List<DiffEntry> diffEntries = scanCommitDiff(repository, revWalk, commit);
        List<GitCommitFileChange> result = new ArrayList<>();
        int cumulativePatchBytes = 0;
        int patchesAttached = 0;

        try (DiffFormatter headerFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE))
        {
            headerFormatter.setRepository(repository);
            headerFormatter.setDetectRenames(true);

            for (DiffEntry entry : diffEntries)
            {
                GitCommitFileChange fileChange = new GitCommitFileChange();
                fileChange.setChangeType(mapDiffChangeType(entry.getChangeType()));
                applyDiffPaths(entry, fileChange);

                FileHeader header = headerFormatter.toFileHeader(entry);
                applyLineStats(header, fileChange);

                if (isBinaryFileHeader(header))
                {
                    fileChange.setBinary(true);
                    result.add(fileChange);
                    continue;
                }

                if (patchesAttached >= MAX_COMMIT_PATCH_FILE_COUNT)
                {
                    fileChange.setTruncated(true);
                    result.add(fileChange);
                    continue;
                }

                ByteArrayOutputStream patchOut = new ByteArrayOutputStream();
                try (DiffFormatter patchFormatter = new DiffFormatter(patchOut))
                {
                    patchFormatter.setRepository(repository);
                    patchFormatter.setDetectRenames(true);
                    patchFormatter.format(entry);
                }
                byte[] patchBytes = patchOut.toByteArray();
                if (!isValidUtf8(patchBytes))
                {
                    fileChange.setBinary(true);
                }
                else if (patchBytes.length > MAX_COMMIT_PATCH_PER_FILE_BYTES
                        || cumulativePatchBytes + patchBytes.length > MAX_COMMIT_PATCH_CUMULATIVE_BYTES)
                {
                    fileChange.setTruncated(true);
                }
                else
                {
                    fileChange.setPatch(new String(patchBytes, StandardCharsets.UTF_8));
                    cumulativePatchBytes += patchBytes.length;
                    patchesAttached++;
                }
                result.add(fileChange);
            }
        }
        return result;
    }

    private static List<DiffEntry> scanCommitDiff(Repository repository, RevWalk revWalk, RevCommit commit)
            throws IOException
    {
        try (ObjectReader reader = repository.newObjectReader())
        {
            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            CanonicalTreeParser newTree = new CanonicalTreeParser();
            if (commit.getParentCount() > 0)
            {
                RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());
                oldTree.reset(reader, parent.getTree());
            }
            else
            {
                oldTree.reset();
            }
            newTree.reset(reader, commit.getTree());
            try (DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE))
            {
                formatter.setRepository(repository);
                formatter.setDetectRenames(true);
                return formatter.scan(oldTree, newTree);
            }
        }
    }

    private static void applyLineStats(FileHeader header, GitCommitFileChange fileChange)
    {
        EditList edits = header.toEditList();
        if (edits == null)
        {
            return;
        }
        int additions = 0;
        int deletions = 0;
        for (Edit edit : edits)
        {
            deletions += edit.getLengthA();
            additions += edit.getLengthB();
        }
        fileChange.setAdditions(additions);
        fileChange.setDeletions(deletions);
    }

    private static boolean isBinaryFileHeader(FileHeader header)
    {
        FileHeader.PatchType patchType = header.getPatchType();
        return patchType == FileHeader.PatchType.BINARY
                || patchType == FileHeader.PatchType.GIT_BINARY;
    }

    private static String mapDiffChangeType(DiffEntry.ChangeType changeType)
    {
        return switch (changeType)
        {
            case ADD -> "ADD";
            case MODIFY -> "MODIFY";
            case DELETE -> "DELETE";
            case RENAME -> "RENAME";
            case COPY -> "COPY";
        };
    }

    private static void applyDiffPaths(DiffEntry entry, GitCommitFileChange fileChange)
    {
        DiffEntry.ChangeType type = entry.getChangeType();
        if (type == DiffEntry.ChangeType.DELETE)
        {
            fileChange.setPath(normalizeDiffPath(entry.getOldPath()));
        }
        else
        {
            fileChange.setPath(normalizeDiffPath(entry.getNewPath()));
        }
        if (type == DiffEntry.ChangeType.RENAME || type == DiffEntry.ChangeType.COPY)
        {
            fileChange.setOldPath(normalizeDiffPath(entry.getOldPath()));
        }
    }

    private static String normalizeDiffPath(String path)
    {
        if (path == null || DiffEntry.DEV_NULL.equals(path))
        {
            return null;
        }
        if (path.startsWith("/"))
        {
            return path.substring(1);
        }
        return path;
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

    private static String normalizePath(String path)
    {
        if (StringUtils.isBlank(path))
        {
            return "";
        }
        String p = path.trim();
        while (p.startsWith("/"))
        {
            p = p.substring(1);
        }
        while (p.endsWith("/"))
        {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }
}
