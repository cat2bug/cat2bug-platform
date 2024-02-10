package com.cat2bug.framework.web.service;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-09 15:32
 * @Version: 1.0.0
 */
@Service
@ConditionalOnProperty(prefix = "cat2bug.api", name = "enabled", havingValue = "true")
public class ApiUserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    ApiTokenService tokenService;

    /**
     * API默认权限
     */
    private static final Set<String> DEFAULT_PERMISSIONS=Sets.newHashSet("api:defect:list");

    @Override
    public UserDetails loadUserByUsername(String token) throws UsernameNotFoundException {
        LoginUser user = createLoginUser(token);
        tokenService.setLoginUser(user);
        return user;
    }

    public LoginUser createLoginUser(String token)
    {
        if(token.equals("123456")) {
            LoginUser user = new LoginUser();
            user.setUserId(0L);
            user.setToken(token);
            user.setExpireTime(Long.MAX_VALUE);
            user.setPermissions(DEFAULT_PERMISSIONS);
            user.setUser(new SysUser());
            return user;
        }
        return null;
    }
}
