package com.cat2bug.common.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.websocket.server.ServerEndpointConfig;


/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-23 15:07
 * @Version: 1.0.0
 */
public class WebSocketSpringConfigurator  extends ServerEndpointConfig.Configurator implements ApplicationContextAware {
    private static volatile BeanFactory context;
    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        return context.getBean(clazz);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        WebSocketSpringConfigurator.context = applicationContext;
    }
}
