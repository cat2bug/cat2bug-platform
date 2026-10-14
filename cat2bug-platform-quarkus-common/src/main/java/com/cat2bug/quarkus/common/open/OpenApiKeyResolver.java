package com.cat2bug.quarkus.common.open;

import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysProjectApi;

public interface OpenApiKeyResolver {

    LoginUser resolveLoginUser(String apiKey);

    SysProjectApi findProjectApi(String apiKey);
}
