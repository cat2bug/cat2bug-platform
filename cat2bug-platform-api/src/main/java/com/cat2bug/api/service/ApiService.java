package com.cat2bug.api.service;

import com.cat2bug.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-10 23:59
 * @Version: 1.0.0
 */
@Service
public class ApiService {
    public Long getProjectId() {
        String token = SecurityUtils.getLoginUser().getToken();
        return 74L;
    }
}
