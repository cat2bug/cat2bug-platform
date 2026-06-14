package com.cat2bug.framework.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.sql.DataSource;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.nativeimage.NativeDomainEntityRegistry;

/**
 * Mybatis支持*匹配扫描包
 * 
 * @author ruoyi
 */
@Configuration(proxyBeanMethods = false)
public class MyBatisConfig
{
    /**
     * GraalVM Native 下 classpath 通配扫描不可用，使用显式包列表。
     */
    private static final String NATIVE_TYPE_ALIASES_PACKAGES =
            "com.cat2bug.system.domain,com.cat2bug.generator.domain,com.cat2bug.quartz.domain,"
                    + "com.cat2bug.common.core.domain,com.cat2bug.api.domain,com.cat2bug.ai.domain,"
                    + "com.cat2bug.im.domain,com.cat2bug.framework.web.domain,com.cat2bug.web.domain.setup";

    @Autowired
    private Environment env;

    @Autowired
    private DatabaseIdProvider databaseIdProvider;

    static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    public static String setTypeAliasesPackage(String typeAliasesPackage)
    {
        if (runningNativeImage())
        {
            return NATIVE_TYPE_ALIASES_PACKAGES;
        }
        ResourcePatternResolver resolver = (ResourcePatternResolver) new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        List<String> allResult = new ArrayList<String>();
        try
        {
            for (String aliasesPackage : typeAliasesPackage.split(","))
            {
                List<String> result = new ArrayList<String>();
                aliasesPackage = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                        + ClassUtils.convertClassNameToResourcePath(aliasesPackage.trim()) + "/" + DEFAULT_RESOURCE_PATTERN;
                Resource[] resources = resolver.getResources(aliasesPackage);
                if (resources != null && resources.length > 0)
                {
                    MetadataReader metadataReader = null;
                    for (Resource resource : resources)
                    {
                        if (resource.isReadable())
                        {
                            metadataReader = metadataReaderFactory.getMetadataReader(resource);
                            try
                            {
                                result.add(Class.forName(metadataReader.getClassMetadata().getClassName()).getPackage().getName());
                            }
                            catch (ClassNotFoundException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                if (result.size() > 0)
                {
                    HashSet<String> hashResult = new HashSet<String>(result);
                    allResult.addAll(hashResult);
                }
            }
            if (allResult.size() > 0)
            {
                typeAliasesPackage = String.join(",", (String[]) allResult.toArray(new String[0]));
            }
            else
            {
                return NATIVE_TYPE_ALIASES_PACKAGES;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return typeAliasesPackage;
    }

    private static boolean runningNativeImage()
    {
        return System.getProperty("org.graalvm.nativeimage.imagecode") != null;
    }

    public Resource[] resolveMapperLocations(String[] mapperLocations)
    {
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        if (runningNativeImage())
        {
            try
            {
                return resourceResolver.getResources("classpath*:mapper/**/*Mapper.xml");
            }
            catch (IOException e)
            {
                return new Resource[0];
            }
        }
        List<Resource> resources = new ArrayList<Resource>();
        if (mapperLocations != null)
        {
            for (String mapperLocation : mapperLocations)
            {
                try
                {
                    Resource[] mappers = resourceResolver.getResources(mapperLocation);
                    resources.addAll(Arrays.asList(mappers));
                }
                catch (IOException e)
                {
                    // ignore
                }
            }
        }
        return resources.toArray(new Resource[resources.size()]);
    }

    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception
    {
        String typeAliasesPackage = env.getProperty("mybatis.typeAliasesPackage");
        String mapperLocations = env.getProperty("mybatis.mapperLocations");
        String configLocation = env.getProperty("mybatis.configLocation");
        typeAliasesPackage = setTypeAliasesPackage(typeAliasesPackage);
        VFS.addImplClass(SpringBootVFS.class);

        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        if (runningNativeImage())
        {
            sessionFactory.setTypeAliases(
                    NativeDomainEntityRegistry.ensureInitialized(Thread.currentThread().getContextClassLoader()));
        }
        else
        {
            sessionFactory.setTypeAliasesPackage(typeAliasesPackage);
        }
        sessionFactory.setMapperLocations(resolveMapperLocations(StringUtils.split(mapperLocations, ",")));
        sessionFactory.setConfigLocation(new DefaultResourceLoader().getResource(configLocation));
        sessionFactory.setDatabaseIdProvider(this::resolveDatabaseId);
        return sessionFactory.getObject();
    }

    private String resolveDatabaseId(DataSource dataSource)
    {
        String configured = env.getProperty("spring.database-type");
        if (StringUtils.isEmpty(configured) && runningNativeImage())
        {
            configured = "h2";
        }
        if (StringUtils.isNotEmpty(configured))
        {
            return configured;
        }
        try
        {
            return databaseIdProvider.getDatabaseId(dataSource);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("无法解析 MyBatis databaseId", ex);
        }
    }
}