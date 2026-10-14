package com.cat2bug.quarkus.system.open;

import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysProjectApi;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.common.open.OpenApiKeyResolver;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.system.mapper.SysProjectApiMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;

@ApplicationScoped
public class OpenApiKeyResolverImpl implements OpenApiKeyResolver {

    private static final Set<String> DEFAULT_PERMISSIONS = Set.of(
            "api:defect:add",
            "api:defect:edit",
            "api:project:push",
            "api:case:list",
            "api:module:list",
            "api:defect:list",
            "api:defect:query",
            "api:report:push"
    );

    @Inject
    SysProjectApiMapper sysProjectApiMapper;

    @Override
    public LoginUser resolveLoginUser(String apiKey) {
        SysProjectApi projectApi = findProjectApi(apiKey);
        if (projectApi == null) {
            return null;
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(projectApi.getUserId());
        loginUser.setToken(apiKey.trim());
        if (projectApi.getExpireTime() != null) {
            loginUser.setExpireTime(projectApi.getExpireTime().getTime());
        }
        loginUser.setPermissions(DEFAULT_PERMISSIONS);
        SysUser user = new SysUser();
        user.setUserId(projectApi.getUserId());
        loginUser.setUser(user);
        return loginUser;
    }

    @Override
    public SysProjectApi findProjectApi(String apiKey) {
        if (StringUtils.isEmpty(apiKey)) {
            return null;
        }
        return sysProjectApiMapper.selectSysProjectApiByApiId(apiKey.trim());
    }
}
