package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.constant.UserConstants;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysTeam;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.common.domain.SysUserConfig;
import com.cat2bug.quarkus.common.domain.SysUserTeam;
import com.cat2bug.quarkus.common.domain.SysUserTeamRole;
import com.cat2bug.quarkus.common.domain.vo.BatchUserRoleVo;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.util.MessageUtils;
import com.cat2bug.quarkus.common.util.PathKeyValidator;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.system.mapper.SysTeamMapper;
import com.cat2bug.quarkus.system.mapper.SysUserConfigMapper;
import com.cat2bug.quarkus.system.mapper.SysUserMapper;
import com.cat2bug.quarkus.system.mapper.SysUserTeamMapper;
import com.cat2bug.quarkus.system.mapper.SysUserTeamRoleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class SysTeamService {

    private static final Long TEAM_CREATE_BY_ROLE_ID = 12L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Inject
    SysTeamMapper sysTeamMapper;

    @Inject
    SysUserMapper sysUserMapper;

    @Inject
    SysUserTeamMapper sysUserTeamMapper;

    @Inject
    SysUserTeamRoleMapper sysUserTeamRoleMapper;

    @Inject
    SysUserConfigMapper sysUserConfigMapper;

    @Inject
    SysUserService sysUserService;

    public TableDataInfo selectTeamPage(String teamName, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysTeamMapper.countSysTeamList(teamName);
        List<SysTeam> rows = sysTeamMapper.selectSysTeamList(teamName, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public TableDataInfo selectMyTeams(Long userId, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysTeamMapper.countSysTeamListByUserId(userId);
        List<SysTeam> rows = sysTeamMapper.selectSysTeamListByUserId(userId, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysTeam> selectTeamListAll(String teamName) {
        return sysTeamMapper.selectSysTeamList(teamName, 0, Integer.MAX_VALUE);
    }

    public SysTeam selectSysTeamByTeamId(Long teamId) {
        return sysTeamMapper.selectSysTeamByTeamId(teamId);
    }

    public TableDataInfo selectTeamMemberPage(Long teamId, String search, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysUserMapper.countSysUserListByTeamId(teamId, search);
        List<SysUser> rows = MemberViewHelper.withRoleIds(
                sysUserMapper.selectSysUserListByTeamId(teamId, search, page.getOffset(), page.getPageSize()));
        return new TableDataInfo(rows, total);
    }

    public TableDataInfo selectTeamNotMemberPage(Long teamId, String search, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysUserMapper.countSysUserListByTeamIdNotMember(teamId, search);
        List<SysUser> rows = MemberViewHelper.withRoleIds(
                sysUserMapper.selectSysUserListByTeamIdNotMember(teamId, search, page.getOffset(), page.getPageSize()));
        return new TableDataInfo(rows, total);
    }

    @Transactional
    public SysTeam insertSysTeam(SysTeam sysTeam, Long userId, String username) {
        if (sysTeam.getTeamName() == null || sysTeam.getTeamName().isBlank()) {
            throw new ApiException("团队名称不能为空");
        }
        if (sysTeamMapper.selectSysTeamByTeamName(sysTeam.getTeamName()) != null) {
            throw new ApiException("团队名称已存在");
        }
        String teamKey = PathKeyValidator.normalizeOrNull(sysTeam.getTeamKey());
        if (teamKey == null) {
            throw new ApiException(MessageUtils.message("team.path-key-invalid"));
        }
        if (sysTeamMapper.selectSysTeamByTeamKey(teamKey) != null) {
            throw new ApiException(MessageUtils.message("team.path-key-duplicate"));
        }
        sysTeam.setTeamKey(teamKey);
        String now = LocalDateTime.now().format(TIME_FMT);
        sysTeam.setCreateBy(username);
        sysTeam.setCreateTime(now);
        if (sysTeamMapper.insertSysTeam(sysTeam) != 1) {
            throw new ApiException("创建团队失败");
        }
        sysTeamMapper.insertSysUserTeam(userId, sysTeam.getTeamId(), String.valueOf(userId));
        Long userTeamId = sysTeamMapper.selectLastUserTeamId(userId, sysTeam.getTeamId());
        if (userTeamId != null) {
            sysTeamMapper.insertSysUserTeamRole(userTeamId, TEAM_CREATE_BY_ROLE_ID);
        }
        return sysTeam;
    }

    public int updateSysTeam(SysTeam sysTeam) {
        sysTeam.setUpdateTime(LocalDateTime.now().format(TIME_FMT));
        return sysTeamMapper.updateSysTeam(sysTeam);
    }

    @Transactional
    public int inviteMember(BatchUserRoleVo batchUserRoleVo, String createBy) {
        if (batchUserRoleVo.getTeamId() == null) {
            throw new ApiException("团队不能为空");
        }
        if (batchUserRoleVo.getMemberIds() == null || batchUserRoleVo.getMemberIds().length == 0) {
            throw new ApiException("成员不能为空");
        }
        if (batchUserRoleVo.getRoleIds() == null || batchUserRoleVo.getRoleIds().length == 0) {
            throw new ApiException("角色不能为空");
        }
        String now = LocalDateTime.now().format(TIME_FMT);
        for (Long memberId : batchUserRoleVo.getMemberIds()) {
            SysUserTeam sysUserTeam = new SysUserTeam();
            sysUserTeam.setUserId(memberId);
            sysUserTeam.setTeamId(batchUserRoleVo.getTeamId());
            sysUserTeam.setCreateTime(now);
            sysUserTeam.setCreateBy(createBy);
            if (sysUserTeamMapper.insertSysUserTeam(sysUserTeam) != 1) {
                throw new ApiException("添加团队成员失败");
            }
            for (Long roleId : batchUserRoleVo.getRoleIds()) {
                SysUserTeamRole utr = new SysUserTeamRole();
                utr.setUserTeamId(sysUserTeam.getUserTeamId());
                utr.setRoleId(roleId);
                if (sysUserTeamRoleMapper.insertSysUserTeamRole(utr) != 1) {
                    throw new ApiException("添加成员角色失败");
                }
            }
        }
        return batchUserRoleVo.getMemberIds().length;
    }

    @Transactional
    public int insertSysUser(Long teamId, SysUser user) {
        user.setPhoneNumber(StringUtils.trimToNull(user.getPhoneNumber()));
        user.setEmail(StringUtils.trimToNull(user.getEmail()));
        if (!sysUserService.checkUserNameUnique(user)) {
            throw new ApiException("创建用户'" + user.getNickName() + "'失败，登录账号已存在");
        }
        if (StringUtils.isNotEmpty(user.getPhoneNumber()) && !sysUserService.checkPhoneUnique(user)) {
            throw new ApiException("创建用户'" + user.getNickName() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !sysUserService.checkEmailUnique(user)) {
            throw new ApiException("创建用户'" + user.getNickName() + "'失败，邮箱账号已存在");
        }
        if (StringUtils.isEmpty(user.getUserType())) {
            user.setUserType(UserConstants.USER_TYPE_SYSTEM);
        }
        if (sysUserMapper.insertUser(user) != 1) {
            throw new ApiException("创建用户失败");
        }

        String now = LocalDateTime.now().format(TIME_FMT);
        SysUserTeam sysUserTeam = new SysUserTeam();
        sysUserTeam.setUserId(user.getUserId());
        sysUserTeam.setTeamId(teamId);
        sysUserTeam.setCreateTime(now);
        sysUserTeam.setCreateBy(user.getCreateBy());
        if (sysUserTeamMapper.insertSysUserTeam(sysUserTeam) != 1) {
            throw new ApiException("添加团队成员失败");
        }

        if (user.getRoleIds() != null) {
            for (Long roleId : user.getRoleIds()) {
                SysUserTeamRole utr = new SysUserTeamRole();
                utr.setUserTeamId(sysUserTeam.getUserTeamId());
                utr.setRoleId(roleId);
                if (sysUserTeamRoleMapper.insertSysUserTeamRole(utr) != 1) {
                    throw new ApiException("添加成员角色失败");
                }
            }
        }

        SysUserConfig sysUserConfig = new SysUserConfig();
        sysUserConfig.setUserId(user.getUserId());
        sysUserConfig.setCurrentTeamId(teamId);
        if (sysUserConfigMapper.insertSysUserConfig(sysUserConfig) != 1) {
            throw new ApiException("创建用户配置失败");
        }
        return 1;
    }

    public int deleteSysTeamByTeamIds(Long[] teamIds) {
        return sysTeamMapper.deleteSysTeamByTeamIds(teamIds);
    }
}
