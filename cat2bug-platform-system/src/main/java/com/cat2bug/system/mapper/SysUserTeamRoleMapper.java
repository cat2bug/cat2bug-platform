package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysUserTeamRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserTeamRoleMapper {

    int insertSysUserTeamRole(SysUserTeamRole sysUserTeamRole);

    int deleteSysUserTeamByUserTeamId(Long userTeamId);
}
