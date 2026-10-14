package com.cat2bug.system.service.impl;
import jakarta.inject.Inject;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.DateUtils;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.framework.security.SecurityUtils;
import com.cat2bug.common.util.StringUtils;
import jakarta.transaction.Transactional;

import com.cat2bug.common.domain.SysGitRepository;
import com.cat2bug.common.domain.SysGitRepositoryMember;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysUserGitRepository;
import com.cat2bug.common.domain.git.SysGitRemoteListRequest;
import com.cat2bug.common.domain.git.SysGitRemoteRepository;
import com.cat2bug.common.domain.git.SysGitRepositoryImportRequest;
import com.cat2bug.system.git.browse.domain.GitProjectOverview;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMemberMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.mapper.SysUserGitRepositoryMapper;
import com.cat2bug.system.service.ISysGitRepositoryMemberService;
import com.cat2bug.system.service.ISysGitRepositoryService;
import com.cat2bug.system.service.git.GitImportUrlValidator;
import com.cat2bug.system.service.git.GitPathValidator;
import com.cat2bug.system.service.git.GitRemoteRepositoryClient;
import com.cat2bug.system.service.git.GitRepoLanguageScanner;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.service.git.JGitBareRepositoryHelper;

/**
 * 项目代码仓库Service业务层处理
 *
 * 权限模型（design.md D3）：
 * <ul>
 *     <li>manage（增/删/改 slug/改路径）：项目角色 {@code project.admin}(role_id=6) 或 {@code project.create-by}(role_id=11)</li>
 *     <li>pull/push（含列表/详情读取）：manage 并集 {@code project.develop}(role_id=7) / {@code project.tester}(role_id=8)</li>
 * </ul>
 * 权限判定始终以「目标仓库所属 projectId」为范围显式查询 {@link SysRoleMapper#selectRolePermissionByUserId}，
 * 且调用时不传 teamId（传 null），因此团队角色不参与判定；系统管理员（userId=1，全局 {@code *:*:*}）与团队管理员
 * 若未在目标项目被赋予上述项目角色，一律拒绝——不依赖 {@code @PreAuthorize} 全局权限字符串放行。
 *
 * @author cat2bug
 */
@ApplicationScoped
public class SysGitRepositoryServiceImpl implements ISysGitRepositoryService
{
    /** manage：project.admin / project.create-by */
    private static final Set<Long> MANAGE_ROLE_IDS = Collections.unmodifiableSet(Set.of(6L, 11L));

    /** pull/push 额外角色（与 manage 取并集）：project.develop / project.tester */
    private static final Set<Long> READ_WRITE_ROLE_IDS = Collections.unmodifiableSet(Set.of(7L, 8L));

    private static final String DEFAULT_BRANCH = "master";

    /** 开源：所有人可见 */
    private static final String VISIBILITY_PUBLIC = "public";

    /** 项目内部开源 */
    private static final String VISIBILITY_PROJECT = "project";

    @Inject
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Inject
    private SysGitRepositoryMemberMapper sysGitRepositoryMemberMapper;

    @Inject
    private SysUserGitRepositoryMapper sysUserGitRepositoryMapper;

    @Inject
    private SysProjectMapper sysProjectMapper;

    @Inject
    private SysRoleMapper sysRoleMapper;

    @Inject
    private GitPathValidator gitPathValidator;

    @Inject
    private JGitBareRepositoryHelper jGitBareRepositoryHelper;

    @Inject
    private GitRemoteRepositoryClient gitRemoteRepositoryClient;

    @Inject
    private ISysGitRepositoryMemberService sysGitRepositoryMemberService;

    @Inject
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Inject
    private GitRepoLanguageScanner gitRepoLanguageScanner;

    /**
     * 查询仓库（要求当前用户对该仓库具备 pull 及以上有效权限）
     */
    @Override
    public SysGitRepository selectSysGitRepositoryByRepositoryId(Long repositoryId)
    {
        SysGitRepository repository = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repository == null)
        {
            return null;
        }
        assertRepoPull(repositoryId);
        return repository;
    }

    /**
     * 查询项目下的仓库列表（要求 projectId 非空，且当前用户在该项目具备 pull 及以上权限）
     */
    @Override
    public List<SysGitRepository> selectSysGitRepositoryList(SysGitRepository sysGitRepository)
    {
        if (sysGitRepository == null || sysGitRepository.getProjectId() == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.project-id-required"));
        }
        assertPull(sysGitRepository.getProjectId());
        sysGitRepository.getParams().put("userId", currentUserId());
        List<SysGitRepository> list = sysGitRepositoryMapper.selectSysGitRepositoryList(sysGitRepository);
        for (SysGitRepository repo : list)
        {
            if (repo.getCollectCount() == null)
            {
                repo.setCollectCount(0);
            }
            String diskPath = repo.getDiskPath();
            if (StringUtils.isNotEmpty(diskPath))
            {
                try
                {
                    diskPath = gitPathValidator.resolveStoredDiskPath(diskPath);
                }
                catch (RuntimeException ignored)
                {
                    // 列表侧统计失败时降级为原路径 / 0，不阻断列表
                }
            }
            repo.setBranchCount(jGitBareRepositoryHelper.countLocalBranches(diskPath));
        }
        return list;
    }

    /**
     * 查询项目级 Git 概览（design.md D1/D2/D3；OpenSpec {@code git-repo-project-overview} 任务 1.3）。
     * <p>鉴权与仓库列表同一门（项目 pull 及以上权限），拒绝时抛出 {@code HttpStatus.FORBIDDEN}
     * 的 {@link ApiException}，确保 Controller 侧统一返回 403（与列表接口共用判定但显式带码，
     * 见 tasks 1.4 「unauthorized denial」用例）。可见仓库集合为空时短路返回全零聚合，不触发语言扫描。</p>
     */
    @Override
    public GitProjectOverview selectProjectOverview(Long projectId)
    {
        if (projectId == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.project-id-required"));
        }
        if (!hasPullPermission(projectId, currentUserId()))
        {
            throw new ApiException(MessageUtils.message("no-permission"), HttpStatus.FORBIDDEN);
        }

        GitProjectOverview overview = sysGitRepositoryMapper.selectProjectOverviewCounts(projectId);
        if (overview == null)
        {
            overview = new GitProjectOverview();
        }
        if (overview.getRepoCount() <= 0)
        {
            overview.setLanguages(Collections.emptyList());
            return overview;
        }

        List<SysGitRepository> visibleRepositories = sysGitRepositoryMapper.selectRepositoriesForOverviewScan(projectId);
        overview.setLanguages(gitRepoLanguageScanner.scan(visibleRepositories));
        return overview;
    }

    /**
     * 项目下各仓库 ACL 成员去重预览（源码成员）。鉴权与 {@link #selectProjectOverview} 一致。
     */
    @Override
    public List<SysGitRepositoryMember> selectProjectGitMembers(Long projectId)
    {
        if (projectId == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.project-id-required"));
        }
        if (!hasPullPermission(projectId, currentUserId()))
        {
            throw new ApiException(MessageUtils.message("no-permission"), HttpStatus.FORBIDDEN);
        }
        List<SysGitRepositoryMember> members =
                sysGitRepositoryMemberMapper.selectDistinctMembersByProjectId(projectId);
        return members != null ? members : Collections.emptyList();
    }

    /**
     * 关注 / 取消关注仓库
     */
    @Override
    @Transactional
    public int collectSysGitRepository(Long repositoryId, boolean collect)
    {
        SysGitRepository repository = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repository == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.not-found"));
        }
        assertRepoPull(repositoryId);

        Long userId = currentUserId();
        String operator = SecurityUtils.getUsername();
        SysUserGitRepository existing = sysUserGitRepositoryMapper.selectByUserIdAndRepositoryId(userId, repositoryId);
        if (existing == null)
        {
            SysUserGitRepository row = new SysUserGitRepository();
            row.setUserId(userId);
            row.setRepositoryId(repositoryId);
            row.setCollect(collect);
            row.setCreateBy(operator);
            row.setUpdateBy(operator);
            return sysUserGitRepositoryMapper.insertSysUserGitRepository(row);
        }
        existing.setCollect(collect);
        existing.setUpdateBy(operator);
        return sysUserGitRepositoryMapper.updateCollectByUserIdAndRepositoryId(existing);
    }

    /**
     * 新增仓库：校验 manage 权限、slug 格式与项目内唯一性，通过 JGit 在磁盘初始化 bare 仓；
     * 元数据写入失败时清理已创建的磁盘目录，避免遗留孤儿仓库。
     */
    @Override
    @Transactional
    public SysGitRepository insertSysGitRepository(SysGitRepository sysGitRepository)
    {
        if (sysGitRepository == null || sysGitRepository.getProjectId() == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.project-id-required"));
        }
        Long projectId = sysGitRepository.getProjectId();
        assertManage(projectId);

        PreparedRepositoryPaths prepared = prepareNewRepositoryPaths(projectId, sysGitRepository.getName(),
            sysGitRepository.getSlug());
        String diskPath = prepared.diskPath;
        String slug = prepared.slug;
        gitPathValidator.ensureParentWritable(diskPath);

        String defaultBranch = StringUtils.isNotEmpty(sysGitRepository.getDefaultBranch())
            ? sysGitRepository.getDefaultBranch() : DEFAULT_BRANCH;
        String visibility = normalizeVisibility(sysGitRepository.getVisibility());

        SysGitRepository toInsert = new SysGitRepository();
        toInsert.setProjectId(projectId);
        toInsert.setName(StringUtils.isNotEmpty(sysGitRepository.getName()) ? sysGitRepository.getName() : slug);
        toInsert.setSlug(slug);
        toInsert.setDiskPath(diskPath);
        toInsert.setDefaultBranch(defaultBranch);
        toInsert.setVisibility(visibility);
        toInsert.setRemark(sysGitRepository.getRemark());
        toInsert.setCreateBy(SecurityUtils.getUsername());
        toInsert.setCreateTime(DateUtils.getNowDate());
        toInsert.setUpdateBy(SecurityUtils.getUsername());
        toInsert.setUpdateTime(DateUtils.getNowDate());

        jGitBareRepositoryHelper.initBareRepository(diskPath, defaultBranch);
        try
        {
            int rows = sysGitRepositoryMapper.insertSysGitRepository(toInsert);
            if (rows <= 0)
            {
                throw new ApiException(MessageUtils.message("git.repo.insert-failed"));
            }
            sysGitRepositoryMemberService.ensureCreatorAdmin(toInsert.getRepositoryId(), currentUserId());
        }
        catch (RuntimeException e)
        {
            jGitBareRepositoryHelper.wipeBareRepository(diskPath);
            throw e;
        }
        return toInsert;
    }

    /**
     * 从远端一次性导入：校验 URL → slug 唯一 → 限时 bare clone → 成功后才插入元数据；
     * 失败 wipe 磁盘且不落库。username/token 仅用于当次 clone，不写入实体。
     */
    @Override
    @Transactional
    public SysGitRepository importSysGitRepository(SysGitRepositoryImportRequest request)
    {
        if (request == null || request.getProjectId() == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.project-id-required"));
        }
        Long projectId = request.getProjectId();
        assertManage(projectId);

        String sourceUrl = GitImportUrlValidator.validateCloneUrl(request.getSourceUrl());
        PreparedRepositoryPaths prepared = prepareNewRepositoryPaths(projectId, request.getName(), request.getSlug());
        String diskPath = prepared.diskPath;
        String slug = prepared.slug;
        gitPathValidator.ensureParentWritable(diskPath);

        // 凭据仅局部变量，不进入 toInsert
        String username = request.getUsername();
        String token = request.getToken();

        try
        {
            cloneWithTimeout(sourceUrl, diskPath, username, token);
        }
        catch (RuntimeException e)
        {
            safeWipe(diskPath);
            throw e;
        }

        String defaultBranch = StringUtils.isNotEmpty(request.getDefaultBranch())
            ? request.getDefaultBranch()
            : jGitBareRepositoryHelper.resolveDefaultBranch(diskPath);
        if (StringUtils.isEmpty(defaultBranch))
        {
            defaultBranch = DEFAULT_BRANCH;
        }
        String visibility = normalizeVisibility(request.getVisibility());

        SysGitRepository toInsert = new SysGitRepository();
        toInsert.setProjectId(projectId);
        toInsert.setName(StringUtils.isNotEmpty(request.getName()) ? request.getName() : slug);
        toInsert.setSlug(slug);
        toInsert.setDiskPath(diskPath);
        toInsert.setDefaultBranch(defaultBranch);
        toInsert.setVisibility(visibility);
        toInsert.setRemark(request.getRemark());
        toInsert.setCreateBy(SecurityUtils.getUsername());
        toInsert.setCreateTime(DateUtils.getNowDate());
        toInsert.setUpdateBy(SecurityUtils.getUsername());
        toInsert.setUpdateTime(DateUtils.getNowDate());

        try
        {
            int rows = sysGitRepositoryMapper.insertSysGitRepository(toInsert);
            if (rows <= 0)
            {
                throw new ApiException(MessageUtils.message("git.repo.insert-failed"));
            }
            sysGitRepositoryMemberService.ensureCreatorAdmin(toInsert.getRepositoryId(), currentUserId());
        }
        catch (RuntimeException e)
        {
            safeWipe(diskPath);
            throw e;
        }
        return toInsert;
    }

    /**
     * 列远端仓（manage 权限不绑定 projectId：入口层已有 manage 菜单权限；
     * 此处要求 token 非空，由 {@link GitRemoteRepositoryClient} 完成出站调用）。
     * <p>
     * 为与「导入同属 manage」一致，调用方须已通过 {@code @PreAuthorize}；
     * 本方法不再二次校验项目角色（列仓无 projectId）。
     */
    @Override
    public List<SysGitRemoteRepository> listRemoteRepositories(SysGitRemoteListRequest request)
    {
        return gitRemoteRepositoryClient.listRepositories(request);
    }

    /**
     * 校验 slug 格式与项目内唯一性，并计算默认磁盘路径（与空白新建共用）。
     */
    private PreparedRepositoryPaths prepareNewRepositoryPaths(Long projectId, String name, String rawSlugInput)
    {
        String rawSlug = rawSlugInput;
        if (StringUtils.isEmpty(rawSlug))
        {
            rawSlug = gitPathValidator.suggestSlugFromName(name);
        }
        String slug = gitPathValidator.validateSlug(rawSlug);
        if (sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(projectId, slug) != null)
        {
            throw new ApiException(MessageUtils.message("git.repo.slug-duplicate"));
        }

        SysProject project = requireProjectWithPathKeys(projectId);
        String diskPath = gitPathValidator.buildDefaultDiskPath(project.getTeamKey(), project.getProjectKey(), slug);
        diskPath = gitPathValidator.resolveStoredDiskPath(diskPath);
        return new PreparedRepositoryPaths(slug, diskPath);
    }

    private void cloneWithTimeout(String sourceUrl, String diskPath, String username, String token)
    {
        int timeoutSeconds = resolveImportTimeoutSeconds();
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "git-repo-import-clone");
            t.setDaemon(true);
            return t;
        });
        Future<?> future = executor.submit(() ->
            jGitBareRepositoryHelper.cloneBareRepository(sourceUrl, diskPath, username, token));
        try
        {
            future.get(timeoutSeconds, TimeUnit.SECONDS);
        }
        catch (TimeoutException e)
        {
            future.cancel(true);
            throw new ApiException(MessageUtils.message("git.repo.import.timeout"));
        }
        catch (ExecutionException e)
        {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ApiException)
            {
                throw (ApiException) cause;
            }
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            throw new ApiException(MessageUtils.message("git.repo.import.clone-failed"))
                .setDetailMessage(cause.getClass().getSimpleName());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new ApiException(MessageUtils.message("git.repo.import.clone-failed"));
        }
        finally
        {
            executor.shutdownNow();
        }
    }

    private int resolveImportTimeoutSeconds()
    {
        return Cat2BugConfig.getImportTimeoutSeconds();
    }

    private void safeWipe(String diskPath)
    {
        try
        {
            jGitBareRepositoryHelper.wipeBareRepository(diskPath);
        }
        catch (RuntimeException ignored)
        {
            // 清理失败不掩盖原始导入错误
        }
    }

    private static final class PreparedRepositoryPaths
    {
        private final String slug;
        private final String diskPath;

        private PreparedRepositoryPaths(String slug, String diskPath)
        {
            this.slug = slug;
            this.diskPath = diskPath;
        }
    }

    /**
     * 修改仓库：校验 manage 权限；slug 或显式路径变更时联动迁移磁盘目录，
     * 返回对象的 {@link SysGitRepository#isSlugChanged()} 供 Controller 组装「旧 clone 地址已失效」提示。
     */
    @Override
    @Transactional
    public SysGitRepository updateSysGitRepository(SysGitRepository sysGitRepository)
    {
        if (sysGitRepository == null || sysGitRepository.getRepositoryId() == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.repository-id-required"));
        }
        SysGitRepository existing = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(sysGitRepository.getRepositoryId());
        if (existing == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.not-found"));
        }
        assertRepoManage(existing.getRepositoryId());

        boolean slugChanged = false;
        String newSlug = existing.getSlug();
        if (StringUtils.isNotEmpty(sysGitRepository.getSlug()) && !sysGitRepository.getSlug().trim().equals(existing.getSlug()))
        {
            newSlug = gitPathValidator.validateSlug(sysGitRepository.getSlug());
            SysGitRepository conflict = sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(existing.getProjectId(), newSlug);
            if (conflict != null && !conflict.getRepositoryId().equals(existing.getRepositoryId()))
            {
                throw new ApiException(MessageUtils.message("git.repo.slug-duplicate"));
            }
            slugChanged = true;
        }

        String existingDiskPath = gitPathValidator.resolveStoredDiskPath(existing.getDiskPath());
        String targetDiskPath = existingDiskPath;
        boolean diskMoveNeeded = false;
        if (slugChanged)
        {
            SysProject project = requireProjectWithPathKeys(existing.getProjectId());
            String defaultForOldSlug = gitPathValidator.buildDefaultDiskPath(
                project.getTeamKey(), project.getProjectKey(), existing.getSlug());
            String legacyDefaultForOldSlug = gitPathValidator.buildLegacyDefaultDiskPath(
                existing.getProjectId(), existing.getSlug());
            if (defaultForOldSlug.equals(existingDiskPath) || legacyDefaultForOldSlug.equals(existingDiskPath))
            {
                // 原本走默认路径规则（含旧版 projectId 布局），slug 改名时同步迁移到新规则目录
                targetDiskPath = gitPathValidator.buildDefaultDiskPath(
                    project.getTeamKey(), project.getProjectKey(), newSlug);
                diskMoveNeeded = true;
            }
            // 否则说明磁盘路径是此前手工指定的绝对路径，与 slug 解耦，不随 slug 改名移动
        }

        if (diskMoveNeeded)
        {
            gitPathValidator.ensureParentWritable(targetDiskPath);
            jGitBareRepositoryHelper.moveBareRepository(existingDiskPath, targetDiskPath);
        }

        SysGitRepository toPersist = new SysGitRepository();
        toPersist.setRepositoryId(existing.getRepositoryId());
        if (StringUtils.isNotEmpty(sysGitRepository.getName()))
        {
            toPersist.setName(sysGitRepository.getName());
        }
        if (slugChanged)
        {
            toPersist.setSlug(newSlug);
        }
        if (diskMoveNeeded)
        {
            toPersist.setDiskPath(targetDiskPath);
        }
        if (StringUtils.isNotEmpty(sysGitRepository.getDefaultBranch()))
        {
            toPersist.setDefaultBranch(sysGitRepository.getDefaultBranch());
        }
        if (StringUtils.isNotEmpty(sysGitRepository.getVisibility()))
        {
            toPersist.setVisibility(normalizeVisibility(sysGitRepository.getVisibility()));
        }
        if (sysGitRepository.getRemark() != null)
        {
            toPersist.setRemark(sysGitRepository.getRemark());
        }
        String operator = SecurityUtils.getUsername();
        Date now = DateUtils.getNowDate();
        toPersist.setUpdateBy(operator);
        toPersist.setUpdateTime(now);

        int rows = sysGitRepositoryMapper.updateSysGitRepository(toPersist);
        if (rows <= 0)
        {
            throw new ApiException(MessageUtils.message("git.repo.update-failed"));
        }

        SysGitRepository merged = new SysGitRepository();
        merged.setRepositoryId(existing.getRepositoryId());
        merged.setProjectId(existing.getProjectId());
        merged.setName(StringUtils.isNotEmpty(sysGitRepository.getName()) ? sysGitRepository.getName() : existing.getName());
        merged.setSlug(slugChanged ? newSlug : existing.getSlug());
        merged.setDiskPath(diskMoveNeeded ? targetDiskPath : existingDiskPath);
        merged.setDefaultBranch(StringUtils.isNotEmpty(sysGitRepository.getDefaultBranch()) ? sysGitRepository.getDefaultBranch() : existing.getDefaultBranch());
        merged.setVisibility(StringUtils.isNotEmpty(sysGitRepository.getVisibility())
            ? normalizeVisibility(sysGitRepository.getVisibility()) : existing.getVisibility());
        merged.setRemark(sysGitRepository.getRemark() != null ? sysGitRepository.getRemark() : existing.getRemark());
        merged.setCreateBy(existing.getCreateBy());
        merged.setCreateTime(existing.getCreateTime());
        merged.setUpdateBy(operator);
        merged.setUpdateTime(now);
        merged.setSlugChanged(slugChanged);
        return merged;
    }

    /**
     * 删除仓库信息（校验 manage 权限；wipeDisk=true 时同步清空磁盘目录）
     */
    @Override
    @Transactional
    public int deleteSysGitRepositoryByRepositoryId(Long repositoryId, boolean wipeDisk)
    {
        SysGitRepository existing = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (existing == null)
        {
            return 0;
        }
        assertRepoManage(repositoryId);
        int rows = sysGitRepositoryMapper.deleteSysGitRepositoryByRepositoryId(repositoryId);
        if (rows > 0)
        {
            sysUserGitRepositoryMapper.deleteByRepositoryId(repositoryId);
            sysGitRepositoryMemberMapper.deleteByRepositoryId(repositoryId);
            if (wipeDisk)
            {
                jGitBareRepositoryHelper.wipeBareRepository(gitPathValidator.resolveStoredDiskPath(existing.getDiskPath()));
            }
        }
        return rows;
    }

    /**
     * 批量删除仓库（逐条校验 manage 权限；跳过不存在的主键）
     */
    @Override
    @Transactional
    public int deleteSysGitRepositoryByRepositoryIds(Long[] repositoryIds, boolean wipeDisk)
    {
        if (repositoryIds == null)
        {
            return 0;
        }
        int rows = 0;
        for (Long repositoryId : repositoryIds)
        {
            rows += deleteSysGitRepositoryByRepositoryId(repositoryId, wipeDisk);
        }
        return rows;
    }

    /**
     * manage 权限校验：项目角色 project.admin / project.create-by 并集；不满足直接抛出无权限异常。
     * 用于新建/导入（尚无 repositoryId）与项目级列表。
     */
    private void assertManage(Long projectId)
    {
        if (!hasManagePermission(projectId, currentUserId()))
        {
            throw new ApiException(MessageUtils.message("no-permission"));
        }
    }

    /**
     * pull/push 权限校验：manage 并集 project.develop / project.tester；不满足直接抛出无权限异常。
     * 用于项目级列表。
     */
    private void assertPull(Long projectId)
    {
        if (!hasPullPermission(projectId, currentUserId()))
        {
            throw new ApiException(MessageUtils.message("no-permission"));
        }
    }

    /** 仓级有效 pull（ACL 优先） */
    private void assertRepoPull(Long repositoryId)
    {
        if (!gitRepositoryAccessService.canPull(currentUserId(), repositoryId))
        {
            throw new ApiException(MessageUtils.message("no-permission"));
        }
    }

    /** 仓级有效 manage（ACL 优先） */
    private void assertRepoManage(Long repositoryId)
    {
        if (!gitRepositoryAccessService.canManage(currentUserId(), repositoryId))
        {
            throw new ApiException(MessageUtils.message("no-permission"));
        }
    }

    private boolean hasManagePermission(Long projectId, Long userId)
    {
        return matchesAnyRole(projectId, userId, MANAGE_ROLE_IDS);
    }

    private boolean hasPullPermission(Long projectId, Long userId)
    {
        if (hasManagePermission(projectId, userId))
        {
            return true;
        }
        return matchesAnyRole(projectId, userId, READ_WRITE_ROLE_IDS);
    }

    /**
     * 按 projectId 查询当前用户的项目角色（不传 teamId，故团队角色不参与并集），
     * 判断其角色ID是否落在给定集合内。系统超级管理员直接放行；团队管理员未被赋予项目角色时不放行。
     */
    private boolean matchesAnyRole(Long projectId, Long userId, Set<Long> roleIds)
    {
        if (projectId == null || userId == null)
        {
            return false;
        }
        if (gitRepositoryAccessService.isSystemAdmin(userId))
        {
            return true;
        }
        List<SysRole> roles = sysRoleMapper.selectRolePermissionByUserId(null, projectId, userId);
        if (roles == null)
        {
            return false;
        }
        return roles.stream()
            .map(SysRole::getRoleId)
            .filter(Objects::nonNull)
            .anyMatch(roleIds::contains);
    }

    private String normalizeVisibility(String visibility)
    {
        if (VISIBILITY_PUBLIC.equals(visibility))
        {
            return VISIBILITY_PUBLIC;
        }
        return VISIBILITY_PROJECT;
    }

    private SysProject requireProjectWithPathKeys(Long projectId)
    {
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
        if (project == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.project-not-found"));
        }
        if (StringUtils.isEmpty(project.getTeamKey()) || StringUtils.isEmpty(project.getProjectKey()))
        {
            throw new ApiException(MessageUtils.message("git.repo.path-keys-required"));
        }
        return project;
    }

    private Long currentUserId()
    {
        return SecurityUtils.getUserId();
    }
}
