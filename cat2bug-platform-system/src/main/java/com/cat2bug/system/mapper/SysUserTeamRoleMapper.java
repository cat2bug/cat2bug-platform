package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysUserTeamRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserTeamRoleMapper {

    int insertSysUserTeamRole(SysUserTeamRole sysUserTeamRole);

    int deleteSysUserTeamByUserTeamId(Long userTeamId);

    int deleteByUserTeamIdAndRoleId(@Param("userTeamId") Long userTeamId, @Param("roleId") Long roleId);

    List<Long> selectRoleIdsByUserTeamId(@Param("userTeamId") Long userTeamId);

    int countByTeamUserAndRole(@Param("teamId") Long teamId,
                               @Param("userId") Long userId,
                               @Param("roleId") Long roleId);
}
