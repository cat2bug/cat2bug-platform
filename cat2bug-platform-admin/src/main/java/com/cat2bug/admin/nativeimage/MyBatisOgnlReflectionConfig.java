package com.cat2bug.admin.nativeimage;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * MyBatis XML 的 OGNL 表达式（如 {@code list.size() > 0}）靠反射调用集合方法，
 * Native 下 JDK 集合类默认未注册，会抛 {@code NoSuchMethodException: java.util.ArrayList.size()}。
 */
@RegisterForReflection(targets = {
        java.util.ArrayList.class,
        java.util.LinkedList.class,
        java.util.HashSet.class,
        java.util.LinkedHashSet.class,
        java.util.HashMap.class,
        java.util.LinkedHashMap.class
}, classNames = {
        "java.util.Arrays$ArrayList",
        "java.util.ImmutableCollections$List12",
        "java.util.ImmutableCollections$ListN",
        "java.util.Collections$UnmodifiableRandomAccessList",
        "java.util.Collections$SingletonList",
        "java.util.Collections$EmptyList"
})
public final class MyBatisOgnlReflectionConfig {

    private MyBatisOgnlReflectionConfig() {
    }
}
