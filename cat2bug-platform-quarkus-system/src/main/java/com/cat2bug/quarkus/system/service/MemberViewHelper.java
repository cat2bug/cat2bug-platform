package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.SysRole;
import com.cat2bug.quarkus.common.domain.SysUser;

import java.util.List;
import java.util.stream.Collectors;

final class MemberViewHelper {

    private MemberViewHelper() {
    }

    static List<SysUser> withRoleIds(List<SysUser> users) {
        return users.stream().map(MemberViewHelper::withRoleIds).collect(Collectors.toList());
    }

    static SysUser withRoleIds(SysUser user) {
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            user.setRoleIds(user.getRoles().stream().map(SysRole::getRoleId).toArray(Long[]::new));
        }
        return user;
    }
}
