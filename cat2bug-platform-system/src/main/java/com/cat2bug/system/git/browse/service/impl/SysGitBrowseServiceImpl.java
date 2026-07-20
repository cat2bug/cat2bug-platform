package com.cat2bug.system.git.browse.service.impl;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.git.browse.domain.GitBlobContent;
import com.cat2bug.system.git.browse.domain.GitBranchInfo;
import com.cat2bug.system.git.browse.domain.GitCommitInfo;
import com.cat2bug.system.git.browse.domain.GitTreeEntryInfo;
import com.cat2bug.system.git.browse.service.ISysGitBrowseService;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.service.git.GitPathValidator;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    @Autowired
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Autowired
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Autowired
    private GitPathValidator gitPathValidator;

    @Override
    public List<GitBranchInfo> listBranches(Long repositoryId)
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
            List<Ref> refs = repository.getRefDatabase().getRefsByPrefix(Constants.R_HEADS);
            for (Ref ref : refs)
            {
                String shortName = Repository.shortenRefName(ref.getName());
                result.add(new GitBranchInfo(shortName, shortName.equals(currentBranch)));
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
