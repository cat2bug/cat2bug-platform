package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysMemberInvite;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 成员邀请链接 Mapper
 */
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
