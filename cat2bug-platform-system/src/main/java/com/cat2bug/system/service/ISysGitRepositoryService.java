package com.cat2bug.system.service;

import java.util.List;

import com.cat2bug.system.domain.SysGitRepository;

/**
 * 项目代码仓库Service接口
 *
 * 除持久化 CRUD 外，Service 层显式校验当前登录用户在目标 {@code projectId} 下的项目角色
 * （{@code project.admin} / {@code project.create-by} / {@code project.develop} / {@code project.tester}），
 * 不依赖 {@code @PreAuthorize} 全局权限字符串放行，也不因系统管理员 / 团队管理员身份绕过——
 * 详见 design.md D3 与 tasks 2.5。
 *
 * @author cat2bug
 */
public interface ISysGitRepositoryService
{
    /**
     * 查询仓库（要求当前用户在该仓库所属项目具备 pull 及以上权限）
     *
     * @param repositoryId 仓库主键
     * @return 仓库
     */
    public SysGitRepository selectSysGitRepositoryByRepositoryId(Long repositoryId);

    /**
     * 查询项目下的仓库列表（要求 {@code sysGitRepository.projectId} 非空，且当前用户在该项目具备 pull 及以上权限）
     *
     * @param sysGitRepository 查询条件，必须携带 projectId
     * @return 仓库集合
     */
    public List<SysGitRepository> selectSysGitRepositoryList(SysGitRepository sysGitRepository);

    /**
     * 新增仓库：校验 manage 权限、slug 格式与项目内唯一性，随后通过 JGit 在磁盘初始化 bare 仓
     *
     * @param sysGitRepository 仓库（磁盘路径由系统按 teamKey/projectKey/slug 自动生成）
     * @return 新增后的仓库（含计算出的磁盘路径与 clone 地址）
     */
    public SysGitRepository insertSysGitRepository(SysGitRepository sysGitRepository);

    /**
     * 修改仓库：校验 manage 权限；slug/绝对路径变更时联动迁移磁盘目录
     *
     * @param sysGitRepository 待更新字段（repositoryId 必填）
     * @return 更新后的仓库；{@link SysGitRepository#isSlugChanged()} 标记本次是否发生 slug 重命名
     */
    public SysGitRepository updateSysGitRepository(SysGitRepository sysGitRepository);

    /**
     * 删除仓库（校验 manage 权限）
     *
     * @param repositoryId 仓库主键
     * @param wipeDisk 是否同步清空磁盘上的 bare 仓目录
     * @return 结果
     */
    public int deleteSysGitRepositoryByRepositoryId(Long repositoryId, boolean wipeDisk);

    /**
     * 批量删除仓库（逐条校验 manage 权限）
     *
     * @param repositoryIds 需要删除的仓库主键
     * @param wipeDisk 是否同步清空磁盘上的 bare 仓目录
     * @return 结果
     */
    public int deleteSysGitRepositoryByRepositoryIds(Long[] repositoryIds, boolean wipeDisk);

    /**
     * 关注 / 取消关注仓库（要求当前用户对该仓库具备 pull 及以上权限）
     *
     * @param repositoryId 仓库主键
     * @param collect 是否关注
     * @return 影响行数
     */
    public int collectSysGitRepository(Long repositoryId, boolean collect);
}
