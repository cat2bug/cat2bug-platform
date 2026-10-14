package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.open.ApiDefectLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface OpenApiDefectLogMapper {

    int insertApiDefectLog(ApiDefectLog sysDefectLog);

    int batchInsertApiDefectLog(@Param("defectLogList") List<ApiDefectLog> defectLogList);
}
