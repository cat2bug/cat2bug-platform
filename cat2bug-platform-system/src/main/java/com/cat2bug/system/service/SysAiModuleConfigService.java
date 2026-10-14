package com.cat2bug.system.service;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysAiModuleConfig;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysAiModuleConfigMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;

@ApplicationScoped
public class SysAiModuleConfigService {

    @Inject
    SysAiModuleConfigMapper sysAiModuleConfigMapper;

    @Inject
    AuthContext authContext;

    public SysAiModuleConfig selectSysAiModuleConfigByAiId(Long aiId) {
        return sysAiModuleConfigMapper.selectSysAiModuleConfigByAiId(aiId);
    }

    public SysAiModuleConfig selectSysAiModuleConfigByProjectId(Long projectId) {
        return sysAiModuleConfigMapper.selectSysAiModuleConfigByProjectId(projectId);
    }

    public List<SysAiModuleConfig> selectSysAiModuleConfigList(SysAiModuleConfig sysAiModuleConfig) {
        return sysAiModuleConfigMapper.selectSysAiModuleConfigList(sysAiModuleConfig);
    }

    @Transactional
    public int insertSysAiModuleConfig(SysAiModuleConfig sysAiModuleConfig) {
        sysAiModuleConfig.setCreateTime(new Date());
        return sysAiModuleConfigMapper.insertSysAiModuleConfig(sysAiModuleConfig);
    }

    @Transactional
    public int updateSysAiModuleConfig(SysAiModuleConfig sysAiModuleConfig) {
        SysAiModuleConfig existing = sysAiModuleConfigMapper.selectSysAiModuleConfigByProjectId(
                sysAiModuleConfig.getProjectId());
        if (existing == null) {
            LoginUser loginUser = requireLoginUser();
            Date now = new Date();
            sysAiModuleConfig.setCreateTime(now);
            sysAiModuleConfig.setCreateById(loginUser.getUserId());
            sysAiModuleConfig.setUpdateTime(now);
            sysAiModuleConfig.setUpdateById(loginUser.getUserId());
            return sysAiModuleConfigMapper.insertSysAiModuleConfig(sysAiModuleConfig);
        }
        LoginUser loginUser = requireLoginUser();
        existing.setUpdateById(loginUser.getUserId());
        existing.setUpdateTime(new Date());
        return sysAiModuleConfigMapper.updateSysAiModuleConfig(existing);
    }

    @Transactional
    public int deleteSysAiModuleConfigByAiIds(Long[] aiIds) {
        return sysAiModuleConfigMapper.deleteSysAiModuleConfigByAiIds(aiIds);
    }

    @Transactional
    public int deleteSysAiModuleConfigByAiId(Long aiId) {
        return sysAiModuleConfigMapper.deleteSysAiModuleConfigByAiId(aiId);
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
