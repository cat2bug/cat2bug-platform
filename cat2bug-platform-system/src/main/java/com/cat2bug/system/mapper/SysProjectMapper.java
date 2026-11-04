package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysProjectMapper {

    long countSysProjectList(@Param("userId") Long userId, @Param("sysProject") SysProject sysProject);

    List<SysProject> selectSysProjectList(@Param("userId") Long userId,
                                          @Param("sysProject") SysProject sysProject,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    long countAdminSysProjectList(@Param("sysProject") SysProject sysProject);

    List<SysProject> selectAdminSysProjectList(@Param("sysProject") SysProject sysProject,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    SysProject selectSysProjectByProjectId(@Param("projectId") Long projectId, @Param("userId") Long userId);

    SysProject selectSysProjectByTeamAndProjectKey(@Param("teamKey") String teamKey,
                                                   @Param("projectKey") String projectKey);

    int countByTeamIdAndProjectKey(@Param("teamId") Long teamId,
                                   @Param("projectKey") String projectKey,
                                   @Param("excludeProjectId") Long excludeProjectId);

    int countActiveByTeamId(@Param("teamId") Long teamId);

    int insertSysProject(SysProject sysProject);

    int updateSysProject(SysProject sysProject);

    /** 转让创建人时同步审计字段 create_by（存用户 id 字符串）。 */
    int updateCreateBy(@Param("projectId") Long projectId, @Param("createBy") String createBy);

    int updateUsedBytes(@Param("projectId") Long projectId, @Param("usedBytes") Long usedBytes);

    /** 当前用户担任创建人或项目管理员的项目（可跨团队）。 */
    List<SysProject> selectCopySourceProjects(@Param("userId") Long userId);

    int deleteSysProjectByProjectId(@Param("projectId") Long projectId);
}
