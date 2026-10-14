package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysAiModuleConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysAiModuleConfigMapper {

    SysAiModuleConfig selectSysAiModuleConfigByAiId(@Param("aiId") Long aiId);

    SysAiModuleConfig selectSysAiModuleConfigByProjectId(@Param("projectId") Long projectId);

    List<SysAiModuleConfig> selectSysAiModuleConfigList(SysAiModuleConfig sysAiModuleConfig);

    int insertSysAiModuleConfig(SysAiModuleConfig sysAiModuleConfig);

    int updateSysAiModuleConfig(SysAiModuleConfig sysAiModuleConfig);

    int deleteSysAiModuleConfigByAiId(@Param("aiId") Long aiId);

    int deleteSysAiModuleConfigByAiIds(@Param("array") Long[] aiIds);
}
