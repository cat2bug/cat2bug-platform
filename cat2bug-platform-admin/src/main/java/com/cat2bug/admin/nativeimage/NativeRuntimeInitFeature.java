package com.cat2bug.admin.nativeimage;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Native 构建：
 * <ol>
 *   <li>把会在镜像堆里留下 Random、已启动线程或构建机状态的类推迟到运行期初始化。
 *   JGit {@code WindowCache} 静态字段持有 {@code Random}，{@code WorkQueue} 会 {@code prestartAllCoreThreads}。</li>
 *   <li>按包前缀批量注册反射：MyBatis resultType/parameterType、Jackson 序列化的
 *   domain/VO 都靠反射；JJWT 0.12 通过类名反射加载 {@code io.jsonwebtoken.impl.*}。</li>
 * </ol>
 */
public final class NativeRuntimeInitFeature implements Feature {

    /** 需要整类反射（构造器/方法/字段）的包前缀。 */
    private static final String[] REFLECTION_PREFIXES = {
            "com.cat2bug.common.",
            "com.cat2bug.system.git.browse.domain.",
            "com.cat2bug.system.mybatis.",
            "com.cat2bug.admin.domain.",
            // McpOperationCatalog 反射读取 REST 资源方法与注解；MCP 工具参数 record 由 Jackson 反序列化
            "com.cat2bug.admin.resource.",
            "com.cat2bug.admin.mcp.",
            // MyBatis OGNL 静态调用 PageOrder.current()
            "com.cat2bug.framework.web.PageOrder",
            "io.jsonwebtoken.impl.",
            "io.jsonwebtoken.jackson.",
    };

    /** 仅注册其中的枚举（JGit Config 通过反射调用 {@code values()}）。 */
    private static final String[] ENUM_PREFIXES = {
            "org.eclipse.jgit.",
    };

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        RuntimeClassInitialization.initializeAtRunTime(
                "io.netty.util.internal.PlatformDependent0",
                "io.netty.util.internal.PlatformDependent",
                "io.netty.util.internal.SocketUtils",
                "io.smallrye.common.ref.References",
                "io.smallrye.common.ref.References$ReaperThread",
                "sun.java2d.Disposer",
                "org.apache.commons.compress",
                "org.eclipse.jgit.internal.storage.file.WindowCache",
                "org.eclipse.jgit.lib.internal.WorkQueue",
                "org.eclipse.jgit.util.FileUtils",
                "org.eclipse.jgit.transport.HttpAuthMethod$Digest",
                // 静态 HttpClient 不能烤进镜像堆
                "com.cat2bug.admin.resource.ProfileResource",
                // JJWT 静态 SecureRandom / 算法注册表，反射可达后会被常量折叠进镜像堆
                "io.jsonwebtoken",
                // 静态 SecureRandom 生成访问令牌，烤进镜像会让所有实例共享种子
                "com.cat2bug.common.security.AccessTokens");

        int registered = 0;
        int providers = 0;
        for (Path entry : access.getApplicationClassPath()) {
            for (String providerName : listServiceProviders(entry)) {
                Class<?> provider = access.findClassByName(providerName);
                if (provider != null && registerFully(provider)) {
                    providers++;
                }
            }
            for (String className : listClassNames(entry)) {
                if (startsWithAny(className, ENUM_PREFIXES)) {
                    Class<?> enumCandidate = access.findClassByName(className);
                    if (enumCandidate != null && isEnumSafe(enumCandidate) && registerFully(enumCandidate)) {
                        registered++;
                    }
                    continue;
                }
                if (!matches(className)) {
                    continue;
                }
                Class<?> cls = access.findClassByName(className);
                if (cls != null && registerFully(cls)) {
                    registered++;
                }
            }
        }
        System.out.println("[cat2bug-native] reflection registered classes: " + registered
                + ", service providers: " + providers);
    }

    /**
     * Quarkus 关闭了 GraalVM 自动 ServiceLoader 注册；这里对白名单接口的 provider 做反射注册，
     * 对应 {@code META-INF/services/*} 资源由 {@code quarkus.native.resources.includes} 打入镜像。
     */
    private static List<String> listServiceProviders(Path entry) {
        List<String> providers = new ArrayList<>();
        for (String service : SERVICE_INTERFACES) {
            String resource = "META-INF/services/" + service;
            try {
                List<String> lines;
                if (Files.isDirectory(entry)) {
                    Path file = entry.resolve(resource);
                    if (!Files.isRegularFile(file)) {
                        continue;
                    }
                    lines = Files.readAllLines(file);
                } else if (entry.toString().endsWith(".jar") && Files.isRegularFile(entry)) {
                    try (JarFile jar = new JarFile(entry.toFile())) {
                        JarEntry je = jar.getJarEntry(resource);
                        if (je == null) {
                            continue;
                        }
                        lines = new String(jar.getInputStream(je).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                                .lines().toList();
                    }
                } else {
                    continue;
                }
                for (String line : lines) {
                    String name = line.replaceAll("#.*", "").trim();
                    if (!name.isEmpty()) {
                        providers.add(name);
                    }
                }
            } catch (IOException ignored) {
                // 忽略
            }
        }
        return providers;
    }

    /** 运行期通过 ServiceLoader 查找实现的接口（与 application.properties 中 META-INF/services 资源保持一致）。 */
    private static final String[] SERVICE_INTERFACES = {
            "io.jsonwebtoken.io.Serializer",
            "io.jsonwebtoken.io.Deserializer",
            "io.jsonwebtoken.CompressionCodec",
    };

    private static boolean isEnumSafe(Class<?> cls) {
        try {
            return cls.isEnum();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean startsWithAny(String className, String[] prefixes) {
        for (String prefix : prefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String className) {
        for (String prefix : REFLECTION_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean registerFully(Class<?> cls) {
        try {
            RuntimeReflection.register(cls);
            RuntimeReflection.register(cls.getDeclaredConstructors());
            RuntimeReflection.register(cls.getDeclaredMethods());
            RuntimeReflection.register(cls.getDeclaredFields());
            RuntimeReflection.registerForReflectiveInstantiation(cls);
            return true;
        } catch (Throwable ignored) {
            // 可选依赖缺失（NoClassDefFoundError）时跳过
            return false;
        }
    }

    private static List<String> listClassNames(Path entry) {
        List<String> names = new ArrayList<>();
        try {
            if (Files.isDirectory(entry)) {
                try (Stream<Path> files = Files.walk(entry)) {
                    files.filter(p -> p.toString().endsWith(".class"))
                            .forEach(p -> names.add(toClassName(entry.relativize(p).toString())));
                }
            } else if (entry.toString().endsWith(".jar") && Files.isRegularFile(entry)) {
                try (JarFile jar = new JarFile(entry.toFile())) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
                            names.add(toClassName(name));
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            // 忽略不可读的 classpath 条目
        }
        return names;
    }

    private static String toClassName(String path) {
        return path.substring(0, path.length() - ".class".length()).replace('/', '.').replace('\\', '.');
    }
}
