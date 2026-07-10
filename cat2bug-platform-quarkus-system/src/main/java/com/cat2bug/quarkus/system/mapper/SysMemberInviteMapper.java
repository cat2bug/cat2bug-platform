package com.cat2bug.quarkus.system.mapper;

import com.cat2bug.quarkus.common.domain.SysMemberInvite;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysMemberInviteMapper {

    SysMemberInvite selectSysMemberInviteByInviteId(Long inviteId);

    SysMemberInvite selectSysMemberInviteByToken(String token);

    List<SysMemberInvite> selectActiveSysMemberInviteListByTeamId(@Param("teamId") Long teamId,
                                                                  @Param("now") Date now);

    List<SysMemberInvite> selectActiveSysMemberInviteListByProjectId(@Param("projectId") Long projectId,
                                                                       @Param("now") Date now);

    int insertSysMemberInvite(SysMemberInvite invite);

    int revokeSysMemberInvite(@Param("inviteId") Long inviteId,
                              @Param("teamId") Long teamId,
                              @Param("projectId") Long projectId);
}
