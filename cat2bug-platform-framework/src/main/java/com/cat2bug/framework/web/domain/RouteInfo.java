package com.cat2bug.framework.web.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-12-16 19:59
 * @Version: 1.0.0
 */
@Data
public class RouteInfo {
    /**
     * 路由前缀
     */
    private String prefix;
    /**
     * 主机 http://开头
     */
    private String host;
}
