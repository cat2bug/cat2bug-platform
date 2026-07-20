package com.cat2bug.web.controller.git;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * tasks.md 3.5 / TESTING.md「进程约束」：Smart HTTP 与 Git 仓库磁盘路径实现不得
 * {@code ProcessBuilder}/{@code Runtime.exec} 调用系统 {@code git}（design.md D1）。
 *
 * <p>扫描本仓库中承载 Git Smart HTTP 的三处源码目录（admin 控制器、framework 安全链/过滤器、
 * system 查找/权限 Service），任何一处出现 {@code ProcessBuilder} 或
 * {@code Runtime.getRuntime().exec} 都会让本测试失败，作为回归护栏而不仅是代码评审口头约定。</p>
 */
class GitNoExternalGitProcessTest
{
    /**
     * 只匹配「真的在构造/调用」的写法，不误伤 Javadoc 里 {@code {@link ProcessBuilder}}
     * 这类解释约束用的引用（如 JGitBareRepositoryHelper 头部注释）。
     */
    private static final List<String> FORBIDDEN_TOKENS = List.of(
            "new ProcessBuilder(",
            "Runtime.getRuntime().exec("
    );

    @Test
    void gitSourceFiles_doNotSpawnExternalProcesses() throws IOException
    {
        List<Path> directories = gitSourceDirectories();
        List<Path> files = gitSourceFiles();
        assertTrue(directories.stream().anyMatch(Files::isDirectory) || files.stream().anyMatch(Files::isRegularFile),
                "至少应能定位到一个 git 相关源码文件，否则本护栏形同虚设：" + directories + " / " + files);

        List<String> violations = new ArrayList<>();
        for (Path root : directories)
        {
            if (!Files.isDirectory(root))
            {
                continue;
            }
            try (Stream<Path> walked = Files.walk(root))
            {
                walked.filter(p -> p.toString().endsWith(".java")).forEach(file -> scan(file, violations));
            }
        }
        for (Path file : files)
        {
            if (Files.isRegularFile(file))
            {
                scan(file, violations);
            }
        }

        assertTrue(violations.isEmpty(), "发现服务端 fork 系统 git 进程的痕迹：\n" + String.join("\n", violations));
    }

    private static void scan(Path file, List<String> violations)
    {
        try
        {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (String token : FORBIDDEN_TOKENS)
            {
                if (content.contains(token))
                {
                    violations.add(file + " 包含禁止的 token: " + token);
                }
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOExceptionWrapper(e);
        }
    }

    /**
     * admin 模块运行时 CWD 就是自身 basedir（Surefire 默认行为），兄弟模块用相对路径 {@code ../}。
     */
    private static List<Path> gitSourceDirectories()
    {
        Path moduleDir = new File(".").getAbsoluteFile().toPath();
        return List.of(
                moduleDir.resolve("src/main/java/com/cat2bug/web/controller/git"),
                moduleDir.resolve("../cat2bug-platform-framework/src/main/java/com/cat2bug/framework/security/git"),
                moduleDir.resolve("../cat2bug-platform-framework/src/main/java/com/cat2bug/framework/security/filter"),
                moduleDir.resolve("../cat2bug-platform-system/src/main/java/com/cat2bug/system/service/git"),
                moduleDir.resolve("../cat2bug-platform-system/src/main/java/com/cat2bug/system/service/impl/git")
        );
    }

    private static List<Path> gitSourceFiles()
    {
        Path moduleDir = new File(".").getAbsoluteFile().toPath();
        return List.of(
                moduleDir.resolve("../cat2bug-platform-framework/src/main/java/com/cat2bug/framework/config/GitSecurityConfig.java")
        );
    }

    private static final class UncheckedIOExceptionWrapper extends RuntimeException
    {
        UncheckedIOExceptionWrapper(IOException cause)
        {
            super(cause);
        }
    }
}
