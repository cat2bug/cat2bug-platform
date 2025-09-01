package com.cat2bug.ai.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;


/**
 * @Author: yuzhantao
 * @CreateTime: 2024-12-24 08:46
 * @Version: 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "cat2bug.proxy.routes.ai", name = "enabled", havingValue = "true")
public class RouterConfig {
    @Value("${cat2bug.proxy.routes.ai.host}")
    private String host;
    @Value("${cat2bug.proxy.routes.ai.enabled}")
    private boolean enabled;
    @Bean
    public WebClient webClient(){
        //配置固定大小连接池
        ConnectionProvider provider = ConnectionProvider
                .builder("custom")
                // 等待超时时间
                .pendingAcquireTimeout(Duration.ofSeconds(600))
                // 最大连接数
                .maxConnections(200)
                // 最大空闲时间
                .maxIdleTime(Duration.ofSeconds(5))
                // 最大等待连接数量
                .pendingAcquireMaxCount(-1)
                .build();
        /**
         * doOnBind	当服务器channel即将被绑定的时候调用。
         * doOnBound	当服务器channel已经被绑定的时候调用。
         * doOnChannelInit	当channel初始化的时候被调用。
         * doOnConnection	当一个远程客户端连接上的时候被调用。
         * doOnUnbound	当服务器channel解绑的时候被调用。
         */
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 600000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(Duration.ofSeconds(600))
                .keepAlive(true)
                //连接成功
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(600))
                        .addHandlerLast(new WriteTimeoutHandler(60)))
                //每次请求后执行flush，防止服务器主动断开连接
                .doAfterRequest((httpClientRequest, connection) -> {
                    connection.channel().alloc().buffer().release();
                    connection.channel().flush();
                    connection.channel().pipeline().flush();
                });

        return WebClient.builder()
                .baseUrl(this.host)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONNECTION, "keep-alive")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
