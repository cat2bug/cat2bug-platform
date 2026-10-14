package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysUserDefect;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.system.mapper.SysUserDefectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class SysUserDefectService {

    @Inject
    SysUserDefectMapper sysUserDefectMapper;

    public int updateSysUserDefect(SysUserDefect sysUserDefect) {
        if (sysUserDefect.getDefectId() == null) {
            throw new ApiException("缺陷ID不能为空");
        }
        if (sysUserDefect.getUserId() == null) {
            throw new ApiException("用户ID不能为空");
        }
        SysUserDefect query = new SysUserDefect();
        query.setDefectId(sysUserDefect.getDefectId());
        query.setUserId(sysUserDefect.getUserId());
        List<SysUserDefect> list = sysUserDefectMapper.selectSysUserDefectList(query);
        if (list == null || list.isEmpty()) {
            return sysUserDefectMapper.insertSysUserDefect(sysUserDefect);
        }
        return sysUserDefectMapper.updateSysUserDefect(sysUserDefect);
    }
}
