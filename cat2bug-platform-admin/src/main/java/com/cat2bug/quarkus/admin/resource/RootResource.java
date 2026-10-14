package com.cat2bug.quarkus.admin.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * 无 embedded 前端时的 API 说明；Accept 不含 text/html 的 GET / 仍返回 JSON。
 * 内嵌 SPA 存在时，{@link com.cat2bug.quarkus.admin.config.EmbeddedSpaFallbackFilter} 处理浏览器 GET /。
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class RootResource {

    @GET
    public Map<String, Object> root() {
        return Map.of(
                "code", 200,
                "msg", "Cat2Bug Quarkus API",
                "data", Map.of(
                        "ui", "完整界面：npm run dev（:2222）；或 deploy/build-embedded.sh 单 JAR",
                        "endpoints", List.of(
                                "GET /version",
                                "POST /login",
                                "POST /logout",
                                "GET /getInfo",
                                "GET /getRouters",
                                "GET /system/defect/list?projectId=1"
                        )
                )
        );
    }
}
