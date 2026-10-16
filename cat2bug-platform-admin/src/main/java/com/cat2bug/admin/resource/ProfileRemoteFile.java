package com.cat2bug.admin.resource;

import java.net.URI;

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
        return URI.create(base + "/profile/" + path);
    }
}
