package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysUserWorkbench;
import com.cat2bug.system.mapper.SysUserWorkbenchMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Date;

@ApplicationScoped
public class SysUserWorkbenchService {

    @Inject
    SysUserWorkbenchMapper sysUserWorkbenchMapper;

    public SysUserWorkbench selectByUserAndProject(Long userId, Long projectId) {
        return sysUserWorkbenchMapper.selectByUserAndProject(userId, projectId);
    }

    public SysUserWorkbench saveConfig(Long userId, Long projectId, String configJson) {
        Date now = new Date();
        SysUserWorkbench existing = sysUserWorkbenchMapper.selectByUserAndProject(userId, projectId);
        if (existing == null) {
            SysUserWorkbench row = new SysUserWorkbench();
            row.setUserId(userId);
            row.setProjectId(projectId);
            row.setConfigJson(configJson);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            sysUserWorkbenchMapper.insert(row);
            return row;
        }
        existing.setConfigJson(configJson);
        existing.setUpdateTime(now);
        sysUserWorkbenchMapper.updateConfig(existing);
        return existing;
    }
}
