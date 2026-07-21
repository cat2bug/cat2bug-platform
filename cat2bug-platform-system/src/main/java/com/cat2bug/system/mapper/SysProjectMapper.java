package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysProject;
import org.apache.ibatis.annotations.Param;

/**
 * 项目Mapper接口
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
public interface SysProjectMapper 
{
    /**
     * 查询项目
     * 
     * @param projectId 项目主键
     * @return 项目
     */
    public SysProject selectSysProjectByProjectId( @Param("projectId") Long projectId,  @Param("userId") Long userId);

    /**
     * 查询项目列表
     * 
     * @param sysProject 项目
     * @return 项目集合
     */
    public List<SysProject> selectSysProjectList(@Param("userId") Long userId, @Param("sysProject") SysProject sysProject);

    /**
     * 按团队路径键 + 项目路径键解析项目
     */
    public SysProject selectSysProjectByTeamAndProjectKey(@Param("teamKey") String teamKey, @Param("projectKey") String projectKey);

    /**
     * 统计团队内 project_key 占用（用于唯一性校验，不受当前用户项目成员过滤）
     */
    public int countByTeamIdAndProjectKey(@Param("teamId") Long teamId,
                                          @Param("projectKey") String projectKey,
                                          @Param("excludeProjectId") Long excludeProjectId);

    /**
     * 新增项目
     * 
     * @param sysProject 项目
     * @return 结果
     */
    public int insertSysProject(SysProject sysProject);

    /**
     * 修改项目
     * 
     * @param sysProject 项目
     * @return 结果
     */
    public int updateSysProject(SysProject sysProject);

    /**
     * 删除项目
     * 
     * @param projectId 项目主键
     * @return 结果
     */
    public int deleteSysProjectByProjectId(Long projectId);

    /**
     * 批量删除项目
     * 
     * @param projectIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysProjectByProjectIds(Long[] projectIds);
}
