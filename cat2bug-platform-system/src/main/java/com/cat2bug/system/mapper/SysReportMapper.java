package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysReportMapper {

    long countSysReportList(@Param("query") SysReport query);

    List<SysReport> selectSysReportList(@Param("query") SysReport query,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    SysReport selectSysReportByReportId(@Param("reportId") Long reportId);

    int insertSysReport(SysReport sysReport);

    int updateSysReport(SysReport sysReport);

    int deleteSysReportByReportId(@Param("reportId") Long reportId);

    int deleteSysReportByReportIds(@Param("reportIds") Long[] reportIds);
}
