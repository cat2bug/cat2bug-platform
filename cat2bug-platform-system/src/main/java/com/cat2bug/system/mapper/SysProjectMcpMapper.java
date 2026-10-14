package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysProjectMcp;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysProjectMcpMapper
{
    SysProjectMcp selectByProjectId(@Param("projectId") Long projectId);

    int insert(SysProjectMcp row);

    int update(SysProjectMcp row);

    int deleteByProjectId(@Param("projectId") Long projectId);

    /** 用户所在、未删除且已开启 MCP 的项目（含团队路径键） */
    List<SysProject> selectEnabledProjectsOfUser(@Param("userId") Long userId);

    int countMember(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
