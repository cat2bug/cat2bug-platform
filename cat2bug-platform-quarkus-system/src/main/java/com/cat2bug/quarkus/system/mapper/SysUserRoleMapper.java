package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserRoleMapper {

    int countUserRoleByRoleId(Long roleId);

    int deleteUserRoleInfo(SysUserRole userRole);

    int deleteUserRoleInfos(@Param("roleId") Long roleId, @Param("userIds") Long[] userIds);

    int batchUserRole(@Param("list") List<SysUserRole> list);
}
