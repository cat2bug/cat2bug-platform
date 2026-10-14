package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface OpenApiReportMapper {

    SysReport findSysReportByReportKey(@Param("reportKey") String reportKey);

    int insertSysReport(SysReport report);

    int updateSysReport(SysReport report);
}
