package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.open.ApiMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface OpenApiMemberMapper {

    List<ApiMember> selectMemberList(@Param("projectId") Long projectId, @Param("apiMember") ApiMember apiMember);

    List<ApiMember> selectMemberByNames(@Param("projectId") Long projectId, @Param("userNames") List<String> userNames);
}
