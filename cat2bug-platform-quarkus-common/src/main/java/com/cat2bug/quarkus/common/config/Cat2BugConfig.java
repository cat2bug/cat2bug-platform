package com.cat2bug.quarkus.common.config;

import com.cat2bug.quarkus.common.util.StringUtils;

import java.io.File;
import java.nio.file.Path;

/**
 * 对齐 Spring Cat2BugConfig 中 Git 相关静态访问（由 {@link Cat2BugConfigHolder} 注入运行时值）。
 */
public final class Cat2BugConfig {

    private static volatile String profile = "./upload";
    private static volatile String gitRoot = "git";
    private static volatile int importTimeoutSeconds = 120;

    private Cat2BugConfig() {
    }

    public static void configure(String profilePath, String gitRootName, int importTimeout) {
        if (StringUtils.isNotEmpty(profilePath)) {
            profile = toAbsolute(profilePath);
        }
        if (StringUtils.isNotEmpty(gitRootName)) {
            gitRoot = gitRootName;
        }
        if (importTimeout > 0) {
            importTimeoutSeconds = importTimeout;
        }
    }

    public static String getProfile() {
        return profile;
    }

    public static String getGitRoot() {
        return StringUtils.isEmpty(gitRoot) ? "git" : gitRoot;
    }

    public static String getGitRootPath() {
        File root = new File(getProfile(), getGitRoot());
        return root.getAbsoluteFile().toPath().normalize().toString();
    }

    public static int getImportTimeoutSeconds() {
        return importTimeoutSeconds;
    }

    /** 兼容 Spring {@code cat2BugConfig.getGit().getImportTimeoutSeconds()} 调用形态。 */
    public Git getGit() {
        return Git.INSTANCE;
    }

    public static final class Git {
        static final Git INSTANCE = new Git();

        public int getImportTimeoutSeconds() {
            return Cat2BugConfig.getImportTimeoutSeconds();
        }
    }

    private static String toAbsolute(String path) {
        Path p = Path.of(path);
        if (p.isAbsolute()) {
            return p.normalize().toString();
        }
        return p.toAbsolutePath().normalize().toString();
    }
}
