package com.cat2bug.web.service.setup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 安装/升级完成后尝试拉起新的 JVM 进程（非 Docker 场景）；Docker 依赖容器 restart 策略。
 */
final class ApplicationSelfRestarter
{
    private static final Logger log = LoggerFactory.getLogger(ApplicationSelfRestarter.class);

    private static final String MAIN_CLASS = "com.cat2bug.Cat2BugApplication";

    private ApplicationSelfRestarter()
    {
    }

    static boolean spawn(String[] applicationArgs)
    {
        if (isSelfRestartDisabled())
        {
            log.info("已禁用自拉起（CAT2BUG_DISABLE_SELF_RESTART），依赖外部进程管理器重启");
            return false;
        }
        if (isDockerEnvironment())
        {
            log.info("检测到容器环境，进程退出后由 Docker restart 策略拉起");
            return false;
        }
        if (spawnJarProcess(applicationArgs))
        {
            return true;
        }
        return spawnClasspathProcess(applicationArgs);
    }

    private static boolean isSelfRestartDisabled()
    {
        return "true".equalsIgnoreCase(System.getenv("CAT2BUG_DISABLE_SELF_RESTART"));
    }

    private static boolean isDockerEnvironment()
    {
        if ("true".equalsIgnoreCase(System.getenv("CAT2BUG_IN_DOCKER")))
        {
            return true;
        }
        return Files.exists(Paths.get("/.dockerenv"));
    }

    private static boolean spawnJarProcess(String[] applicationArgs)
    {
        Optional<Path> jar = findExecutableJar();
        if (jar.isEmpty())
        {
            return false;
        }
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.addAll(relevantJvmArguments());
        command.add("-jar");
        command.add(jar.get().toAbsolutePath().normalize().toString());
        appendApplicationArgs(command, applicationArgs);
        return startProcess(command, "jar");
    }

    private static boolean spawnClasspathProcess(String[] applicationArgs)
    {
        String classpath = System.getProperty("java.class.path");
        if (classpath == null || classpath.isBlank())
        {
            return false;
        }
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.addAll(relevantJvmArguments());
        command.add("-cp");
        command.add(classpath);
        command.add(MAIN_CLASS);
        appendApplicationArgs(command, applicationArgs);
        return startProcess(command, "classpath");
    }

    private static Optional<Path> findExecutableJar()
    {
        String classpath = System.getProperty("java.class.path", "");
        Path selected = null;
        for (String entry : classpath.split(File.pathSeparator))
        {
            if (!entry.endsWith(".jar"))
            {
                continue;
            }
            Path path = Paths.get(entry);
            String name = path.getFileName().toString().toLowerCase();
            if (name.contains("devtools") || name.startsWith("spring-boot-starter"))
            {
                continue;
            }
            if (name.contains("cat2bug") || name.contains("admin"))
            {
                selected = path;
            }
        }
        return Optional.ofNullable(selected);
    }

    private static List<String> relevantJvmArguments()
    {
        List<String> args = new ArrayList<>();
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments())
        {
            if (arg.startsWith("-agentlib:") || arg.startsWith("-javaagent:") || arg.contains("jdwp"))
            {
                args.add(arg);
            }
        }
        return args;
    }

    private static void appendApplicationArgs(List<String> command, String[] applicationArgs)
    {
        if (applicationArgs == null)
        {
            return;
        }
        for (String arg : applicationArgs)
        {
            if (arg != null && !arg.isBlank())
            {
                command.add(arg);
            }
        }
    }

    private static String javaExecutable()
    {
        Path javaHome = Paths.get(System.getProperty("java.home"));
        Path bin = javaHome.resolve("bin").resolve(isWindows() ? "java.exe" : "java");
        return bin.toAbsolutePath().normalize().toString();
    }

    private static boolean isWindows()
    {
        return File.separatorChar == '\\';
    }

    private static boolean startProcess(List<String> command, String mode)
    {
        try
        {
            new ProcessBuilder(command)
                    .directory(new File(System.getProperty("user.dir")))
                    .inheritIO()
                    .start();
            log.info("已启动新进程（{} 模式）: {}", mode, String.join(" ", command));
            return true;
        }
        catch (IOException ex)
        {
            log.warn("自拉起新进程失败（{} 模式）: {}", mode, ex.getMessage());
            return false;
        }
    }
}
