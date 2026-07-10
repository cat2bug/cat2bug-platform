package com.cat2bug.web.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 根据当前请求构造邀请链接根地址（scheme + host + port）。
 */
public final class InviteLinkUrlSupport {

    private InviteLinkUrlSupport() {
    }

    public static String resolveInviteBaseUrl(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80)
                || ("https".equalsIgnoreCase(scheme) && serverPort == 443);
        if (defaultPort) {
            return scheme + "://" + serverName;
        }
        return scheme + "://" + serverName + ":" + serverPort;
    }

    /** 内嵌 SPA（hash 路由）邀请页完整 URL */
    public static String buildInvitePageUrl(String inviteBaseUrl, String token) {
        if (inviteBaseUrl == null || token == null || token.isEmpty()) {
            return null;
        }
        String base = inviteBaseUrl.endsWith("/") ? inviteBaseUrl.substring(0, inviteBaseUrl.length() - 1) : inviteBaseUrl;
        return base + "/#/invite/" + token;
    }
}
