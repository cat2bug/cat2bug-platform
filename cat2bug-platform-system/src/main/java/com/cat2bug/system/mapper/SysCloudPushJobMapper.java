package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysCloudPushJob;
import com.cat2bug.common.domain.SysCloudPushPart;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysCloudPushJobMapper {

    int insertJob(SysCloudPushJob row);

    int updateJob(SysCloudPushJob row);

    SysCloudPushJob selectJob(@Param("jobId") String jobId);

    /** 同一项目未完成任务：进行中优先，否则最近一次失败。 */
    SysCloudPushJob selectOpenByProject(@Param("projectId") Long projectId);

    List<SysCloudPushJob> selectRunning();

    int insertPart(SysCloudPushPart row);

    int updatePart(SysCloudPushPart row);

    List<SysCloudPushPart> selectParts(@Param("jobId") String jobId);
}
