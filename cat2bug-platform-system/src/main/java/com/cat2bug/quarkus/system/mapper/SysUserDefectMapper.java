package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysUserDefect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserDefectMapper {

    List<SysUserDefect> selectSysUserDefectList(@Param("query") SysUserDefect query);

    int insertSysUserDefect(SysUserDefect sysUserDefect);

    int updateSysUserDefect(SysUserDefect sysUserDefect);
}
