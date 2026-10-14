package com.cat2bug.admin.setup;

import com.cat2bug.common.config.InstallConfigSupport;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 安装/升级完成后尝试拉起新的 JVM 进程（对齐 Spring {@code ApplicationSelfRestarter}）。
 */
final class QuarkusApplicationSelfRestarter {

    private static final Logger log = Logger.getLogger(QuarkusApplicationSelfRestarter.class);

    private QuarkusApplicationSelfRestarter() {
    }

    static boolean spawn(String configPath) {
        if (isSelfRestartDisabled()) {
            log.info("已禁用自拉起（CAT2BUG_DISABLE_SELF_RESTART），依赖外部进程管理器重启");
            return false;
        }
        if (isDockerEnvironment()) {
            log.info("检测到容器环境，进程退出后由 Docker restart 策略拉起");
            return false;
        }
        Optional<String> configLocations = buildConfigLocations(configPath);
        return spawnJarProcess(configLocations.orElse(null));
    }

    private static boolean isSelfRestartDisabled() {
        return "true".equalsIgnoreCase(System.getenv("CAT2BUG_DISABLE_SELF_RESTART"));
    }

    private static boolean isDockerEnvironment() {
        if ("true".equalsIgnoreCase(System.getenv("CAT2BUG_IN_DOCKER"))) {
            return true;
        }
        return Files.exists(Paths.get("/.dockerenv"));
    }

    private static Optional<String> buildConfigLocations(String configPath) {
        List<String> locations = new ArrayList<>();
        InstallConfigSupport.locateExistingConfigFile(configPath).ifPresent(path ->
                locations.add(path.toUri().toString()));
        Path runtimeProps = InstallConfigSupport.resolveWorkingDirectoryConfigPath(configPath)
                .getParent()
                .resolve("quarkus-runtime.properties");
        if (Files.isRegularFile(runtimeProps)) {
            locations.add(runtimeProps.toUri().toString());
        }
        if (locations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(",", locations));
    }

    private static boolean spawnJarProcess(String configLocations) {
        Optional<Path> jar = findExecutableJar();
        if (jar.isEmpty()) {
            log.warn("未找到可执行 JAR，无法自拉起");
            return false;
        }
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.addAll(relevantJvmArguments());
        appendSystemProperty(command, "quarkus.http.port");
        appendSystemProperty(command, "quarkus.http.host");
        if (configLocations != null && !configLocations.isBlank()) {
            command.add("-Dquarkus.config.locations=" + configLocations);
        }
        command.add("-jar");
        command.add(jar.get().toAbsolutePath().normalize().toString());
        return startProcess(command);
    }

    private static void appendSystemProperty(List<String> command, String key) {
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            command.add("-D" + key + "=" + value);
        }
    }

    private static Optional<Path> findExecutableJar() {
        String javaCommand = System.getProperty("sun.java.command", "");
        int jarIndex = javaCommand.indexOf(".jar");
        if (jarIndex > 0) {
            String jarToken = javaCommand.substring(0, jarIndex + 4).trim();
            int space = jarToken.lastIndexOf(' ');
            if (space >= 0) {
                jarToken = jarToken.substring(space + 1).trim();
            }
            Path path = Paths.get(jarToken);
            if (Files.isRegularFile(path)) {
                return Optional.of(path.toAbsolutePath().normalize());
            }
        }
        String classpath = System.getProperty("java.class.path", "");
        Path selected = null;
        for (String entry : classpath.split(File.pathSeparator)) {
            if (!entry.endsWith(".jar")) {
                continue;
            }
            Path path = Paths.get(entry);
            String name = path.getFileName().toString().toLowerCase();
            if (name.contains("quarkus") && name.contains("core")) {
                continue;
            }
            if (name.contains("cat2bug") || name.contains("admin") || name.contains("runner")) {
                selected = path;
            }
        }
        return Optional.ofNullable(selected);
    }

    private static List<String> relevantJvmArguments() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(arg -> arg.startsWith("-agentlib:")
                        || arg.startsWith("-javaagent:")
                        || arg.contains("jdwp"))
                .collect(Collectors.toList());
    }

    private static String javaExecutable() {
        Path javaHome = Paths.get(System.getProperty("java.home"));
        Path bin = javaHome.resolve("bin").resolve(isWindows() ? "java.exe" : "java");
        return bin.toAbsolutePath().normalize().toString();
    }

    private static boolean isWindows() {
        return File.separatorChar == '\\';
    }

    private static boolean startProcess(List<String> command) {
        try {
            new ProcessBuilder(command)
                    .directory(new File(System.getProperty("user.dir")))
                    .inheritIO()
                    .start();
            log.infof("已启动新进程: %s", String.join(" ", command));
            return true;
        } catch (IOException ex) {
            log.warnf("自拉起新进程失败: %s", ex.getMessage());
            return false;
        }
    }
}
