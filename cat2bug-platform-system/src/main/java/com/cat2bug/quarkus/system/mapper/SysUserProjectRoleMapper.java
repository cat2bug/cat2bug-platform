package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysUserProjectRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserProjectRoleMapper {

    int insertSysUserProjectRole(SysUserProjectRole sysUserProjectRole);

    int deleteSysUserProjectRoleByUserProjectId(Long userProjectId);
}
