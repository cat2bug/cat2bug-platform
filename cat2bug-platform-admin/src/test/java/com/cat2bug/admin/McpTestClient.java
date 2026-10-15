package com.cat2bug.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.response.Response;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;

/**
 * 测试用最小 MCP 客户端：Streamable HTTP + JSON-RPC，兼容 JSON 与 SSE 两种响应。
 */
final class McpTestClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String bearer;
    private final AtomicInteger ids = new AtomicInteger();
    private String sessionId;
    private JsonNode initializeResult;

    McpTestClient(String bearer) {
        this.bearer = bearer;
    }

    McpTestClient initialize(String clientName) {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("protocolVersion", "2025-03-26");
        params.putObject("capabilities");
        params.putObject("clientInfo").put("name", clientName).put("version", "1.0");
        Response response = send("initialize", params, true);
        sessionId = response.header("Mcp-Session-Id");
        initializeResult = parse(response).path("result");
        ObjectNode notify = MAPPER.createObjectNode();
        notify.put("jsonrpc", "2.0");
        notify.put("method", "notifications/initialized");
        request().body(notify.toString()).post("/mcp");
        return this;
    }

    JsonNode listTools() {
        return parse(send("tools/list", MAPPER.createObjectNode(), false)).path("result").path("tools");
    }

    JsonNode initializeResult() {
        return initializeResult;
    }

    JsonNode listPrompts() {
        return parse(send("prompts/list", MAPPER.createObjectNode(), false)).path("result").path("prompts");
    }

    /** 返回第一条消息的文本。 */
    String getPrompt(String name, Map<String, String> arguments) {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("name", name);
        params.set("arguments", MAPPER.valueToTree(arguments));
        JsonNode root = parse(send("prompts/get", params, false));
        if (root.has("error")) {
            throw new AssertionError("JSON-RPC 错误：" + root.get("error"));
        }
        return root.path("result").path("messages").path(0).path("content").path("text").asText();
    }

    /** 返回 {@code result}；业务失败时 {@code result.isError=true}。 */
    JsonNode callTool(String name, Map<String, ?> arguments) {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("name", name);
        params.set("arguments", MAPPER.valueToTree(arguments));
        JsonNode root = parse(send("tools/call", params, false));
        if (root.has("error")) {
            throw new AssertionError("JSON-RPC 错误：" + root.get("error"));
        }
        return root.path("result");
    }

    static String text(JsonNode result) {
        return result.path("content").path(0).path("text").asText();
    }

    static JsonNode json(JsonNode result) {
        try {
            return MAPPER.readTree(text(result));
        } catch (Exception e) {
            throw new AssertionError("工具结果不是 JSON：" + text(result), e);
        }
    }

    private Response send(String method, JsonNode params, boolean init) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("jsonrpc", "2.0");
        body.put("id", ids.incrementAndGet());
        body.put("method", method);
        body.set("params", params);
        Response response = request().body(body.toString()).post("/mcp");
        if (response.statusCode() != 200) {
            throw new AssertionError(method + " 返回 HTTP " + response.statusCode() + "：" + response.asString());
        }
        return response;
    }

    private io.restassured.specification.RequestSpecification request() {
        var spec = given()
                .header("Authorization", "Bearer " + bearer)
                .header("Accept", "application/json, text/event-stream")
                .contentType("application/json");
        if (sessionId != null) {
            spec.header("Mcp-Session-Id", sessionId);
        }
        return spec;
    }

    private static JsonNode parse(Response response) {
        String raw = response.asString();
        String contentType = response.contentType();
        try {
            if (contentType != null && contentType.startsWith("text/event-stream")) {
                JsonNode last = null;
                for (String line : raw.split("\n")) {
                    if (line.startsWith("data:")) {
                        JsonNode node = MAPPER.readTree(line.substring(5).trim());
                        if (node.has("id")) {
                            last = node;
                        }
                    }
                }
                if (last == null) {
                    throw new AssertionError("SSE 中没有响应：" + raw);
                }
                return last;
            }
            return MAPPER.readTree(raw);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError("无法解析 MCP 响应：" + raw, e);
        }
    }
}
