package com.cat2bug.common.util;

import java.util.UUID;

public final class IdUtils {

    private IdUtils() {
    }

    public static String fastSimpleUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
