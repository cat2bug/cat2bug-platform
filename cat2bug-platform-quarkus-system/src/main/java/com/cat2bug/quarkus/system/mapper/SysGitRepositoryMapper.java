package com.cat2bug.quarkus.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;

import java.util.List;

import com.cat2bug.quarkus.common.domain.SysGitRepository;
import com.cat2bug.quarkus.system.git.browse.domain.GitProjectOverview;
import org.apache.ibatis.annotations.Param;

/**
 * 项目代码仓库Mapper接口
 *
 * @author cat2bug
 */
@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysGitRepositoryMapper
{
    /**
     * 查询仓库
     *
     * @param repositoryId 仓库主键
     * @return 仓库
     */
    public SysGitRepository selectSysGitRepositoryByRepositoryId(Long repositoryId);

    /**
     * 按项目ID + slug 查询仓库（校验 slug 唯一性 / 定位仓库）
     *
     * @param projectId 项目ID
     * @param slug slug
     * @return 仓库，不存在返回 null
     */
    public SysGitRepository selectSysGitRepositoryByProjectIdAndSlug(@Param("projectId") Long projectId, @Param("slug") String slug);

    /**
     * 按 teamKey + projectKey + slug 查询仓库（Smart HTTP namespaced URL）
     */
    public SysGitRepository selectSysGitRepositoryByTeamKeyAndProjectKeyAndSlug(
            @Param("teamKey") String teamKey,
            @Param("projectKey") String projectKey,
            @Param("slug") String slug);

    /**
     * 查询仓库列表
     *
     * @param sysGitRepository 仓库查询条件
     * @return 仓库集合
     */
    public List<SysGitRepository> selectSysGitRepositoryList(SysGitRepository sysGitRepository);

    /**
     * 新增仓库
     *
     * @param sysGitRepository 仓库
     * @return 结果
     */
    public int insertSysGitRepository(SysGitRepository sysGitRepository);

    /**
     * 修改仓库
     *
     * @param sysGitRepository 仓库
     * @return 结果
     */
    public int updateSysGitRepository(SysGitRepository sysGitRepository);

    /**
     * 删除仓库
     *
     * @param repositoryId 仓库主键
     * @return 结果
     */
    public int deleteSysGitRepositoryByRepositoryId(Long repositoryId);

    /**
     * 批量删除仓库
     *
     * @param repositoryIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysGitRepositoryByRepositoryIds(Long[] repositoryIds);

    /**
     * 统计项目级 Git 概览聚合数（OpenSpec {@code git-repo-project-overview} 任务 1.2）：
     * 与仓库列表同一可见集（{@code project_id} + {@code project_state > 0}）下的仓库数、
     * open 状态 PR 数、关注（收藏）行数合计（跨用户）。仅填充
     * {@link GitProjectOverview#getRepoCount()} / {@link GitProjectOverview#getOpenPrCount()} /
     * {@link GitProjectOverview#getCollectCount()} 三个聚合字段，{@code languages} 由 Service 层填充。
     *
     * @param projectId 项目ID
     * @return 聚合计数（projectId 无匹配仓库时三个字段均为 0，不返回 null）
     */
    public GitProjectOverview selectProjectOverviewCounts(Long projectId);

    /**
     * 查询项目级 Git 概览可见仓库集合（仅 repositoryId/diskPath/defaultBranch 等语言扫描所需字段），
     * 与 {@link #selectSysGitRepositoryList} 使用同一可见集判定（不含关注 join，语言扫描无需该字段）。
     *
     * @param projectId 项目ID
     * @return 可见仓库集合
     */
    public List<SysGitRepository> selectRepositoriesForOverviewScan(Long projectId);
}
