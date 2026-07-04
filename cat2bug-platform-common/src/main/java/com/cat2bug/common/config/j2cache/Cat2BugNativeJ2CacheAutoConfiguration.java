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
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;

/**
 * GraalVM Native 下 Redis-only J2Cache（L1 禁用），在 {@code native} profile 下注册；
 * 运行期 {@code cat2bug.cache.type=redis} 时经 {@link CacheChannel} 读写（见 {@code RedisCache}）。
 * AOT 须在 {@code process-aot} 阶段以 {@code cat2bug.cache.type=redis} 处理，否则运行期选 Redis 时无 {@link CacheChannel} bean。
 */
@AutoConfiguration
@Profile("native")
@ConditionalOnClass(name = "net.oschina.j2cache.J2Cache")
@EnableConfigurationProperties(net.oschina.j2cache.autoconfigure.J2CacheConfig.class)
@PropertySource(value = "classpath:redis-native-j2cache.properties", encoding = "UTF-8")
@Import({
        net.oschina.j2cache.autoconfigure.J2CacheSpringRedisAutoConfiguration.class,
        net.oschina.j2cache.autoconfigure.J2CacheSpringCacheAutoConfiguration.class
})
public class Cat2BugNativeJ2CacheAutoConfiguration {

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
    @Lazy
    @DependsOn({"springUtil", "j2CacheConfig"})
    public CacheChannel cacheChannel(J2CacheConfig j2CacheConfig) {
        return J2CacheBuilder.init(j2CacheConfig).getChannel();
    }
}
