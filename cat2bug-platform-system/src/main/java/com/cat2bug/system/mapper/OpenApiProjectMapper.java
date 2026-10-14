package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.open.ApiMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface OpenApiProjectMapper {

    SysProject selectProjectById(@Param("projectId") Long projectId);

    ApiMember selectCreatorByProjectId(@Param("projectId") Long projectId);

    Map<String, Object> getProjectStatistics(@Param("projectId") Long projectId);
}
