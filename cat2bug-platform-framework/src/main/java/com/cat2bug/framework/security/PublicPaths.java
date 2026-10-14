package com.cat2bug.framework.security;

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
        // 连接池监控页在 iframe 中加载，无法携带 Authorization 头，由 DruidMonitorResource 按 Cookie 自行鉴权
        if (normalized.startsWith("druid/") || "druid".equals(normalized)) {
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
        if (isInvitePublicPath(normalized)) {
            return true;
        }
        if (isReportPresetPreviewPath(normalized)) {
            return true;
        }
        if (normalized.startsWith(".well-known/")) {
            return true;
        }
        return OAUTH_PUBLIC.contains(normalized);
    }

    /** OAuth 中由 MCP 客户端直接调用的端点；{@code oauth/authorize/decision} 需网页登录 */
    private static final Set<String> OAUTH_PUBLIC = Set.of(
            "oauth/register",
            "oauth/authorize",
            "oauth/token",
            "oauth/revoke"
    );

    /** 预制报告模版预览图由 &lt;img&gt; 加载，不会带 token（对齐 Spring SecurityConfig）。 */
    private static boolean isReportPresetPreviewPath(String normalized) {
        return normalized.matches("system/report/template/preset/.+/preview");
    }

    /** 邀请预览与旧版直链 GET 允许匿名；accept 仍为 POST 需登录。 */
    private static boolean isInvitePublicPath(String normalized) {
        if (!normalized.startsWith("invite/")) {
            return false;
        }
        if (normalized.endsWith("/preview")) {
            return normalized.matches("invite/[^/]+/preview");
        }
        return normalized.matches("invite/[^/]+");
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
