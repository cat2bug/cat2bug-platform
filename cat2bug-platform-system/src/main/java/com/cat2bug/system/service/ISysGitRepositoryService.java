package com.cat2bug.system.service;

import java.util.List;

import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.git.SysGitRemoteListRequest;
import com.cat2bug.system.domain.git.SysGitRemoteRepository;
import com.cat2bug.system.domain.git.SysGitRepositoryImportRequest;
import com.cat2bug.system.git.browse.domain.GitProjectOverview;

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

    /**
     * 从远端 URL 一次性导入为本地 bare 仓：先 clone 成功再落库；失败 wipe 磁盘且不插入。
     * 凭据仅请求作用域，不持久化。
     *
     * @param request 公共元数据 + sourceUrl + 可选 username/token
     * @return 创建后的仓库（与新增空白仓同形）
     */
    public SysGitRepository importSysGitRepository(SysGitRepositoryImportRequest request);

    /**
     * 使用会话内 PAT 列远端仓库（GitHub / GitLab / Gitee）；token 不持久化。
     *
     * @param request provider + token + 可选 GitLab host
     * @return 仓库摘要列表
     */
    public List<SysGitRemoteRepository> listRemoteRepositories(SysGitRemoteListRequest request);

    /**
     * 查询项目级 Git 概览（OpenSpec {@code git-repo-project-overview}）：要求当前用户在该项目
     * 具备 pull 及以上权限（与仓库列表同一鉴权门），否则拒绝。可见仓库集合为空时返回全零聚合与空语言列表；
     * 单仓语言扫描失败不影响整体聚合结果（design.md D3）。
     *
     * @param projectId 项目ID（非空）
     * @return 项目概览：仓库数、open PR 数、关注合计、语言分布
     */
    public GitProjectOverview selectProjectOverview(Long projectId);
}
