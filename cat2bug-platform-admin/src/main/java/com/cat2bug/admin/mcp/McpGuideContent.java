package com.cat2bug.admin.mcp;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 使用指南与引导命令的正文，来自 {@code mcp-guide/<语言>/}；新增目录须加入 {@code quarkus.native.resources.includes}。
 */
@ApplicationScoped
public class McpGuideContent {

    static final String LOCALE = "zh-CN";
    static final String MISSING_ARG = "（未提供）";

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /** 主题正文，如 {@code defect}。 */
    public String topic(String topic) {
        return load(topic + ".md");
    }

    /** 引导命令正文，{@code {{name}}} 替换为参数值，空值替换为 {@link #MISSING_ARG}。 */
    public String prompt(String name, Map<String, String> args) {
        String text = load("prompts/" + name + ".md");
        for (Map.Entry<String, String> e : args.entrySet()) {
            String v = e.getValue() == null || e.getValue().isBlank() ? MISSING_ARG : e.getValue().trim();
            text = text.replace("{{" + e.getKey() + "}}", v);
        }
        return text;
    }

    private String load(String relative) {
        return cache.computeIfAbsent(relative, r -> {
            String path = "mcp-guide/" + LOCALE + "/" + r;
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException("缺少 MCP 指南资源：" + path);
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
