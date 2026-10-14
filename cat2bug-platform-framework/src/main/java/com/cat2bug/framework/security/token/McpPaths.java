package com.cat2bug.framework.security.token;

/**
 * MCP 端点路径。个人访问令牌与 OAuth 访问令牌只在这些路径上生效。
 */
public final class McpPaths {

    public static final String ROOT = "/mcp";

    private McpPaths() {
    }

    public static boolean isMcp(String path) {
        if (path == null) {
            return false;
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        return normalized.equals(ROOT) || normalized.startsWith(ROOT + "/");
    }
}
