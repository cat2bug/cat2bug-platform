package com.cat2bug.quarkus.system.mapper;

import com.cat2bug.quarkus.common.domain.SysLogininfor;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysLogininforMapper {

    void insertLogininfor(SysLogininfor logininfor);

    long countLogininforList(@Param("query") SysLogininfor query);

    List<SysLogininfor> selectLogininforList(@Param("query") SysLogininfor query,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    List<SysLogininfor> selectLogininforListAll(@Param("query") SysLogininfor query);

    int deleteLogininforByIds(@Param("array") Long[] infoIds);

    int cleanLogininfor();
}
