package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysCaseType;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysCaseTypeMapper {

    List<SysCaseType> selectByTeamId(@Param("teamId") Long teamId);

    SysCaseType selectById(@Param("typeId") Long typeId);

    SysCaseType selectByTeamAndName(@Param("teamId") Long teamId, @Param("typeName") String typeName);

    int countCases(@Param("teamId") Long teamId, @Param("typeId") Long typeId);

    int insert(SysCaseType type);

    int updateName(SysCaseType type);

    int updateEnabled(SysCaseType type);

    int deleteById(@Param("typeId") Long typeId);

    Long selectTeamIdByProjectId(@Param("projectId") Long projectId);

    String selectProjectName(@Param("projectId") Long projectId);

    String selectReviewTemplate(@Param("teamId") Long teamId);

    int updateReviewTemplate(@Param("teamId") Long teamId, @Param("template") String template);
}
