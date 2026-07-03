package com.cat2bug.nativeimage;

import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.J2CacheBuilder;
import net.oschina.j2cache.J2CacheConfig;
import net.oschina.j2cache.cache.support.redis.SpringRedisProvider;
import net.oschina.j2cache.util.FastjsonSerializer;
import net.oschina.j2cache.util.SerializationUtils;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * J2Cache Redis provider、Jedis 客户端与 fastjson 序列化在 Native 下需反射/资源提示。
 */
public class J2CacheNativeRuntimeHints implements RuntimeHintsRegistrar
{
    private static final String[] PACKAGE_PREFIXES = {
            "net/oschina/j2cache/",
            "redis/clients/jedis/",
            "com/alibaba/fastjson/"
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader)
    {
        hints.resources().registerPattern("redis-native-j2cache.properties");
        hints.resources().registerPattern("redis-j2cache.properties");
        hints.resources().registerPattern("local-j2cache.properties");
        hints.resources().registerPattern("caffeine.properties");

        registerReflection(hints, CacheChannel.class);
        registerReflection(hints, J2CacheBuilder.class);
        registerReflection(hints, J2CacheConfig.class);
        registerReflection(hints, SpringRedisProvider.class);
        registerReflection(hints, FastjsonSerializer.class);
        registerReflection(hints, SerializationUtils.class);
        registerReflection(hints, Jedis.class);
        registerReflection(hints, JedisPool.class);
        registerReflection(hints, JedisPoolConfig.class);
        registerReflection(hints, HostAndPort.class);
        registerReflection(hints, DefaultJedisClientConfig.class);

        Set<String> classNames = new HashSet<>();
        collectFromClassLoader(classLoader, classNames);
        for (String className : classNames)
        {
            hints.reflection().registerType(TypeReference.of(className), MemberCategory.values());
        }
    }

    private static void registerReflection(RuntimeHints hints, Class<?> type)
    {
        hints.reflection().registerType(type, MemberCategory.values());
    }

    private static void collectFromClassLoader(ClassLoader classLoader, Set<String> classNames)
    {
        for (String prefix : PACKAGE_PREFIXES)
        {
            Enumeration<URL> resources;
            try
            {
                resources = classLoader.getResources(prefix);
            }
            catch (IOException e)
            {
                continue;
            }
            while (resources.hasMoreElements())
            {
                collectFromUrl(resources.nextElement(), prefix, classNames);
            }
        }
    }

    private static void collectFromUrl(URL url, String packagePrefix, Set<String> classNames)
    {
        if (url == null)
        {
            return;
        }
        String protocol = url.getProtocol();
        try
        {
            if ("jar".equals(protocol))
            {
                collectFromJarUrl(url, packagePrefix, classNames);
            }
            else if ("file".equals(protocol))
            {
                collectFromDirectory(Path.of(url.toURI()), packagePrefix, classNames);
            }
        }
        catch (Exception ignored)
        {
            // AOT 阶段尽力扫描；缺失条目会在运行时继续暴露
        }
    }

    private static void collectFromJarUrl(URL url, String packagePrefix, Set<String> classNames) throws IOException
    {
        String path = url.getPath();
        int separator = path.indexOf("!/");
        if (separator <= 0)
        {
            return;
        }
        Path jarPath = Path.of(path.substring(5, separator));
        try (JarFile jarFile = new JarFile(jarPath.toFile()))
        {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements())
            {
                addClassName(entries.nextElement().getName(), packagePrefix, classNames);
            }
        }
    }

    private static void collectFromDirectory(Path root, String packagePrefix, Set<String> classNames) throws IOException
    {
        Path packageRoot = root.resolve(packagePrefix);
        if (!Files.isDirectory(packageRoot))
        {
            return;
        }
        try (Stream<Path> paths = Files.walk(packageRoot))
        {
            paths.filter(Files::isRegularFile)
                    .map(packageRoot::relativize)
                    .map(Path::toString)
                    .map(name -> packagePrefix + name.replace('\\', '/'))
                    .forEach(name -> addClassName(name, packagePrefix, classNames));
        }
    }

    private static void addClassName(String entryName, String packagePrefix, Set<String> classNames)
    {
        if (entryName == null
                || !entryName.startsWith(packagePrefix)
                || !entryName.endsWith(".class")
                || entryName.contains("$"))
        {
            return;
        }
        classNames.add(entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.'));
    }
}
