package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysModule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysModuleMapper {

    List<SysModule> selectSysModuleList(SysModule sysModule);

    List<SysModule> selectSysModulePathList(@Param("projectId") Long projectId);

    SysModule selectSysModuleByModuleId(@Param("moduleId") Long moduleId);

    Set<Long> getAllChildIds(@Param("projectId") Long projectId, @Param("moduleId") Long moduleId);

    int insertSysModule(SysModule sysModule);

    int updateSysModule(SysModule sysModule);

    int deleteSysModuleByModuleId(@Param("moduleId") Long moduleId);

    int deleteSysModuleByModuleIds(@Param("moduleIds") Long[] moduleIds);
}
