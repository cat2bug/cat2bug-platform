package com.cat2bug.system.util;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.service.ISysUserProjectService;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 缺陷列表「合并关键字」查询：按关键字解析项目内成员 ID，供 Mapper 匹配 handle_by；
 * 名称、编号（project_num）、版本、交付物名称另由 Mapper 模糊匹配；成员解析仅匹配昵称。
 */
public final class DefectListKeywordSupport {

    private DefectListKeywordSupport() {
    }

    /**
     * 当存在 {@link SysDefect#getNameVersionKeyword()} 或 {@code params.nameVersionKeyword} 时，
     * 将项目内昵称/用户名模糊匹配到的用户 ID 写入 {@code params.nameVersionKeywordHandleBy}。
     */
    public static void fillNameVersionKeywordHandleBy(SysDefect sysDefect, ISysUserProjectService sysUserProjectService) {
        if (sysDefect == null || sysDefect.getProjectId() == null || sysUserProjectService == null) {
            return;
        }
        String kw = sysDefect.getNameVersionKeyword();
        if (StringUtils.isEmpty(kw) && sysDefect.getParams() != null) {
            Object o = sysDefect.getParams().get("nameVersionKeyword");
            if (o != null) {
                kw = o.toString();
            }
        }
        if (StringUtils.isEmpty(kw)) {
            return;
        }
        kw = kw.trim();
        if (sysDefect.getParams() == null) {
            sysDefect.setParams(new HashMap<>());
        }
        SysUser sysUser = new SysUser();
        sysUser.setParams(new HashMap<>());
        sysUser.getParams().put("search", kw);
        List<SysUser> userList = sysUserProjectService.selectSysUserListByProjectId(sysDefect.getProjectId(), sysUser);
        sysDefect.getParams().put("nameVersionKeywordHandleBy",
                userList.stream().map(SysUser::getUserId).collect(Collectors.toList()));
    }
}
