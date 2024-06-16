package com.cat2bug.ai.annotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI解析用注解
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AIField {
    /** 说明 */
    public String explain() default "";
    /** 最大值 */
    public String maxValue() default "";
    /** 最小值 */
    public String minValue() default "";
    /** 是否必填 */
    public boolean isRequired() default true;
}
