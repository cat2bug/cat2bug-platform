package com.cat2bug.system.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.service.ISysGitRepositoryService;
import com.cat2bug.system.service.git.GitPathValidator;
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
@Service
public class SysGitRepositoryServiceImpl implements ISysGitRepositoryService
{
    /** manage：project.admin / project.create-by */
    private static final Set<Long> MANAGE_ROLE_IDS = Collections.unmodifiableSet(Set.of(6L, 11L));

    /** pull/push 额外角色（与 manage 取并集）：project.develop / project.tester */
    private static final Set<Long> READ_WRITE_ROLE_IDS = Collections.unmodifiableSet(Set.of(7L, 8L));

    private static final String DEFAULT_BRANCH = "main";

    @Autowired
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Autowired
    private SysProjectMapper sysProjectMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private GitPathValidator gitPathValidator;

    @Autowired
    private JGitBareRepositoryHelper jGitBareRepositoryHelper;

    /**
     * 查询仓库（要求当前用户在该仓库所属项目具备 pull 及以上权限）
     */
    @Override
    public SysGitRepository selectSysGitRepositoryByRepositoryId(Long repositoryId)
    {
        SysGitRepository repository = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repository == null)
        {
            return null;
        }
        assertPull(repository.getProjectId());
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
            throw new ServiceException(MessageUtils.message("git.repo.project-id-required"));
        }
        assertPull(sysGitRepository.getProjectId());
        return sysGitRepositoryMapper.selectSysGitRepositoryList(sysGitRepository);
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
            throw new ServiceException(MessageUtils.message("git.repo.project-id-required"));
        }
        Long projectId = sysGitRepository.getProjectId();
        assertManage(projectId);
        assertProjectExists(projectId);

        String slug = gitPathValidator.validateSlug(sysGitRepository.getSlug());
        if (sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(projectId, slug) != null)
        {
            throw new ServiceException(MessageUtils.message("git.repo.slug-duplicate"));
        }

        String diskPath = StringUtils.isNotEmpty(sysGitRepository.getDiskPath())
            ? gitPathValidator.normalizeAndValidateAbsolutePath(sysGitRepository.getDiskPath())
            : gitPathValidator.buildDefaultDiskPath(projectId, slug);
        // 再绝对化一次：默认路径与历史相对路径输入均落到稳定绝对路径再落库
        diskPath = gitPathValidator.resolveStoredDiskPath(diskPath);
        gitPathValidator.ensureParentWritable(diskPath);

        String defaultBranch = StringUtils.isNotEmpty(sysGitRepository.getDefaultBranch())
            ? sysGitRepository.getDefaultBranch() : DEFAULT_BRANCH;

        SysGitRepository toInsert = new SysGitRepository();
        toInsert.setProjectId(projectId);
        toInsert.setName(StringUtils.isNotEmpty(sysGitRepository.getName()) ? sysGitRepository.getName() : slug);
        toInsert.setSlug(slug);
        toInsert.setDiskPath(diskPath);
        toInsert.setDefaultBranch(defaultBranch);
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
                throw new ServiceException(MessageUtils.message("git.repo.insert-failed"));
            }
        }
        catch (RuntimeException e)
        {
            jGitBareRepositoryHelper.wipeBareRepository(diskPath);
            throw e;
        }
        return toInsert;
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
            throw new ServiceException(MessageUtils.message("git.repo.repository-id-required"));
        }
        SysGitRepository existing = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(sysGitRepository.getRepositoryId());
        if (existing == null)
        {
            throw new ServiceException(MessageUtils.message("git.repo.not-found"));
        }
        assertManage(existing.getProjectId());

        boolean slugChanged = false;
        String newSlug = existing.getSlug();
        if (StringUtils.isNotEmpty(sysGitRepository.getSlug()) && !sysGitRepository.getSlug().trim().equals(existing.getSlug()))
        {
            newSlug = gitPathValidator.validateSlug(sysGitRepository.getSlug());
            SysGitRepository conflict = sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(existing.getProjectId(), newSlug);
            if (conflict != null && !conflict.getRepositoryId().equals(existing.getRepositoryId()))
            {
                throw new ServiceException(MessageUtils.message("git.repo.slug-duplicate"));
            }
            slugChanged = true;
        }

        String existingDiskPath = gitPathValidator.resolveStoredDiskPath(existing.getDiskPath());
        String targetDiskPath = existingDiskPath;
        boolean diskMoveNeeded = false;
        if (StringUtils.isNotEmpty(sysGitRepository.getDiskPath())
            && !gitPathValidator.resolveStoredDiskPath(sysGitRepository.getDiskPath()).equals(existingDiskPath))
        {
            // manage 显式改路径：优先于「slug 联动改默认路径」
            targetDiskPath = gitPathValidator.normalizeAndValidateAbsolutePath(sysGitRepository.getDiskPath());
            diskMoveNeeded = true;
        }
        else if (slugChanged)
        {
            String defaultForOldSlug = gitPathValidator.buildDefaultDiskPath(existing.getProjectId(), existing.getSlug());
            if (defaultForOldSlug.equals(existingDiskPath))
            {
                // 原本走默认路径规则，slug 改名时同步迁移目录，保持磁盘路径与 slug 一致
                targetDiskPath = gitPathValidator.buildDefaultDiskPath(existing.getProjectId(), newSlug);
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
            throw new ServiceException(MessageUtils.message("git.repo.update-failed"));
        }

        SysGitRepository merged = new SysGitRepository();
        merged.setRepositoryId(existing.getRepositoryId());
        merged.setProjectId(existing.getProjectId());
        merged.setName(StringUtils.isNotEmpty(sysGitRepository.getName()) ? sysGitRepository.getName() : existing.getName());
        merged.setSlug(slugChanged ? newSlug : existing.getSlug());
        merged.setDiskPath(diskMoveNeeded ? targetDiskPath : existingDiskPath);
        merged.setDefaultBranch(StringUtils.isNotEmpty(sysGitRepository.getDefaultBranch()) ? sysGitRepository.getDefaultBranch() : existing.getDefaultBranch());
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
        assertManage(existing.getProjectId());
        int rows = sysGitRepositoryMapper.deleteSysGitRepositoryByRepositoryId(repositoryId);
        if (rows > 0 && wipeDisk)
        {
            jGitBareRepositoryHelper.wipeBareRepository(gitPathValidator.resolveStoredDiskPath(existing.getDiskPath()));
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
     */
    private void assertManage(Long projectId)
    {
        if (!hasManagePermission(projectId, currentUserId()))
        {
            throw new ServiceException(MessageUtils.message("no-permission"));
        }
    }

    /**
     * pull/push 权限校验：manage 并集 project.develop / project.tester；不满足直接抛出无权限异常。
     */
    private void assertPull(Long projectId)
    {
        if (!hasPullPermission(projectId, currentUserId()))
        {
            throw new ServiceException(MessageUtils.message("no-permission"));
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
     * 判断其角色ID是否落在给定集合内。刻意不使用 {@code SecurityUtils.isAdmin()} /
     * {@code *:*:*} 全局放行，避免系统管理员或团队管理员在未被赋予项目角色时绕过校验。
     */
    private boolean matchesAnyRole(Long projectId, Long userId, Set<Long> roleIds)
    {
        if (projectId == null || userId == null)
        {
            return false;
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

    private void assertProjectExists(Long projectId)
    {
        if (sysProjectMapper.selectSysProjectByProjectId(projectId, null) == null)
        {
            throw new ServiceException(MessageUtils.message("git.repo.project-not-found"));
        }
    }

    private Long currentUserId()
    {
        return SecurityUtils.getUserId();
    }
}
