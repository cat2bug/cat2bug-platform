package com.cat2bug.framework.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 等价于 Spring {@code @PreAuthorize("@ss.hasPermi('…')")} 的简化版。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermissions {

    String[] value();

    Logical logical() default Logical.AND;

    enum Logical {
        AND,
        OR
    }
}
