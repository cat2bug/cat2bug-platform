package com.cat2bug.system.service;

import com.cat2bug.common.core.domain.entity.SysUser;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-21 23:56
 * @Version: 1.0.0
 * 成员焦点服务
 */
public interface IMemberFocusService {
    public void setFocus(String moduleName, Long dataId, SysUser user);

    public void removeFocus(Long user);

    public List<SysUser> getFocusMemberList(String moduleName, Long dataId);
}
