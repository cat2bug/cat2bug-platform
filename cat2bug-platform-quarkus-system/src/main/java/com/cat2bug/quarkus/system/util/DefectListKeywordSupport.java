package com.cat2bug.quarkus.system.util;

import com.cat2bug.quarkus.common.domain.SysDefect;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.system.mapper.SysUserProjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 缺陷列表合并关键字：按昵称解析项目成员 ID，供 Mapper 匹配 handle_by。
 */
@ApplicationScoped
public class DefectListKeywordSupport {

    private static final int MEMBER_SEARCH_LIMIT = 500;

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

    public void fillNameVersionKeywordHandleBy(SysDefect sysDefect) {
        if (sysDefect == null || sysDefect.getProjectId() == null) {
            return;
        }
        String kw = sysDefect.getNameVersionKeyword();
        if (isBlank(kw) && sysDefect.getParams() != null) {
            Object value = sysDefect.getParams().get("nameVersionKeyword");
            if (value != null) {
                kw = value.toString();
            }
        }
        if (isBlank(kw)) {
            return;
        }
        kw = kw.trim();
        if (sysDefect.getParams() == null) {
            sysDefect.setParams(new HashMap<>());
        }
        List<SysUser> userList = sysUserProjectMapper.selectSysUserListByProjectId(
                sysDefect.getProjectId(), kw, 0, MEMBER_SEARCH_LIMIT);
        sysDefect.getParams().put("nameVersionKeywordHandleBy",
                userList.stream().map(SysUser::getUserId).distinct().collect(Collectors.toList()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
