package com.cat2bug.nativeimage;

import com.cat2bug.common.nativeimage.NativeDomainEntityRegistry;

import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ReuseExecutor;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.javassist.util.proxy.ProxyFactory;
import org.apache.ibatis.javassist.util.proxy.RuntimeSupport;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MyBatis GraalVM Native 配置（源自 mybatis-spring-boot-starter wiki）。
 */
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(MyBatisNativeConfiguration.MyBatisRuntimeHintsRegistrar.class)
public class MyBatisNativeConfiguration
{
    @Bean
    MyBatisBeanFactoryInitializationAotProcessor myBatisBeanFactoryInitializationAotProcessor()
    {
        return new MyBatisBeanFactoryInitializationAotProcessor();
    }

    @Bean
    static MyBatisMapperFactoryBeanPostProcessor myBatisMapperFactoryBeanPostProcessor()
    {
        return new MyBatisMapperFactoryBeanPostProcessor();
    }

    @Bean
    static NativeDomainEntityAotProcessor nativeDomainEntityAotProcessor()
    {
        return new NativeDomainEntityAotProcessor();
    }

    static class NativeDomainEntityAotProcessor implements BeanFactoryInitializationAotProcessor
    {
        @Override
        public BeanFactoryInitializationAotContribution processAheadOfTime(
                ConfigurableListableBeanFactory beanFactory)
        {
            ClassLoader classLoader = beanFactory.getBeanClassLoader();
            return (generationContext, code) -> {
                Class<?>[] domainTypes = NativeDomainEntityRegistry.scanDomainEntities(classLoader);
                StringBuilder content = new StringBuilder();
                for (Class<?> domainType : domainTypes)
                {
                    generationContext.getRuntimeHints().reflection().registerType(domainType, MemberCategory.values());
                    content.append(domainType.getName()).append('\n');
                }
                generationContext.getGeneratedFiles().addResourceFile(
                        NativeDomainEntityRegistry.RESOURCE_PATH, content.toString());
                generationContext.getRuntimeHints().resources().registerPattern(
                        NativeDomainEntityRegistry.RESOURCE_PATH);
            };
        }
    }

    static class MyBatisRuntimeHintsRegistrar implements RuntimeHintsRegistrar
    {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader)
        {
            Stream.of(
                    RawLanguageDriver.class,
                    XMLLanguageDriver.class,
                    org.apache.ibatis.session.Configuration.class,
                    RuntimeSupport.class,
                    ProxyFactory.class,
                    Slf4jImpl.class,
                    Log.class,
                    JakartaCommonsLoggingImpl.class,
                    Log4j2Impl.class,
                    Jdk14LoggingImpl.class,
                    StdOutImpl.class,
                    NoLoggingImpl.class,
                    SqlSessionFactory.class,
                    PerpetualCache.class,
                    FifoCache.class,
                    LruCache.class,
                    SoftCache.class,
                    WeakCache.class,
                    SqlSessionFactoryBean.class,
                    Executor.class,
                    CachingExecutor.class,
                    SimpleExecutor.class,
                    ReuseExecutor.class,
                    BatchExecutor.class,
                    Plugin.class,
                    Interceptor.class,
                    Invocation.class,
                    com.github.pagehelper.PageInterceptor.class,
                    com.github.pagehelper.PageHelper.class,
                    com.github.pagehelper.page.PageAutoDialect.class,
                    com.github.pagehelper.dialect.helper.MySqlDialect.class,
                    BoundSql.class,
                    ParameterMapping.class,
                    MappedStatement.class,
                    ArrayList.class,
                    HashMap.class,
                    TreeSet.class,
                    HashSet.class
            ).forEach(type -> hints.reflection().registerType(type, MemberCategory.values()));
            registerExecutorQueryMethods(hints);
            hints.reflection().registerType(
                    org.apache.ibatis.reflection.MetaObject.class,
                    MemberCategory.values());
            hints.reflection().registerType(
                    org.apache.ibatis.reflection.Reflector.class,
                    MemberCategory.values());
            hints.proxies().registerJdkProxy(Executor.class);
            hints.resources().registerPattern(NativeDomainEntityRegistry.RESOURCE_PATH);
            Stream.of(
                    "org/apache/ibatis/builder/xml/*.dtd",
                    "org/apache/ibatis/builder/xml/*.xsd"
            ).forEach(pattern -> hints.resources().registerPattern(pattern));
            Stream.of(
                    "mapper/**/*.xml",
                    "mapper/**/*Mapper.xml"
            ).forEach(pattern -> hints.resources().registerPattern(pattern));
        }

        private static void registerExecutorQueryMethods(RuntimeHints hints)
        {
            hints.reflection().registerType(Signature.class, MemberCategory.values());
            hints.reflection().registerType(Intercepts.class, MemberCategory.values());
            hints.reflection().registerType(MappedStatement.class, MemberCategory.INVOKE_PUBLIC_METHODS);
            hints.reflection().registerType(RowBounds.class, MemberCategory.INVOKE_PUBLIC_METHODS);
            hints.reflection().registerType(ResultHandler.class, MemberCategory.INVOKE_PUBLIC_METHODS);
            hints.reflection().registerType(CacheKey.class, MemberCategory.INVOKE_PUBLIC_METHODS);
            hints.reflection().registerType(BoundSql.class, MemberCategory.INVOKE_PUBLIC_METHODS);
            registerPublicMethod(hints, Executor.class, "query",
                    MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class);
            registerPublicMethod(hints, Executor.class, "query",
                    MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                    CacheKey.class, BoundSql.class);
        }

        private static void registerPublicMethod(
                RuntimeHints hints,
                Class<?> type,
                String name,
                Class<?>... parameterTypes)
        {
            try
            {
                hints.reflection().registerMethod(type.getMethod(name, parameterTypes), ExecutableMode.INVOKE);
            }
            catch (NoSuchMethodException e)
            {
                throw new IllegalStateException("Missing method " + type.getName() + '.' + name, e);
            }
        }
    }

    static class MyBatisBeanFactoryInitializationAotProcessor
            implements BeanFactoryInitializationAotProcessor, BeanRegistrationExcludeFilter
    {
        private final Set<Class<?>> excludeClasses = new HashSet<>();

        MyBatisBeanFactoryInitializationAotProcessor()
        {
            excludeClasses.add(MapperScannerConfigurer.class);
        }

        @Override
        public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean)
        {
            return excludeClasses.contains(registeredBean.getBeanClass());
        }

        @Override
        public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory)
        {
            String[] beanNames = beanFactory.getBeanNamesForType(MapperFactoryBean.class);
            if (beanNames.length == 0)
            {
                return null;
            }
            return (context, code) -> {
                RuntimeHints hints = context.getRuntimeHints();
                for (String beanName : beanNames)
                {
                    BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName.substring(1));
                    PropertyValue mapperInterface = beanDefinition.getPropertyValues().getPropertyValue("mapperInterface");
                    if (mapperInterface != null && mapperInterface.getValue() != null)
                    {
                        Class<?> mapperInterfaceType = (Class<?>) mapperInterface.getValue();
                        if (mapperInterfaceType != null)
                        {
                            registerReflectionTypeIfNecessary(mapperInterfaceType, hints);
                            hints.proxies().registerJdkProxy(mapperInterfaceType);
                            hints.resources()
                                    .registerPattern(mapperInterfaceType.getName().replace('.', '/').concat(".xml"));
                            registerMapperRelationships(mapperInterfaceType, hints);
                        }
                    }
                }
            };
        }

        private void registerMapperRelationships(Class<?> mapperInterfaceType, RuntimeHints hints)
        {
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(mapperInterfaceType);
            for (Method method : methods)
            {
                if (method.getDeclaringClass() != Object.class)
                {
                    ReflectionUtils.makeAccessible(method);
                    registerSqlProviderTypes(method, hints, SelectProvider.class, SelectProvider::value, SelectProvider::type);
                    registerSqlProviderTypes(method, hints, InsertProvider.class, InsertProvider::value, InsertProvider::type);
                    registerSqlProviderTypes(method, hints, UpdateProvider.class, UpdateProvider::value, UpdateProvider::type);
                    registerSqlProviderTypes(method, hints, DeleteProvider.class, DeleteProvider::value, DeleteProvider::type);
                    Class<?> returnType = MyBatisMapperTypeUtils.resolveReturnClass(mapperInterfaceType, method);
                    registerReflectionTypeIfNecessary(returnType, hints);
                    MyBatisMapperTypeUtils.resolveParameterClasses(mapperInterfaceType, method)
                            .forEach(type -> registerReflectionTypeIfNecessary(type, hints));
                }
            }
        }

        @SafeVarargs
        private <T extends Annotation> void registerSqlProviderTypes(
                Method method,
                RuntimeHints hints,
                Class<T> annotationType,
                Function<T, Class<?>>... providerTypeResolvers)
        {
            for (T annotation : method.getAnnotationsByType(annotationType))
            {
                for (Function<T, Class<?>> providerTypeResolver : providerTypeResolvers)
                {
                    registerReflectionTypeIfNecessary(providerTypeResolver.apply(annotation), hints);
                }
            }
        }

        private void registerReflectionTypeIfNecessary(Class<?> type, RuntimeHints hints)
        {
            if (!type.isPrimitive() && !type.getName().startsWith("java"))
            {
                hints.reflection().registerType(type, MemberCategory.values());
            }
        }
    }

    static class MyBatisMapperTypeUtils
    {
        private MyBatisMapperTypeUtils()
        {
        }

        static Class<?> resolveReturnClass(Class<?> mapperInterface, Method method)
        {
            Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
            return typeToClass(resolvedReturnType, method.getReturnType());
        }

        static Set<Class<?>> resolveParameterClasses(Class<?> mapperInterface, Method method)
        {
            return Stream.of(TypeParameterResolver.resolveParamTypes(method, mapperInterface))
                    .map(type -> typeToClass(type, type instanceof Class ? (Class<?>) type : Object.class))
                    .collect(Collectors.toSet());
        }

        private static Class<?> typeToClass(Type src, Class<?> fallback)
        {
            Class<?> result = null;
            if (src instanceof Class<?>)
            {
                if (((Class<?>) src).isArray())
                {
                    result = ((Class<?>) src).getComponentType();
                }
                else
                {
                    result = (Class<?>) src;
                }
            }
            else if (src instanceof ParameterizedType)
            {
                ParameterizedType parameterizedType = (ParameterizedType) src;
                int index = (parameterizedType.getRawType() instanceof Class
                        && Map.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())
                        && parameterizedType.getActualTypeArguments().length > 1) ? 1 : 0;
                Type actualType = parameterizedType.getActualTypeArguments()[index];
                result = typeToClass(actualType, fallback);
            }
            if (result == null)
            {
                result = fallback;
            }
            return result;
        }
    }

    static class MyBatisMapperFactoryBeanPostProcessor implements MergedBeanDefinitionPostProcessor, BeanFactoryAware
    {
        private static final org.apache.commons.logging.Log LOG = LogFactory.getLog(
                MyBatisMapperFactoryBeanPostProcessor.class);

        private static final String MAPPER_FACTORY_BEAN = "org.mybatis.spring.mapper.MapperFactoryBean";

        private ConfigurableBeanFactory beanFactory;

        @Override
        public void setBeanFactory(BeanFactory beanFactory)
        {
            this.beanFactory = (ConfigurableBeanFactory) beanFactory;
        }

        @Override
        public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName)
        {
            if (ClassUtils.isPresent(MAPPER_FACTORY_BEAN, this.beanFactory.getBeanClassLoader()))
            {
                resolveMapperFactoryBeanTypeIfNecessary(beanDefinition);
            }
        }

        private void resolveMapperFactoryBeanTypeIfNecessary(RootBeanDefinition beanDefinition)
        {
            if (!beanDefinition.hasBeanClass() || !MapperFactoryBean.class.isAssignableFrom(beanDefinition.getBeanClass()))
            {
                return;
            }
            Class<?> mapperInterface = getMapperInterface(beanDefinition);
            if (mapperInterface == null && beanDefinition.getResolvableType().hasUnresolvableGenerics())
            {
                return;
            }
            if (mapperInterface != null)
            {
                beanDefinition.setTargetType(
                        ResolvableType.forClassWithGenerics(beanDefinition.getBeanClass(), mapperInterface));
                // Native AOT：构造参数必须是 Class，不能是 FQCN 字符串，否则 AutowiredArguments 无法解析
                beanDefinition.getConstructorArgumentValues().clear();
                beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(mapperInterface);
                if (!beanDefinition.getPropertyValues().contains("sqlSessionFactory"))
                {
                    beanDefinition.getPropertyValues().add("sqlSessionFactory", new RuntimeBeanReference("sqlSessionFactory"));
                }
            }
        }

        private Class<?> getMapperInterface(RootBeanDefinition beanDefinition)
        {
            try
            {
                return (Class<?>) beanDefinition.getPropertyValues().get("mapperInterface");
            }
            catch (Exception e)
            {
                LOG.debug("Fail getting mapper interface type.", e);
                return null;
            }
        }
    }
}
