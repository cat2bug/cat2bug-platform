package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysTeam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysTeamMapper {

    long countSysTeamList(@Param("teamName") String teamName);

    List<SysTeam> selectSysTeamList(@Param("teamName") String teamName,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    long countSysTeamListByUserId(@Param("userId") Long userId);

    List<SysTeam> selectSysTeamListByUserId(@Param("userId") Long userId,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    SysTeam selectSysTeamByTeamId(@Param("teamId") Long teamId);

    SysTeam selectSysTeamByTeamName(@Param("teamName") String teamName);

    SysTeam selectSysTeamByTeamKey(@Param("teamKey") String teamKey);

    int insertSysTeam(SysTeam sysTeam);

    int updateSysTeam(SysTeam sysTeam);

    int deleteSysTeamByTeamIds(@Param("teamIds") Long[] teamIds);

    int insertSysUserTeam(@Param("userId") Long userId,
                          @Param("teamId") Long teamId,
                          @Param("createBy") String createBy);

    Long selectLastUserTeamId(@Param("userId") Long userId, @Param("teamId") Long teamId);

    int insertSysUserTeamRole(@Param("userTeamId") Long userTeamId, @Param("roleId") Long roleId);
}
