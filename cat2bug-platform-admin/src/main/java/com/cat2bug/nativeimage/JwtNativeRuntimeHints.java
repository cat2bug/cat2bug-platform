package com.cat2bug.nativeimage;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

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
 * JJWT 实现类在运行时通过类名动态加载，需整包注册到 Native 镜像。
 */
public class JwtNativeRuntimeHints implements RuntimeHintsRegistrar
{
    private static final String[] PACKAGE_PREFIXES = {
            "io/jsonwebtoken/impl/",
            "io/jsonwebtoken/jackson/"
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader)
    {
        Set<String> classNames = new HashSet<>();
        collectFromClassLoader(classLoader, classNames);
        for (String className : classNames)
        {
            hints.reflection().registerType(TypeReference.of(className), MemberCategory.values());
        }
    }

    private static void collectFromClassLoader(ClassLoader classLoader, Set<String> classNames)
    {
        for (String prefix : PACKAGE_PREFIXES)
        {
            String resourcePath = prefix;
            Enumeration<URL> resources;
            try
            {
                resources = classLoader.getResources(resourcePath);
            }
            catch (IOException e)
            {
                continue;
            }
            while (resources.hasMoreElements())
            {
                URL url = resources.nextElement();
                collectFromUrl(url, prefix, classNames);
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
                JarEntry entry = entries.nextElement();
                addClassName(entry.getName(), packagePrefix, classNames);
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
