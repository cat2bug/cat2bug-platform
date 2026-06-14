package com.cat2bug.common.nativeimage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * Native 镜像：AOT 阶段生成 domain 类型列表资源，运行时加载并注册 MyBatis type alias。
 */
public final class NativeDomainEntityRegistry
{
    public static final String RESOURCE_PATH = "META-INF/cat2bug/native-domain-types.list";

    public static final String DOMAIN_PACKAGES =
            "com.cat2bug.system.domain,com.cat2bug.generator.domain,com.cat2bug.quartz.domain,"
                    + "com.cat2bug.common.core.domain,com.cat2bug.api.domain,com.cat2bug.ai.domain,"
                    + "com.cat2bug.im.domain,com.cat2bug.framework.web.domain,com.cat2bug.web.domain.setup";

    private static Class<?>[] types;

    private NativeDomainEntityRegistry()
    {
    }

    public static synchronized Class<?>[] ensureInitialized(ClassLoader classLoader)
    {
        if (types == null)
        {
            types = loadTypes(classLoader);
        }
        return types;
    }

    public static Class<?>[] getTypes()
    {
        return types != null ? types : new Class<?>[0];
    }

    public static List<Class<?>> asList()
    {
        return Collections.unmodifiableList(Arrays.asList(getTypes()));
    }

    public static String buildDomainTypeListContent(ClassLoader classLoader)
    {
        StringBuilder content = new StringBuilder();
        for (Class<?> type : scanDomainEntities(classLoader))
        {
            content.append(type.getName()).append('\n');
        }
        return content.toString();
    }

    static Class<?>[] loadTypes(ClassLoader classLoader)
    {
        ClassLoader loader = classLoader != null ? classLoader : NativeDomainEntityRegistry.class.getClassLoader();
        try (InputStream inputStream = loader.getResourceAsStream(RESOURCE_PATH))
        {
            if (inputStream != null)
            {
                return parseTypeList(inputStream, loader);
            }
        }
        catch (IOException ignored)
        {
        }
        return scanDomainEntities(loader);
    }

    public static Class<?>[] scanDomainEntities(ClassLoader classLoader)
    {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));
        List<Class<?>> scanned = new ArrayList<>();
        for (String pkg : DOMAIN_PACKAGES.split(","))
        {
            String trimmed = pkg.trim();
            if (trimmed.isEmpty())
            {
                continue;
            }
            for (BeanDefinition candidate : scanner.findCandidateComponents(trimmed))
            {
                String className = candidate.getBeanClassName();
                if (className == null)
                {
                    continue;
                }
                try
                {
                    Class<?> type = ClassUtils.forName(className, classLoader);
                    if (isMyBatisTypeAliasCandidate(type))
                    {
                        scanned.add(type);
                    }
                }
                catch (ClassNotFoundException | LinkageError ignored)
                {
                }
            }
        }
        return scanned.toArray(new Class<?>[0]);
    }

    private static Class<?>[] parseTypeList(InputStream inputStream, ClassLoader classLoader) throws IOException
    {
        List<Class<?>> parsed = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                {
                    continue;
                }
                try
                {
                    Class<?> type = ClassUtils.forName(line, classLoader);
                    if (isMyBatisTypeAliasCandidate(type))
                    {
                        parsed.add(type);
                    }
                }
                catch (ClassNotFoundException | LinkageError ignored)
                {
                }
            }
        }
        return parsed.toArray(new Class<?>[0]);
    }

    private static boolean isMyBatisTypeAliasCandidate(Class<?> type)
    {
        return type != null && !type.isInterface() && !type.isEnum() && !type.getName().contains("$");
    }
}
