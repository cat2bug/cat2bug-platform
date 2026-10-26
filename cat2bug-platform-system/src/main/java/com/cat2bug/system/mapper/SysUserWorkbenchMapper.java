package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysUserWorkbench;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserWorkbenchMapper {

    SysUserWorkbench selectByUserAndProject(@Param("userId") Long userId, @Param("projectId") Long projectId);

    int insert(SysUserWorkbench row);

    int updateConfig(SysUserWorkbench row);
}
