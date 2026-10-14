package com.cat2bug.admin.service;

import com.cat2bug.common.api.WebSocketResult;
import com.cat2bug.common.websocket.MessageWebsocketPush;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 用本地 HTTP 桩模拟 Ollama，验证启动时默认模型的检查与拉取。 */
class OllamaDefaultModelBootstrapTest {

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<String> pullBodies = new CopyOnWriteArrayList<>();
    private final CountDownLatch pulled = new CountDownLatch(1);

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void pullsMissingDefaultModel() throws Exception {
        String host = startStub("{\"models\":[{\"name\":\"llava:7b\",\"size\":1}]}");
        OllamaDefaultModelBootstrap bootstrap = bootstrap(true, host, "qwen2.5:7b");

        assertEquals(List.of("qwen2.5:7b"), bootstrap.ensureDefaultModels(List.of("qwen2.5:7b")));
        assertTrue(pulled.await(5, TimeUnit.SECONDS));
        assertTrue(pullBodies.get(0).contains("\"name\":\"qwen2.5:7b\""));
    }

    @Test
    void skipsExistingModelIncludingLatestTag() throws Exception {
        String host = startStub("{\"models\":[{\"name\":\"qwen2.5:7b\"},{\"name\":\"llama3:latest\"}]}");
        OllamaDefaultModelBootstrap bootstrap = bootstrap(true, host, "qwen2.5:7b");

        assertEquals(List.of(), bootstrap.ensureDefaultModels(List.of("qwen2.5:7b", "llama3")));
        assertEquals(List.of("GET /api/tags"), requests);
    }

    @Test
    void unreachableOllamaOnlyWarns() throws Exception {
        HttpServer probe = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int closedPort = probe.getAddress().getPort();
        probe.stop(0);
        OllamaDefaultModelBootstrap bootstrap = bootstrap(true, "http://127.0.0.1:" + closedPort, "qwen2.5:7b");

        assertEquals(List.of(), bootstrap.ensureDefaultModels(List.of("qwen2.5:7b")));
    }

    @Test
    void disabledAiNeverContactsOllama() throws Exception {
        String host = startStub("{\"models\":[]}");
        OllamaDefaultModelBootstrap bootstrap = bootstrap(false, host, "qwen2.5:7b");

        bootstrap.onStart(null);
        Thread.sleep(300);
        assertEquals(List.of(), requests);
    }

    private String startStub(String tagsJson) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/tags", exchange -> {
            requests.add(exchange.getRequestMethod() + " /api/tags");
            respond(exchange, tagsJson);
        });
        server.createContext("/api/pull", exchange -> {
            requests.add(exchange.getRequestMethod() + " /api/pull");
            pullBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, "{\"status\":\"success\"}\n");
            pulled.countDown();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static OllamaDefaultModelBootstrap bootstrap(boolean enabled, String host, String defaultModel) {
        ObjectMapper objectMapper = new ObjectMapper();
        OllamaAvailabilityService availability = new OllamaAvailabilityService();
        availability.enabled = enabled;
        availability.host = Optional.of(host);
        availability.defaultBusinessModel = Optional.of(defaultModel);
        availability.models = Optional.empty();
        availability.objectMapper = objectMapper;

        OllamaModelService modelService = new OllamaModelService();
        modelService.ollamaAvailability = availability;
        modelService.objectMapper = objectMapper;
        modelService.timeoutSeconds = 5;
        modelService.messageWebsocketPush = new MessageWebsocketPush() {
            @Override
            public void sendToAll(WebSocketResult result) {
            }

            @Override
            public void sendToMember(Long memberId, WebSocketResult result) {
            }

            @Override
            public boolean isMemberConnected(Long memberId) {
                return false;
            }
        };

        OllamaDefaultModelBootstrap bootstrap = new OllamaDefaultModelBootstrap();
        bootstrap.ollamaAvailability = availability;
        bootstrap.ollamaModelService = modelService;
        return bootstrap;
    }
}
