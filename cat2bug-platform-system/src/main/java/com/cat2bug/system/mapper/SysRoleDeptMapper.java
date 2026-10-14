package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysRoleDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysRoleDeptMapper {

    int deleteRoleDeptByRoleId(Long roleId);

    int deleteRoleDept(Long[] roleIds);

    int batchRoleDept(@Param("list") List<SysRoleDept> list);
}
