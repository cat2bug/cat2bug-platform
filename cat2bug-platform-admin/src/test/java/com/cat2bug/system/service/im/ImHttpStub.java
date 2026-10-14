package com.cat2bug.system.service.im;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 本地 HTTP 桩：按路径排队预设响应，记录收到的请求。
 */
final class ImHttpStub implements AutoCloseable {

    record Recorded(String method, String path, String query, Map<String, List<String>> headers, String body) {
        String header(String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name) && !e.getValue().isEmpty()) {
                    return e.getValue().get(0);
                }
            }
            return null;
        }
    }

    private record Reply(int status, String body) {
    }

    private final HttpServer server;
    private final Map<String, Deque<Reply>> replies = new ConcurrentHashMap<>();
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();

    ImHttpStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String path = exchange.getRequestURI().getPath();
            requests.add(new Recorded(exchange.getRequestMethod(), path, exchange.getRequestURI().getRawQuery(),
                    Map.copyOf(exchange.getRequestHeaders()), body));
            Reply reply = next(path);
            byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** 同一路径可多次调用，按顺序消费；最后一个响应会被重复使用。 */
    ImHttpStub reply(String path, int status, String body) {
        replies.computeIfAbsent(path, k -> new ArrayDeque<>()).add(new Reply(status, body));
        return this;
    }

    List<Recorded> requests() {
        return requests;
    }

    List<Recorded> requests(String path) {
        return requests.stream().filter(r -> r.path().equals(path)).toList();
    }

    private synchronized Reply next(String path) {
        Deque<Reply> queue = replies.get(path);
        if (queue == null || queue.isEmpty()) {
            return new Reply(404, "{\"errcode\":404}");
        }
        return queue.size() > 1 ? queue.poll() : queue.peek();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
