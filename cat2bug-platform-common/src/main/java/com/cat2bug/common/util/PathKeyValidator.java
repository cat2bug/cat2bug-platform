package com.cat2bug.common.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 团队/项目路径键（teamKey / projectKey）格式与保留字校验。
 * 规则对齐仓库 slug；保留字避免与前端/API 第一路径段冲突。
 */
public final class PathKeyValidator
{
    /** 字母/数字开头结尾，中间允许字母数字与 . _ -，长度 1~100 */
    public static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9](?:[a-zA-Z0-9._-]{0,98}[a-zA-Z0-9])?$");

    private static final Set<String> RESERVED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "login", "register", "logout", "setup", "upgrade", "static", "profile", "prod-api",
            "git", "system", "project", "docs", "websocket", "captchaImage", "version", "index",
            "home", "api", "oauth", "mcp", "actuator", "error", "favicon.ico", "code",
            // SPA 第一路径段，不可作 teamKey（与前端 PATH_KEY_RESERVED 对齐）
            "invite", "redirect", "404", "401", "tools", "tool", "shard", "monitor", "admin",
            "team", "team-options",
            // /member/profile、/member/keyboard；/notice 等常量路由首段
            "member", "notice"
    )));

    private PathKeyValidator()
    {
    }

    public static Set<String> reservedWords()
    {
        return RESERVED;
    }

    public static boolean isReserved(String key)
    {
        if (key == null)
        {
            return false;
        }
        return RESERVED.contains(key.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean matchesFormat(String key)
    {
        return key != null && KEY_PATTERN.matcher(key.trim()).matches();
    }

    /**
     * @return 规范化后的 key；非法时返回 null
     */
    public static String normalizeOrNull(String key)
    {
        if (key == null)
        {
            return null;
        }
        String trimmed = key.trim();
        if (!matchesFormat(trimmed) || isReserved(trimmed))
        {
            return null;
        }
        return trimmed;
    }
}
