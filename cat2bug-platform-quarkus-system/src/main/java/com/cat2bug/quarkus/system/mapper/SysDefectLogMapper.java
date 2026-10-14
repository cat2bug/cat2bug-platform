package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysDefectLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDefectLogMapper {

    long countSysDefectLogList(@Param("log") SysDefectLog log);

    List<SysDefectLog> selectSysDefectLogList(@Param("log") SysDefectLog log,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    SysDefectLog selectSysDefectLogByDefectLogId(Long defectLogId);

    int insertSysDefectLog(SysDefectLog sysDefectLog);

    int updateSysDefectLog(SysDefectLog sysDefectLog);

    int deleteSysDefectLogByDefectLogIds(Long[] defectLogIds);

    int batchInsertSysDefectLog(@Param("logList") List<SysDefectLog> logList);
}
