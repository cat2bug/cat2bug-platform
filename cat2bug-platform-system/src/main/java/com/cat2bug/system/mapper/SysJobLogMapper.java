package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysJobLog;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysJobLogMapper {

    long countJobLogList(@Param("query") SysJobLog query);

    List<SysJobLog> selectJobLogList(@Param("query") SysJobLog query,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    List<SysJobLog> selectJobLogListAll(@Param("query") SysJobLog query);

    SysJobLog selectJobLogById(@Param("jobLogId") Long jobLogId);

    int insertJobLog(SysJobLog jobLog);

    int deleteJobLogByIds(@Param("array") Long[] jobLogIds);

    int cleanJobLog();
}
