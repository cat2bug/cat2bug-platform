package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysUserTeam;
import com.cat2bug.system.mapper.SysUserTeamMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class SysUserTeamService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Inject
    SysUserTeamMapper sysUserTeamMapper;

    public int updateSysUserTeamByTeamIdAndMemberId(Long teamId, Long memberId, SysUserTeam sysUserTeam) {
        sysUserTeam.setTeamId(teamId);
        sysUserTeam.setUserId(memberId);
        sysUserTeam.setUpdateTime(LocalDateTime.now().format(TIME_FMT));
        return sysUserTeamMapper.updateSysUserTeamByTeamIdAndMemberId(sysUserTeam);
    }
}
