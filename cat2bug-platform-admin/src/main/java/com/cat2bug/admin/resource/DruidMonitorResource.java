package com.cat2bug.admin.resource;

import com.cat2bug.admin.setup.InstallEffectiveDataSource;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.framework.security.LoginUserResolver;
import com.cat2bug.framework.security.PermissionChecker;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 「系统监控 → 数据监控」iframe 页。Quarkus 使用 Agroal 连接池，此处以只读 HTML 展示连接池状态，
 * 替代 Spring 版的 Druid StatViewServlet；鉴权使用前端 Cookie {@code Admin-Token} + {@code monitor:druid:list}。
 */
@Path("/druid")
public class DruidMonitorResource {

    static final String TOKEN_COOKIE = "Admin-Token";
    static final String PERMISSION = "monitor:druid:list";

    @Inject
    LoginUserResolver loginUserResolver;

    @Inject
    PermissionChecker permissionChecker;

    @Inject
    InstallEffectiveDataSource effectiveDataSource;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response root(@CookieParam(TOKEN_COOKIE) String token) {
        return page(token);
    }

    @GET
    @Path("/{page}")
    @Produces(MediaType.TEXT_HTML)
    public Response page(@PathParam("page") String page, @CookieParam(TOKEN_COOKIE) String token) {
        return page(token);
    }

    private Response page(String token) {
        LoginUser loginUser = token == null || token.isBlank() ? null : loginUserResolver.resolveToken(token);
        if (loginUser == null) {
            return html(Response.Status.UNAUTHORIZED, "<p>登录状态已过期，请重新登录后再访问数据监控。</p>");
        }
        if (!permissionChecker.hasPermi(loginUser, PERMISSION)) {
            return html(Response.Status.FORBIDDEN, "<p>没有权限，请联系管理员授权（" + PERMISSION + "）。</p>");
        }
        return html(Response.Status.OK, renderStats());
    }

    Map<String, String> collectStats() {
        Map<String, String> stats = new LinkedHashMap<>();
        DataSource dataSource = effectiveDataSource.get();
        if (!(dataSource instanceof AgroalDataSource agroal)) {
            stats.put("连接池", dataSource == null ? "未初始化" : dataSource.getClass().getSimpleName());
            return stats;
        }
        AgroalConnectionPoolConfiguration pool = agroal.getConfiguration().connectionPoolConfiguration();
        stats.put("连接池类型", "Agroal");
        stats.put("JDBC URL", maskUrl(pool.connectionFactoryConfiguration().jdbcUrl()));
        stats.put("初始连接数", String.valueOf(pool.initialSize()));
        stats.put("最小连接数", String.valueOf(pool.minSize()));
        stats.put("最大连接数", String.valueOf(pool.maxSize()));
        stats.put("获取连接超时", formatDuration(pool.acquisitionTimeout()));
        AgroalDataSourceMetrics m = agroal.getMetrics();
        stats.put("活跃连接数", String.valueOf(m.activeCount()));
        stats.put("空闲连接数", String.valueOf(m.availableCount()));
        stats.put("活跃峰值", String.valueOf(m.maxUsedCount()));
        stats.put("等待线程数", String.valueOf(m.awaitingCount()));
        stats.put("连接获取次数", String.valueOf(m.acquireCount()));
        stats.put("物理连接创建次数", String.valueOf(m.creationCount()));
        stats.put("物理连接销毁次数", String.valueOf(m.destroyCount()));
        stats.put("无效连接次数", String.valueOf(m.invalidCount()));
        stats.put("泄漏检测次数", String.valueOf(m.leakDetectionCount()));
        stats.put("空闲回收次数", String.valueOf(m.reapCount()));
        stats.put("平均获取等待", formatDuration(m.blockingTimeAverage()));
        stats.put("最大获取等待", formatDuration(m.blockingTimeMax()));
        stats.put("平均创建耗时", formatDuration(m.creationTimeAverage()));
        stats.put("最大创建耗时", formatDuration(m.creationTimeMax()));
        return stats;
    }

    private String renderStats() {
        StringBuilder rows = new StringBuilder();
        collectStats().forEach((k, v) -> rows.append("<tr><th>").append(escape(k)).append("</th><td>")
                .append(escape(v)).append("</td></tr>"));
        return "<table>" + rows + "</table><p class=\"tip\">每 5 秒自动刷新，更新时间 "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>";
    }

    private static Response html(Response.Status status, String body) {
        String refresh = status == Response.Status.OK ? "<meta http-equiv=\"refresh\" content=\"5\">" : "";
        String doc = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" + refresh
                + "<title>数据监控</title><style>"
                + "body{font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;margin:20px;color:#303133}"
                + "h3{margin:0 0 12px}table{border-collapse:collapse;min-width:480px}"
                + "th,td{border:1px solid #ebeef5;padding:8px 12px;text-align:left;font-size:14px}"
                + "th{background:#f5f7fa;width:180px;font-weight:500}.tip{color:#909399;font-size:12px}"
                + "</style></head><body><h3>数据源连接池</h3>" + body + "</body></html>";
        return Response.status(status).entity(doc).type("text/html;charset=UTF-8").build();
    }

    static String maskUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.replaceAll("(?i)(password=)[^&;]*", "$1******");
    }

    private static String formatDuration(Duration d) {
        if (d == null) {
            return "-";
        }
        return d.toMillis() + " ms";
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
