package com.cat2bug.common.config.j2cache;

import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.J2CacheBuilder;
import net.oschina.j2cache.J2CacheConfig;
import net.oschina.j2cache.cache.support.util.SpringJ2CacheConfigUtil;
import net.oschina.j2cache.cache.support.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;

/**
 * Spring Boot 3 下 J2Cache 自动配置，行为对齐 j2cache-spring-boot2-starter。
 */
@AutoConfiguration
@ConditionalOnClass(name = "net.oschina.j2cache.J2Cache")
@EnableConfigurationProperties(net.oschina.j2cache.autoconfigure.J2CacheConfig.class)
@PropertySource(value = "${j2cache.config-location}", encoding = "UTF-8", ignoreResourceNotFound = true)
@Import({
        net.oschina.j2cache.autoconfigure.J2CacheSpringRedisAutoConfiguration.class,
        net.oschina.j2cache.autoconfigure.J2CacheSpringCacheAutoConfiguration.class
})
public class Cat2BugJ2CacheAutoConfiguration {

    @Autowired
    private StandardEnvironment standardEnvironment;

    @Bean
    public SpringUtil springUtil() {
        return new SpringUtil();
    }

    @Bean
    public J2CacheConfig j2CacheConfig() throws IOException {
        return SpringJ2CacheConfigUtil.initFromConfig(standardEnvironment);
    }

    @Bean
    @DependsOn({"springUtil", "j2CacheConfig"})
    public CacheChannel cacheChannel(J2CacheConfig j2CacheConfig) {
        return J2CacheBuilder.init(j2CacheConfig).getChannel();
    }
}
