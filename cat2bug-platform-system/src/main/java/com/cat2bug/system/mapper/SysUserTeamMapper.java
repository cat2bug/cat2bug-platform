package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysUserTeam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserTeamMapper {

    List<SysUserTeam> selectSysUserTeamList(SysUserTeam sysUserTeam);

    int insertSysUserTeam(SysUserTeam sysUserTeam);

    int updateSysUserTeamByTeamIdAndMemberId(SysUserTeam sysUserTeam);
}
