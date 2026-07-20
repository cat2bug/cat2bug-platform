package com.cat2bug.system.mapper;

import java.util.List;

import com.cat2bug.system.domain.SysGitRepository;
import org.apache.ibatis.annotations.Param;

/**
 * 项目代码仓库Mapper接口
 *
 * @author cat2bug
 */
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
}
