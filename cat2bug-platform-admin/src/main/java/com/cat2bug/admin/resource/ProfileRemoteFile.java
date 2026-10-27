package com.cat2bug.admin.resource;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 本地 profile 目录没有文件时，按配置的站点根地址去取同一条 /profile 路径。 */
public final class ProfileRemoteFile {

    private ProfileRemoteFile() {
    }

    public static URI uri(String remoteBase, String filePath) {
        if (remoteBase == null || filePath == null) {
            return null;
        }
        String base = remoteBase.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            return null;
        }
        String path = filePath.replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.isBlank() || path.contains("..")) {
            return null;
        }
        StringBuilder remote = new StringBuilder(base).append("/profile");
        for (String segment : path.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            // URLEncoder 按表单编码空格为 +，URI 路径需用 %20
            remote.append('/').append(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        try {
            return URI.create(remote.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
