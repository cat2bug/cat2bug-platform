package com.cat2bug.common.open;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysProjectApi;

public interface OpenApiKeyResolver {

    LoginUser resolveLoginUser(String apiKey);

    SysProjectApi findProjectApi(String apiKey);
}
