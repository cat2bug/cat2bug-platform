package com.cat2bug.system.open;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysProjectApiMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OpenApiContextService {

    @Inject
    AuthContext authContext;

    @Inject
    SysProjectApiMapper sysProjectApiMapper;

    public Long requireProjectId() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null || StringUtils.isEmpty(loginUser.getToken())) {
            throw new ApiException(401, "未授权");
        }
        Long projectId = sysProjectApiMapper.selectProjectIdByApiId(loginUser.getToken());
        if (projectId == null) {
            throw new ApiException(401, "无效的 API KEY");
        }
        return projectId;
    }
}
