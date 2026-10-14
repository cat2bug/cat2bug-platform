package com.cat2bug.quarkus.common.constant;

/**
 * 与 Spring {@code CacheConstants} 对齐的缓存键前缀。
 */
public final class CacheConstants {

    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /** 字典缓存 region */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /** 参数配置缓存 region */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    private CacheConstants() {
    }
}
