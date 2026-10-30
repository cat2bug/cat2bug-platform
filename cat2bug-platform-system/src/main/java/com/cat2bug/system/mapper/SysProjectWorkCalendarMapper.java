package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysProjectWorkCalendar;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysProjectWorkCalendarMapper {

    SysProjectWorkCalendar selectByProjectId(@Param("projectId") Long projectId);

    int insert(SysProjectWorkCalendar row);

    int update(SysProjectWorkCalendar row);

    int deleteByProjectId(@Param("projectId") Long projectId);
}
