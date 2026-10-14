package com.cat2bug.quarkus.admin.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.system.mapper.SysUserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthService {

    @Inject
    SysUserMapper sysUserMapper;

    public SysUser authenticate(String username, String password) {
        SysUser user = findUndeletedUser(username);
        if (user == null || !"0".equals(user.getStatus())) {
            return null;
        }
        return passwordMatches(user, password) ? user : null;
    }

    public SysUser findUndeletedUser(String username) {
        return username == null ? null : sysUserMapper.selectUserByUserName(username);
    }

    public boolean passwordMatches(SysUser user, String password) {
        if (user == null || password == null || user.getPassword() == null) {
            return false;
        }
        return BCrypt.verifyer().verify(password.toCharArray(), user.getPassword()).verified;
    }
}
