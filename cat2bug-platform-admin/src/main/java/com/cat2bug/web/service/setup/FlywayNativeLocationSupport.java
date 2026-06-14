package com.cat2bug.web.service.setup;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * GraalVM Native 下 Flyway {@code ClassPathScanner} 无法使用 {@code resource:} 协议扫描脚本，
 * 需先将 classpath 中的 SQL 物化到本地目录，再使用 {@code filesystem:} 位置。
 */
final class FlywayNativeLocationSupport
{
    private FlywayNativeLocationSupport()
    {
    }

    static String resolveMigrationLocation(String databaseType) throws IOException
    {
        String classpathDir = "db/migration/" + databaseType;
        if (!runningNativeImage())
        {
            return "classpath:" + classpathDir;
        }
        return "filesystem:" + materializeClasspathMigrations(classpathDir);
    }

    private static boolean runningNativeImage()
    {
        return System.getProperty("org.graalvm.nativeimage.imagecode") != null;
    }

    private static Path materializeClasspathMigrations(String classpathDir) throws IOException
    {
        Path root = Path.of(System.getProperty("java.io.tmpdir"), "cat2bug-flyway", classpathDir.replace('/', '_'));
        Files.createDirectories(root);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:" + classpathDir + "/*.sql");
        if (resources.length == 0)
        {
            throw new IOException("classpath 上未找到 Flyway 迁移脚本: " + classpathDir);
        }
        for (Resource resource : resources)
        {
            String filename = resource.getFilename();
            if (filename == null || filename.isBlank())
            {
                continue;
            }
            Path target = root.resolve(filename);
            try (InputStream in = resource.getInputStream())
            {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return root;
    }
}
