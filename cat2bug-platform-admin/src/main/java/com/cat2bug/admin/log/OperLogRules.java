package com.cat2bug.admin.log;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 操作日志规则表（classpath {@code oper-log-rules.tsv}，由 Spring 控制器 {@code @Log} 注解生成），
 * 按「HTTP 方法 + 归一化路径模板」匹配资源方法。
 */
@ApplicationScoped
public class OperLogRules {

    static final String RULES_RESOURCE = "oper-log-rules.tsv";

    public record Rule(String title, int businessType, boolean saveRequest, boolean saveResponse,
                       Set<String> excludeParams) {
    }

    private final Map<String, Rule> rules;
    private final Map<Method, Optional<Rule>> byMethod = new ConcurrentHashMap<>();

    public OperLogRules() {
        this.rules = load();
    }

    public Optional<Rule> find(String httpMethod, Class<?> resourceClass, Method resourceMethod) {
        if (resourceClass == null || resourceMethod == null) {
            return Optional.empty();
        }
        return byMethod.computeIfAbsent(resourceMethod,
                m -> Optional.ofNullable(rules.get(httpMethod + " " + template(resourceClass, m))));
    }

    static String template(Class<?> resourceClass, Method method) {
        Path classPath = resourceClass.getAnnotation(Path.class);
        Path methodPath = method.getAnnotation(Path.class);
        String raw = "/" + (classPath != null ? classPath.value() : "") + "/" + (methodPath != null ? methodPath.value() : "");
        return normalize(raw);
    }

    static String normalize(String path) {
        StringBuilder out = new StringBuilder();
        int depth = 0;
        for (char c : path.toCharArray()) {
            if (c == '{') {
                if (depth++ == 0) {
                    out.append("{}");
                }
            } else if (c == '}') {
                depth--;
            } else if (depth == 0) {
                out.append(c);
            }
        }
        String p = out.toString().replaceAll("/+", "/");
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p.isEmpty() ? "/" : p;
    }

    private static Map<String, Rule> load() {
        Map<String, Rule> map = new HashMap<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream in = loader.getResourceAsStream(RULES_RESOURCE)) {
            if (in == null) {
                return map;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] f = line.split("\t", -1);
                Set<String> excludes = f.length > 6 && !f[6].isBlank() ? Set.of(f[6].split(",")) : Set.of();
                map.put(f[0] + " " + f[1], new Rule(f[2], Integer.parseInt(f[3]),
                        "1".equals(f[4]), "1".equals(f[5]), excludes));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("读取 " + RULES_RESOURCE + " 失败", e);
        }
        return map;
    }
}
