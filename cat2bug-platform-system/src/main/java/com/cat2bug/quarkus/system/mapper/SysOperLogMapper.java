package com.cat2bug.quarkus.system.mapper;

import com.cat2bug.quarkus.common.domain.SysOperLog;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysOperLogMapper {

    void insertOperlog(SysOperLog operLog);

    long countOperLogList(@Param("query") SysOperLog query);

    List<SysOperLog> selectOperLogList(@Param("query") SysOperLog query,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    List<SysOperLog> selectOperLogListAll(@Param("query") SysOperLog query);

    SysOperLog selectOperLogById(@Param("operId") Long operId);

    int deleteOperLogByIds(@Param("array") Long[] operIds);

    void cleanOperLog();
}
