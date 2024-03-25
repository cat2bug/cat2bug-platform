package com.cat2bug.framework.config;

import com.cat2bug.common.config.WebSocketSpringConfigurator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-21 14:44
 * @Version: 1.0.0
 */
@Configuration
@ConditionalOnWebApplication
@EnableWebSocket
public class WebSocketConfig {
    @Bean
    public WebSocketSpringConfigurator webSocketSpringConfigurator() {
        return new WebSocketSpringConfigurator();
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
