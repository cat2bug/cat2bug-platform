package com.cat2bug.common.ai;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AIField {
    String explain() default "";

    String maxValue() default "";

    String minValue() default "";

    boolean isRequired() default true;
}
