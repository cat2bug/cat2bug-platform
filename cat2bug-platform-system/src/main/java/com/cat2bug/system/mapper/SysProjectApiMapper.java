package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysProjectApi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysProjectApiMapper {

    long countSysProjectApiList(@Param("query") SysProjectApi query);

    List<SysProjectApi> selectSysProjectApiList(@Param("query") SysProjectApi query,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    SysProjectApi selectSysProjectApiByApiId(@Param("apiId") String apiId);

    Long selectProjectIdByApiId(@Param("apiId") String apiId);

    int insertSysProjectApi(SysProjectApi sysProjectApi);

    int updateSysProjectApi(SysProjectApi sysProjectApi);

    int deleteSysProjectApiByApiId(@Param("apiId") String apiId);

    int deleteSysProjectApiByApiIds(@Param("array") String[] apiIds);
}
