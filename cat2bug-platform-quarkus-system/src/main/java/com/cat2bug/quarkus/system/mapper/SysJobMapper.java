package com.cat2bug.quarkus.system.mapper;

import com.cat2bug.quarkus.common.domain.SysJob;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysJobMapper {

    long countJobList(@Param("query") SysJob query);

    List<SysJob> selectJobList(@Param("query") SysJob query,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    List<SysJob> selectJobListAll(@Param("query") SysJob query);

    List<SysJob> selectJobAll();

    SysJob selectJobById(@Param("jobId") Long jobId);

    int deleteJobById(@Param("jobId") Long jobId);

    int updateJob(SysJob job);

    int insertJob(SysJob job);
}
