package com.cat2bug.framework.security.open;

import com.cat2bug.framework.security.PublicPaths;

public final class OpenApiPaths {

    private OpenApiPaths() {
    }

    public static boolean isOpenApi(String path) {
        String normalized = PublicPaths.normalize(path);
        return normalized.equals("api") || normalized.startsWith("api/");
    }
}
