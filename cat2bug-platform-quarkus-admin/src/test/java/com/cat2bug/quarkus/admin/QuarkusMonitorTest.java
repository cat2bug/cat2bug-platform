package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class QuarkusMonitorTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void onlineUsersIncludeCurrentSession() {
        given().header("Authorization", token)
                .when().get("/monitor/online/list")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThan(0))
                .body("rows[0].userName", notNullValue())
                .body("rows[0].ipaddr", notNullValue());
    }

    @Test
    void forceLogoutRemovesSession() {
        List<String> before = given().header("Authorization", token)
                .when().get("/monitor/online/list")
                .then().statusCode(200)
                .extract().path("rows.tokenId");
        String other = QuarkusTestAuth.loginAsAdmin();
        List<String> after = given().header("Authorization", token)
                .when().get("/monitor/online/list")
                .then().statusCode(200)
                .extract().path("rows.tokenId");
        String otherTokenId = after.stream().filter(id -> !before.contains(id)).findFirst().orElseThrow();
        given().header("Authorization", token)
                .when().delete("/monitor/online/" + otherTokenId)
                .then().statusCode(200).body("code", equalTo(200));
        given().header("Authorization", other)
                .when().get("/getInfo")
                .then().body("code", equalTo(401));
    }

    @Test
    void serverInfo() {
        given().header("Authorization", token)
                .when().get("/monitor/server")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.cpu.cpuNum", greaterThan(0))
                .body("data.jvm.version", notNullValue())
                .body("data.sys.osName", notNullValue());
    }

    @Test
    void cacheEndpoints() {
        given().header("Authorization", token)
                .when().get("/monitor/cache")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.info", notNullValue());
        given().header("Authorization", token)
                .when().get("/monitor/cache/getNames")
                .then().statusCode(200)
                .body("data.cacheName", org.hamcrest.Matchers.hasItem("login_tokens:"));
        given().header("Authorization", token)
                .when().get("/monitor/cache/getKeys/login_tokens:")
                .then().statusCode(200)
                .body("data.size()", greaterThan(0));
        given().header("Authorization", token)
                .when().delete("/monitor/cache/clearCacheName/sys_config:")
                .then().statusCode(200).body("code", equalTo(200));
    }

    @Test
    void druidPagePoolStats() {
        given().accept("text/html")
                .when().get("/druid/login.html")
                .then().statusCode(401);
        given().accept("text/html")
                .cookie("Admin-Token", token)
                .when().get("/druid/login.html")
                .then().statusCode(200)
                .body(containsString("最大连接数"))
                .body(containsString("Agroal"));
    }

    @Test
    void operLogRecordsAnnotatedEndpoints() throws Exception {
        String dictType = "oplog_" + System.nanoTime();
        given().header("Authorization", token)
                .contentType("application/json")
                .body(Map.of("dictName", "操作日志测试", "dictType", dictType, "status", "0"))
                .when().post("/system/dict/type")
                .then().statusCode(200).body("code", equalTo(200));
        given().header("Authorization", token)
                .contentType("application/x-www-form-urlencoded")
                .formParam("configName", "oplog-form")
                .when().post("/system/config/export")
                .then().statusCode(200);

        Map<String, Object> insert = awaitOperLog("字典类型", dictType);
        assertEquals(1, ((Number) insert.get("businessType")).intValue());
        assertEquals(0, ((Number) insert.get("status")).intValue());
        assertEquals("POST", insert.get("requestMethod"));
        assertEquals("/system/dict/type", insert.get("operUrl"));
        assertEquals("admin", insert.get("operName"));
        assertTrue(String.valueOf(insert.get("jsonResult")).contains("\"code\":200"));

        Map<String, Object> export = awaitOperLog("参数管理", "oplog-form");
        assertEquals(5, ((Number) export.get("businessType")).intValue());

        Number dictId = given().header("Authorization", token)
                .when().get("/system/dict/type/list?dictType=" + dictType)
                .then().statusCode(200).extract().path("rows[0].dictId");
        given().header("Authorization", token)
                .when().delete("/system/dict/type/" + dictId)
                .then().statusCode(200).body("code", equalTo(200));
    }

    @Test
    void operLogStripsPasswordFields() throws Exception {
        given().header("Authorization", token)
                .contentType("application/json")
                .body(Map.of("userId", 999999, "password", "secret-should-not-leak"))
                .when().put("/system/user/resetPwd");
        Map<String, Object> row = awaitOperLog("用户管理", "999999");
        assertTrue(!String.valueOf(row.get("operParam")).contains("secret-should-not-leak"));
    }

    private Map<String, Object> awaitOperLog(String title, String paramFragment) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            List<Map<String, Object>> rows = given().header("Authorization", token)
                    .when().get("/monitor/operlog/list?pageNum=1&pageSize=50&title=" + title)
                    .then().statusCode(200).extract().path("rows");
            for (Map<String, Object> row : rows) {
                if (String.valueOf(row.get("operParam")).contains(paramFragment)) {
                    return row;
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError("未找到操作日志 title=" + title + " param~" + paramFragment);
    }

    @Test
    void loginWritesLogininfor() {
        given().header("Authorization", token)
                .when().get("/monitor/logininfor/list?pageNum=1&pageSize=10")
                .then().statusCode(200)
                .body("rows.msg", org.hamcrest.Matchers.hasItem("登录成功"));
    }

    @Test
    void jobLifecycle() throws Exception {
        String name = "测试任务" + System.nanoTime();
        given().header("Authorization", token)
                .contentType("application/json")
                .body(Map.of("jobName", name, "jobGroup", "DEFAULT",
                        "invokeTarget", "ryTask.ryParams('x')", "cronExpression", "0 0 0 1 1 ? 2099",
                        "misfirePolicy", "1", "concurrent", "1", "status", "1"))
                .when().post("/monitor/job")
                .then().statusCode(200).body("code", equalTo(200));

        given().header("Authorization", token)
                .contentType("application/json")
                .body(Map.of("jobName", "bad", "jobGroup", "DEFAULT",
                        "invokeTarget", "java.lang.Runtime.getRuntime()", "cronExpression", "0/1 * * * * ?"))
                .when().post("/monitor/job")
                .then().statusCode(200).body("code", equalTo(500));

        given().header("Authorization", token)
                .contentType("application/json")
                .body(Map.of("jobName", "badcron", "jobGroup", "DEFAULT",
                        "invokeTarget", "ryTask.ryNoParams", "cronExpression", "not a cron"))
                .when().post("/monitor/job")
                .then().statusCode(200).body("code", equalTo(500))
                .body("msg", containsString("Cron表达式不正确"));

        Response list = given().header("Authorization", token)
                .when().get("/monitor/job/list?jobName=" + name);
        list.then().statusCode(200).body("total", equalTo(1));
        Number jobId = list.path("rows[0].jobId");

        given().header("Authorization", token)
                .contentType("application/json")
                .body(Map.of("jobId", jobId, "jobGroup", "DEFAULT"))
                .when().put("/monitor/job/run")
                .then().statusCode(200).body("code", equalTo(200));

        long deadline = System.currentTimeMillis() + 5000;
        int total = 0;
        while (System.currentTimeMillis() < deadline && total == 0) {
            total = given().header("Authorization", token)
                    .when().get("/monitor/jobLog/list?jobName=" + name)
                    .then().statusCode(200).extract().path("total");
            if (total == 0) {
                Thread.sleep(200);
            }
        }
        assertTrue(total > 0, "执行一次后应写入调度日志");

        given().header("Authorization", token)
                .contentType("application/json")
                .body(Map.of("jobId", jobId, "status", "0"))
                .when().put("/monitor/job/changeStatus")
                .then().statusCode(200).body("code", equalTo(200));
        given().header("Authorization", token)
                .when().get("/monitor/job/" + jobId)
                .then().statusCode(200).body("data.status", equalTo("0"));

        byte[] xlsx = given().header("Authorization", token)
                .contentType("application/x-www-form-urlencoded")
                .formParam("jobName", name)
                .when().post("/monitor/job/export")
                .then().statusCode(200).extract().asByteArray();
        assertEquals('P', (char) xlsx[0]);

        given().header("Authorization", token)
                .when().delete("/monitor/job/" + jobId)
                .then().statusCode(200).body("code", equalTo(200));
        given().header("Authorization", token)
                .when().get("/monitor/job/list?jobName=" + name)
                .then().statusCode(200).body("total", equalTo(0));

        List<Number> logIds = given().header("Authorization", token)
                .when().get("/monitor/jobLog/list?pageSize=100&jobName=" + name)
                .then().statusCode(200).extract().path("rows.jobLogId");
        String ids = logIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        given().header("Authorization", token)
                .when().delete("/monitor/jobLog/" + ids)
                .then().statusCode(200).body("code", equalTo(200));
    }
}
