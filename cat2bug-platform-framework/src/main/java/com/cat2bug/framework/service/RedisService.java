package com.cat2bug.framework.service;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-20 07:29
 * @Version: 1.0.0
 */
@Service
public class RedisService {

//    @Bean
//    public RedisServer mockRedisServer(RedisProperties redisProperties) throws IOException {
//        RedisServer server = new RedisServer();
//        server.listener(redisProperties.getHost(), redisProperties.getPort());
//        InetSocketAddress address = (InetSocketAddress) server.getServerSocket().getLocalSocketAddress();
//        logger.info("Mocker Redis start :: [{}:{}], set 'server.redis.host' to match it", address.getHostName(), address.getPort());
//        return server;
//    }
}
