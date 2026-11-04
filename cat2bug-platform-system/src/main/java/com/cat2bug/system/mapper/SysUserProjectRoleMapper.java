package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysUserProjectRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserProjectRoleMapper {

    int insertSysUserProjectRole(SysUserProjectRole sysUserProjectRole);

    int deleteSysUserProjectRoleByUserProjectId(Long userProjectId);

    int deleteByUserProjectIdAndRoleId(@Param("userProjectId") Long userProjectId, @Param("roleId") Long roleId);

    List<Long> selectRoleIdsByUserProjectId(@Param("userProjectId") Long userProjectId);

    int countByProjectUserAndRole(@Param("projectId") Long projectId,
                                  @Param("userId") Long userId,
                                  @Param("roleId") Long roleId);
}
