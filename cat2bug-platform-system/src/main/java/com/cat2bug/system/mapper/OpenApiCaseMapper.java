package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.open.ApiCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface OpenApiCaseMapper {

    List<ApiCase> selectApiCaseList(@Param("projectId") Long projectId, @Param("apiCase") ApiCase apiCase);

    ApiCase selectApiCaseByCaseNum(@Param("projectId") Long projectId, @Param("caseNum") Long caseNum);
}
