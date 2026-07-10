package com.cat2bug.quarkus.framework.security;

import java.util.Set;

/**
 * 与 Spring {@code SecurityConfig} permitAll 路径对齐（Phase 1 子集）。
 */
public final class PublicPaths {

    private static final Set<String> EXACT = Set.of(
            "",
            "login",
            "logout",
            "register",
            "version",
            "captchaImage"
    );

    private PublicPaths() {
    }

    public static boolean isPublic(String path) {
        if (path == null || path.isEmpty()) {
            return true;
        }
        String normalized = normalize(path);
        if (EXACT.contains(normalized)) {
            return true;
        }
        if (normalized.startsWith("setup/") || "setup".equals(normalized)) {
            return true;
        }
        if (normalized.startsWith("upgrade/") || "upgrade".equals(normalized)) {
            return true;
        }
        if (normalized.startsWith("q/health")) {
            return true;
        }
        if (normalized.startsWith("websocket/")) {
            return true;
        }
        if (normalized.startsWith("profile/")) {
            return true;
        }
        if (isDefectShardPath(normalized)) {
            return true;
        }
        if (normalized.startsWith("invite/") && normalized.endsWith("/preview")) {
            return true;
        }
        return false;
    }

    private static boolean isDefectShardPath(String normalized) {
        return normalized.matches("system/defect/[^/]+/shard(/.*)?");
    }

    public static String normalize(String path) {
        if (path == null) {
            return "";
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
