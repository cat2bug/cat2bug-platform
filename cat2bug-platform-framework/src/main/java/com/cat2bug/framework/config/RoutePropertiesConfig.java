package com.cat2bug.framework.config;

import com.cat2bug.framework.web.domain.RouteInfo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-12-16 20:00
 * @Version: 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cat2bug.proxy")
public class RoutePropertiesConfig {
    //map<服务，路由>
    private Map<String, RouteInfo> routes;

    /**
     * 获取路由
     *
     * @param prefix
     * @return
     */
    public RouteInfo getRouteByPrefix(String prefix) {
        return routes.entrySet().stream()
            .map(Map.Entry::getValue).filter(r -> r.getPrefix().equals(prefix))
            .findFirst().orElse(null);
    }
}
