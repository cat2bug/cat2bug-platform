package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysProject;
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

    int insertSysProject(SysProject sysProject);

    int updateSysProject(SysProject sysProject);

    int deleteSysProjectByProjectId(@Param("projectId") Long projectId);
}
