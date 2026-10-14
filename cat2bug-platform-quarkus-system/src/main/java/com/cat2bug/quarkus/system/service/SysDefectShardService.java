package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysDefect;
import com.cat2bug.quarkus.common.domain.SysDefectLog;
import com.cat2bug.quarkus.common.domain.SysDefectShard;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.common.util.Md5Utils;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.framework.security.AuthContext;
import com.cat2bug.quarkus.system.mapper.SysDefectShardMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class SysDefectShardService {

    private static final String MSG_NOT_FOUND = "没有找到分享的缺陷!";
    private static final String MSG_EXPIRE = "分享已过期!";
    private static final String MSG_INPUT_PASSWORD = "请输入分享密码!";
    private static final String MSG_PASSWORD_ERROR = "分享密码错误!";

    @Inject
    SysDefectShardMapper sysDefectShardMapper;

    @Inject
    AuthContext authContext;

    @Inject
    SysDefectService sysDefectService;

    @Inject
    SysDefectLogService sysDefectLogService;

    @Inject
    SysUserProjectService sysUserProjectService;

    @Inject
    SysDictDataService sysDictDataService;

    public SysDefectShard selectSysDefectShardByDefectShardId(String defectShardId) {
        return sysDefectShardMapper.selectSysDefectShardByDefectShardId(defectShardId);
    }

    public SysDefectShard selectSysDefectShardByDefectIdAndMemberId(Long defectId, Long memberId) {
        return sysDefectShardMapper.selectSysDefectShardByDefectIdAndMemberId(defectId, memberId);
    }

    public List<SysDefectShard> selectSysDefectShardList(SysDefectShard sysDefectShard) {
        return sysDefectShardMapper.selectSysDefectShardList(sysDefectShard);
    }

    @Transactional
    public SysDefectShard insertSysDefectShard(SysDefectShard sysDefectShard) {
        LoginUser loginUser = requireLoginUser();
        Date now = new Date();
        sysDefectShard.setCreateById(loginUser.getUserId());
        sysDefectShard.setUpdateTime(now);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        if (sysDefectShard.getAgingHour() != null) {
            calendar.add(Calendar.HOUR, sysDefectShard.getAgingHour());
        }
        sysDefectShard.setAgingTime(calendar.getTime());

        SysDefectShard existing = sysDefectShardMapper.selectSysDefectShardByDefectIdAndMemberId(
                sysDefectShard.getDefectId(), loginUser.getUserId());
        int count;
        if (existing == null) {
            sysDefectShard.setCreateTime(now);
            sysDefectShard.setDefectShardId(createShardId(sysDefectShard));
            count = sysDefectShardMapper.insertSysDefectShard(sysDefectShard);
        } else {
            sysDefectShard.setDefectShardId(existing.getDefectShardId());
            count = sysDefectShardMapper.updateSysDefectShard(sysDefectShard);
        }
        return count == 1 ? sysDefectShard : null;
    }

    public int updateSysDefectShard(SysDefectShard sysDefectShard) {
        sysDefectShard.setUpdateTime(new Date());
        return sysDefectShardMapper.updateSysDefectShard(sysDefectShard);
    }

    public int deleteSysDefectShardByDefectShardIds(String[] defectShardIds) {
        return sysDefectShardMapper.deleteSysDefectShardByDefectShardIds(defectShardIds);
    }

    public int deleteSysDefectShardByDefectShardId(String defectShardId) {
        return sysDefectShardMapper.deleteSysDefectShardByDefectShardId(defectShardId);
    }

    public Map<String, Object> buildSharedDefectView(String defectShardId, String password, Long memberId) {
        SysDefectShard shard = selectSysDefectShardByDefectShardId(defectShardId);
        if (shard == null) {
            return errorPayload(MSG_NOT_FOUND);
        }
        if (shard.getAgingHour() != null
                && shard.getAgingHour() > 0
                && shard.getAgingTime() != null
                && shard.getAgingTime().getTime() > 0
                && shard.getAgingTime().getTime() < System.currentTimeMillis()) {
            return errorPayload(MSG_EXPIRE);
        }

        SysDefect sysDefect = sysDefectService.selectSysDefectByDefectIdOptional(shard.getDefectId());
        if (sysDefect == null) {
            return errorPayload(MSG_NOT_FOUND);
        }

        SysDefectLog query = new SysDefectLog();
        query.setDefectId(sysDefect.getDefectId());
        List<SysDefectLog> logList = sysDefectLogService.selectSysDefectLogList(query);

        Map<String, Object> ret = new HashMap<>();
        ret.put("logs", logList);
        ret.put("defect", sysDefect);
        ret.put("defectLevel", sysDictDataService.selectDictDataByType("defect_level"));
        if (memberId != null) {
            Map<String, Object> redirect = new HashMap<>();
            redirect.put("projectId", sysDefect.getProjectId());
            redirect.put("defectId", sysDefect.getDefectId());
            ret.put("redirect", redirect);
        }

        if (StringUtils.isEmpty(shard.getPassword())) {
            return successPayload(ret);
        }

        if (memberId != null) {
            List<SysUser> users = sysUserProjectService.selectSysUserListByProjectId(shard.getProjectId());
            if (users != null && users.stream().anyMatch(u -> memberId.equals(u.getUserId()))) {
                return successPayload(ret);
            }
        }

        if (StringUtils.isNotEmpty(shard.getPassword()) && StringUtils.isEmpty(password)) {
            return errorPayload(MSG_INPUT_PASSWORD);
        }

        if (StringUtils.isNotEmpty(shard.getPassword()) && !shard.getPassword().equals(password)) {
            return errorPayload(MSG_PASSWORD_ERROR);
        }

        return successPayload(ret);
    }

    private String createShardId(SysDefectShard sysDefectShard) {
        String id = sysDefectShard.getDefectId() + UUID.randomUUID().toString();
        return Md5Utils.hash(id);
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new com.cat2bug.quarkus.common.exception.ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }

    private Map<String, Object> successPayload(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> errorPayload(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
