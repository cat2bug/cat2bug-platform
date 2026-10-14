package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysRoleMapper {

    List<SysRole> selectRoleList(@Param("role") SysRole role,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    long countRoleList(@Param("role") SysRole role);

    SysRole selectRoleById(Long roleId);

    List<SysRole> selectRoleAll();

    List<SysRole> selectRolePermissionByUserId(@Param("teamId") Long teamId,
                                               @Param("projectId") Long projectId,
                                               @Param("userId") Long userId);

    SysRole checkRoleNameUnique(String roleName);

    SysRole checkRoleKeyUnique(String roleKey);

    int insertRole(SysRole role);

    int updateRole(SysRole role);

    int deleteRoleById(Long roleId);

    int deleteRoleByIds(Long[] roleIds);
}
