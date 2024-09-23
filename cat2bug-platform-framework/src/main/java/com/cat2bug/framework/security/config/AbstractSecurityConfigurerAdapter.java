package com.cat2bug.framework.security.config;

import org.springframework.security.config.annotation.SecurityConfigurerAdapter;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-09-23 18:32
 * @Version: 1.0.0
 */
public abstract class AbstractSecurityConfigurerAdapter extends SecurityConfigurerAdapter {
    /**
     * 获取匹配网址
     * @return
     */
    public abstract String[] getMatchers();
}
