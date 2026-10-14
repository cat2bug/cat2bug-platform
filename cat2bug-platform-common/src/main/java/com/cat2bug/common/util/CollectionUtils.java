package com.cat2bug.common.util;

import java.util.Collection;
import java.util.Map;

/** 对齐 Spring {@code CollectionUtils} 的最小子集。 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }
}
