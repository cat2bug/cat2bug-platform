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
}
