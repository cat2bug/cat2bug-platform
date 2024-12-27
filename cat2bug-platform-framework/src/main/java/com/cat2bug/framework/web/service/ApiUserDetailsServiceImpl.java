package com.cat2bug.framework.web.service;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.system.domain.SysProjectApi;
import com.cat2bug.system.mapper.SysProjectApiMapper;
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

    @Autowired
    SysProjectApiMapper sysProjectApiMapper;
    /**
     * API默认权限
     */
    private static final Set<String> DEFAULT_PERMISSIONS=Sets.newHashSet(
            "api:defect:add",
            "api:project:push",
            "api:case:list",
            "api:module:list",
            "api:defect:list",
            "api:defect:query",
            "api:report:push");

    @Override
    public UserDetails loadUserByUsername(String token) throws UsernameNotFoundException {
        LoginUser user = createLoginUser(token);
        if(user!=null) {
            tokenService.setLoginUser(user);
        }
        return user;
    }

    public LoginUser createLoginUser(String token)
    {
        SysProjectApi  sysProjectApi = sysProjectApiMapper.selectSysProjectApiByApiId(token);
        if(sysProjectApi!=null) {
            LoginUser user = new LoginUser();
            user.setUserId(sysProjectApi.getUserId());
            user.setToken(token);
            user.setExpireTime(sysProjectApi.getExpireTime()==null?0:sysProjectApi.getExpireTime().getTime());
            user.setPermissions(DEFAULT_PERMISSIONS);
            user.setUser(new SysUser());
            return user;
        }
        return null;
    }
}
