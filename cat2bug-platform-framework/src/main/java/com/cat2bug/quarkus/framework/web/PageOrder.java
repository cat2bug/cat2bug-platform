package com.cat2bug.quarkus.framework.web;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;

import java.util.regex.Pattern;

/**
 * 列表排序参数（与 Spring {@code PageDomain#getOrderBy} + {@code SqlUtil#escapeOrderBySql} 一致）。
 * <p>
 * Mapper XML 通过 {@code @com.cat2bug.quarkus.framework.web.PageOrder@current()} 读取当前请求的
 * {@code orderByColumn}/{@code isAsc}，结果以 {@code ${}} 拼入 ORDER BY，因此只允许白名单字符；
 * 非法值由 {@link PageOrderFilter} 在进入资源方法前拒绝。
 */
public final class PageOrder {

    public static final String PARAM_COLUMN = "orderByColumn";
    public static final String PARAM_ASC = "isAsc";
    public static final String INVALID_MESSAGE = "参数不符合规范，不能进行查询";
    public static final String TOO_LONG_MESSAGE = "参数已超过最大限制，不能进行查询";

    private static final Pattern SAFE = Pattern.compile("[a-zA-Z0-9_ ,.]+");
    private static final int MAX_LENGTH = 500;
    /** 缺陷表格自定义列对应 JSON 字段而非物理列，Spring 同样忽略。 */
    private static final String CUSTOM_FIELD_PREFIX = "custom_";

    private PageOrder() {
    }

    /** 当前请求的 ORDER BY 片段；无排序参数或参数非法时返回 null，由 SQL 使用默认排序。 */
    public static String current() {
        RoutingContext ctx = currentRoutingContext();
        if (ctx == null) {
            return null;
        }
        String orderBy = build(ctx.request().getParam(PARAM_COLUMN), ctx.request().getParam(PARAM_ASC));
        return validationError(orderBy) == null ? orderBy : null;
    }

    /**
     * 同 {@link #current()}，但把不在 SELECT 中的列名替换为可排序表达式。
     *
     * @param aliases 形如 {@code "create_member=d.create_by_id,update_by_name=u.nick_name"}
     */
    public static String current(String aliases) {
        return applyAliases(current(), aliases);
    }

    static String applyAliases(String orderBy, String aliases) {
        if (orderBy == null || aliases == null || aliases.isBlank()) {
            return orderBy;
        }
        int space = orderBy.indexOf(' ');
        String column = space < 0 ? orderBy : orderBy.substring(0, space);
        for (String pair : aliases.split(",")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).trim().equals(column)) {
                return pair.substring(eq + 1).trim() + (space < 0 ? "" : orderBy.substring(space));
            }
        }
        return orderBy;
    }

    static String build(String column, String isAsc) {
        if (column == null || column.isBlank() || column.startsWith(CUSTOM_FIELD_PREFIX)) {
            return null;
        }
        return toUnderScoreCase(column.trim()) + " " + normalizeAsc(isAsc);
    }

    /** 与 Spring {@code SqlUtil.escapeOrderBySql} 相同的校验，返回错误信息或 null。 */
    static String validationError(String orderBy) {
        if (orderBy == null) {
            return null;
        }
        if (!SAFE.matcher(orderBy).matches()) {
            return INVALID_MESSAGE;
        }
        if (orderBy.length() > MAX_LENGTH) {
            return TOO_LONG_MESSAGE;
        }
        return null;
    }

    private static String normalizeAsc(String isAsc) {
        if (isAsc == null || isAsc.isBlank()) {
            return "asc";
        }
        if ("ascending".equals(isAsc)) {
            return "asc";
        }
        if ("descending".equals(isAsc)) {
            return "desc";
        }
        return isAsc;
    }

    /** 同 Spring {@code StringUtils.toUnderScoreCase}。 */
    static String toUnderScoreCase(String str) {
        StringBuilder sb = new StringBuilder();
        boolean preCharIsUpperCase;
        boolean curreCharIsUpperCase;
        boolean nexteCharIsUpperCase = true;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            preCharIsUpperCase = i > 0 && Character.isUpperCase(str.charAt(i - 1));
            curreCharIsUpperCase = Character.isUpperCase(c);
            if (i < (str.length() - 1)) {
                nexteCharIsUpperCase = Character.isUpperCase(str.charAt(i + 1));
            }
            if (preCharIsUpperCase && curreCharIsUpperCase && !nexteCharIsUpperCase) {
                sb.append('_');
            } else if ((i != 0 && !preCharIsUpperCase) && curreCharIsUpperCase) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private static RoutingContext currentRoutingContext() {
        if (Arc.container() == null || !Arc.container().requestContext().isActive()) {
            return null;
        }
        try (InstanceHandle<CurrentVertxRequest> handle = Arc.container().instance(CurrentVertxRequest.class)) {
            return handle.isAvailable() ? handle.get().getCurrent() : null;
        }
    }
}
