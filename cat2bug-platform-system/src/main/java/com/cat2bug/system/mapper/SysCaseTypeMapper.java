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

    List<SysCaseType> selectByProjectId(@Param("projectId") Long projectId);

    SysCaseType selectById(@Param("typeId") Long typeId);

    SysCaseType selectByProjectAndName(@Param("projectId") Long projectId, @Param("typeName") String typeName);

    int countCases(@Param("projectId") Long projectId, @Param("typeId") Long typeId);

    int insert(SysCaseType type);

    int updateName(SysCaseType type);

    int updateEnabled(SysCaseType type);

    int deleteById(@Param("typeId") Long typeId);

    String selectProjectName(@Param("projectId") Long projectId);

    String selectReviewTemplate(@Param("projectId") Long projectId);

    int updateReviewTemplate(@Param("projectId") Long projectId, @Param("template") String template);
}
