package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysReportHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysReportHistoryMapper {

    List<SysReportHistory> selectSysReportHistoryListByReportId(@Param("reportId") Long reportId);

    SysReportHistory selectSysReportHistoryByHistoryId(@Param("historyId") Long historyId);

    int insertSysReportHistory(SysReportHistory history);

    int deleteSysReportHistoryByHistoryId(@Param("historyId") Long historyId);

    int deleteSysReportHistoryByReportId(@Param("reportId") Long reportId);

    int deleteSysReportHistoryByReportIds(@Param("reportIds") Long[] reportIds);
}
