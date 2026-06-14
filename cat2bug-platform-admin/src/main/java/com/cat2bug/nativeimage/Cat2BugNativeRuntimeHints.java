package com.cat2bug.nativeimage;

import com.cat2bug.common.nativeimage.NativeDomainEntityRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.MessageFormatMessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.status.StatusLogger;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM Native 资源与反射提示（AOT 阶段注册）。
 */
public class Cat2BugNativeRuntimeHints implements RuntimeHintsRegistrar
{
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader)
    {
        hints.resources().registerPattern(NativeDomainEntityRegistry.RESOURCE_PATH);

        hints.resources().registerPattern("static/**");
        hints.resources().registerPattern("mapper/**/*Mapper.xml");
        hints.resources().registerPattern("db/migration/**");
        hints.resources().registerPattern("mybatis/**");
        hints.resources().registerPattern("i18n/**");
        hints.resources().registerPattern("defaults/**");
        hints.resources().registerPattern("banner.txt");
        hints.resources().registerPattern("*.xml");
        hints.resources().registerPattern("*.properties");
        hints.resources().registerPattern("*.yml");

        hints.reflection().registerType(
                com.cat2bug.framework.config.EmbeddedSpaFallbackFilter.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(
                com.cat2bug.framework.config.properties.DruidMasterDataSourceProperties.class,
                MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(
                com.cat2bug.framework.config.properties.DruidSlaveDataSourceProperties.class,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        registerLog4j2Hints(hints);
    }

    private static void registerLog4j2Hints(RuntimeHints hints)
    {
        hints.resources().registerPattern("META-INF/services/org.apache.logging.log4j.spi.Provider");
        hints.resources().registerPattern("META-INF/log4j-provider.properties");

        registerLog4jReflection(hints, DefaultFlowMessageFactory.class);
        registerLog4jReflection(hints, ReusableMessageFactory.class);
        registerLog4jReflection(hints, ParameterizedMessageFactory.class);
        registerLog4jReflection(hints, MessageFormatMessageFactory.class);
        registerLog4jReflection(hints, SimpleMessageFactory.class);
        registerLog4jReflection(hints, LogManager.class);
        registerLog4jReflection(hints, StatusLogger.class);
        registerLog4jReflection(hints, ExtendedLogger.class);
        registerLog4jReflection(hints, org.apache.logging.log4j.spi.AbstractLogger.class);
        registerLog4jReflection(hints, org.apache.logging.log4j.spi.Provider.class);
        registerLog4jReflection(hints, org.apache.logging.slf4j.SLF4JProvider.class);
        registerLog4jReflection(hints, org.apache.logging.log4j.util.LoaderUtil.class);

        hints.reflection().registerType(com.github.pagehelper.PageHelper.class, MemberCategory.values());
        hints.reflection().registerType(com.github.pagehelper.PageInterceptor.class, MemberCategory.values());
        hints.reflection().registerType(com.github.pagehelper.page.PageAutoDialect.class, MemberCategory.values());
        hints.reflection().registerType(com.github.pagehelper.dialect.helper.MySqlDialect.class, MemberCategory.values());
        hints.reflection().registerType(org.apache.ibatis.executor.Executor.class, MemberCategory.values());
        hints.reflection().registerType(org.apache.ibatis.plugin.Plugin.class, MemberCategory.values());
        hints.proxies().registerJdkProxy(org.apache.ibatis.executor.Executor.class);

        hints.reflection().registerType(com.cat2bug.common.config.WebSocketSpringConfigurator.class, MemberCategory.values());
        hints.reflection().registerType(com.cat2bug.common.websocket.MessageWebsocket.class, MemberCategory.values());
    }

    private static void registerLog4jReflection(RuntimeHints hints, Class<?> type)
    {
        hints.reflection().registerType(type, MemberCategory.values());
    }
}
