package com.cat2bug.framework.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 当前请求的客户端信息（IP、浏览器、操作系统），以及 IP 归属地查询。
 */
public record ClientInfo(String ip, String browser, String os) {

    private static final Logger LOG = Logger.getLogger(ClientInfo.class);

    public static final String INTERNAL_ADDRESS = "内网IP";
    public static final String UNKNOWN_ADDRESS = "XX XX";
    private static final String UNKNOWN = "Unknown";
    private static final String IP_URL = "http://whois.pconline.com.cn/ipJson.jsp";

    private static final Pattern EDGE = Pattern.compile("Edg(?:e|A|iOS)?/(\\d+)");
    private static final Pattern OPERA = Pattern.compile("(?:OPR|Opera)/(\\d+)");
    private static final Pattern FIREFOX = Pattern.compile("(?:Firefox|FxiOS)/(\\d+)");
    private static final Pattern CHROME = Pattern.compile("(?:Chrome|CriOS)/(\\d+)");
    private static final Pattern SAFARI = Pattern.compile("Version/(\\d+).*Safari/");
    private static final Pattern IE = Pattern.compile("(?:MSIE |Trident/.*rv:)(\\d+)");
    private static final Pattern WINDOWS_NT = Pattern.compile("Windows NT (\\d+\\.\\d+)");

    public static ClientInfo current() {
        RoutingContext ctx = currentRoutingContext();
        if (ctx == null) {
            return new ClientInfo("127.0.0.1", UNKNOWN, UNKNOWN);
        }
        HttpServerRequest request = ctx.request();
        String ua = request.getHeader("User-Agent");
        return new ClientInfo(resolveIp(request), parseBrowser(ua), parseOs(ua));
    }

    private static RoutingContext currentRoutingContext() {
        if (Arc.container() == null || !Arc.container().requestContext().isActive()) {
            return null;
        }
        try (InstanceHandle<CurrentVertxRequest> handle = Arc.container().instance(CurrentVertxRequest.class)) {
            return handle.isAvailable() ? handle.get().getCurrent() : null;
        }
    }

    static String resolveIp(HttpServerRequest request) {
        String ip = null;
        for (String header : new String[]{"X-Forwarded-For", "Proxy-Client-IP", "X-Real-IP", "WL-Proxy-Client-IP"}) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
            ip = null;
        }
        if (ip == null) {
            SocketAddress remote = request.remoteAddress();
            ip = remote != null ? remote.hostAddress() : null;
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(',')).trim();
        }
        if (ip == null || ip.isBlank()) {
            return "127.0.0.1";
        }
        return "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip) ? "127.0.0.1" : ip;
    }

    static String parseBrowser(String ua) {
        if (ua == null || ua.isBlank()) {
            return UNKNOWN;
        }
        String version;
        if ((version = firstGroup(EDGE, ua)) != null) {
            return "Microsoft Edge " + version;
        }
        if ((version = firstGroup(OPERA, ua)) != null) {
            return "Opera " + version;
        }
        if ((version = firstGroup(FIREFOX, ua)) != null) {
            return "Firefox " + version;
        }
        if ((version = firstGroup(CHROME, ua)) != null) {
            return "Chrome " + version;
        }
        if ((version = firstGroup(SAFARI, ua)) != null) {
            return "Safari " + version;
        }
        if ((version = firstGroup(IE, ua)) != null) {
            return "Internet Explorer " + version;
        }
        return UNKNOWN;
    }

    static String parseOs(String ua) {
        if (ua == null || ua.isBlank()) {
            return UNKNOWN;
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) {
            return "iOS";
        }
        String nt = firstGroup(WINDOWS_NT, ua);
        if (nt != null) {
            return switch (nt) {
                case "10.0" -> "Windows 10";
                case "6.3" -> "Windows 8.1";
                case "6.2" -> "Windows 8";
                case "6.1" -> "Windows 7";
                default -> "Windows";
            };
        }
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
            return "Mac OS X";
        }
        if (ua.contains("CrOS")) {
            return "Chrome OS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return UNKNOWN;
    }

    public static boolean isInternalIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        if ("127.0.0.1".equals(ip) || "localhost".equalsIgnoreCase(ip) || ip.startsWith("127.")) {
            return true;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return ip.contains(":") && (ip.startsWith("fc") || ip.startsWith("fd") || ip.startsWith("fe80"));
        }
        try {
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1]);
            return a == 10 || (a == 172 && b >= 16 && b <= 31) || (a == 192 && b == 168);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 内网直接返回「内网IP」；仅当 {@code cat2bug.addressEnabled=true} 时才联网查询。
     */
    public static String resolveLocation(String ip) {
        if (isInternalIp(ip)) {
            return INTERNAL_ADDRESS;
        }
        boolean enabled = ConfigProvider.getConfig()
                .getOptionalValue("cat2bug.addressEnabled", Boolean.class).orElse(false);
        if (!enabled) {
            return UNKNOWN_ADDRESS;
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            IP_URL + "?ip=" + URLEncoder.encode(ip, StandardCharsets.UTF_8) + "&json=true"))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            byte[] body = client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
            JsonNode node = new ObjectMapper().readTree(new String(body, Charset.forName("GBK")));
            return String.format("%s %s", node.path("pro").asText(""), node.path("city").asText(""));
        } catch (Exception e) {
            LOG.warnf("获取地理位置异常 %s: %s", ip, e.getMessage());
            return UNKNOWN_ADDRESS;
        }
    }

    private static String firstGroup(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }
}
