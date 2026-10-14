package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class QuarkusDefectEnumNameTest {

    private String admin;
    private long projectId;

    @BeforeEach
    void setUp() {
        admin = QuarkusTestAuth.loginAsAdmin();
        String suffix = Long.toString(System.nanoTime(), 36);
        Number teamId = given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"teamName\":\"枚举团队" + suffix + "\",\"teamKey\":\"et" + suffix + "\"}")
                .when().post("/system/team")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().path("data.teamId");
        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"teamId\":" + teamId + ",\"projectName\":\"枚举项目" + suffix + "\",\"projectKey\":\"ep" + suffix + "\"}")
                .when().post("/system/project")
                .then().statusCode(200).body("code", equalTo(200));
        projectId = given().header("Authorization", admin)
                .when().get("/system/project/list?teamId=" + teamId + "&pageNum=1&pageSize=10")
                .then().statusCode(200).extract().<Number>path("rows[0].projectId").longValue();
    }

    @Test
    void outputsNamesAndAcceptsNamesOrCodes() {
        long task = create("\"TASK\"", "任务缺陷");
        long demand = create("2", "需求缺陷");
        long bug = create("\"bug\"", "问题缺陷");

        JsonPath detail = given().header("Authorization", admin)
                .when().get("/system/defect/" + task)
                .then().statusCode(200).body("code", equalTo(200))
                .extract().jsonPath();
        assertEquals("TASK", detail.getString("data.defectType"));
        assertEquals("TASK", detail.getString("data.defectTypeName"));
        assertEquals(0, detail.getInt("data.defectState"));
        assertEquals("PROCESSING", detail.getString("data.defectStateName"));

        given().header("Authorization", admin)
                .when().get("/system/defect/" + demand)
                .then().body("data.defectTypeName", equalTo("DEMAND"));
        given().header("Authorization", admin)
                .when().get("/system/defect/" + bug)
                .then().body("data.defectTypeName", equalTo("BUG"));

        Map<String, Object> row = given().header("Authorization", admin)
                .when().get("/system/defect/list?projectId=" + projectId + "&pageNum=1&pageSize=10")
                .then().statusCode(200)
                .extract().jsonPath().getMap("rows.find { it.defectId == " + task + " }");
        assertEquals("TASK", row.get("defectTypeName"));
        assertEquals("PROCESSING", row.get("defectStateName"));

        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"defectName\":\"非法类型\",\"projectId\":" + projectId + ",\"defectType\":\"FEATURE\",\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then().statusCode(400);
        given().header("Authorization", admin)
                .when().get("/system/defect/list?projectId=" + projectId + "&pageNum=1&pageSize=10")
                .then().body("rows.defectName", not(hasItem("非法类型")));
    }

    @Test
    void roundTripUpdateKeepsTypeAndState() {
        long id = create("\"DEMAND\"", "回传缺陷");
        Map<String, Object> data = given().header("Authorization", admin)
                .when().get("/system/defect/" + id)
                .then().statusCode(200).extract().jsonPath().getMap("data");
        data.put("defectName", "回传缺陷-改");
        given().header("Authorization", admin).contentType(ContentType.JSON).body(data)
                .when().put("/system/defect")
                .then().statusCode(200).body("code", equalTo(200));
        given().header("Authorization", admin)
                .when().get("/system/defect/" + id)
                .then().body("data.defectName", equalTo("回传缺陷-改"))
                .body("data.defectTypeName", equalTo("DEMAND"))
                .body("data.defectStateName", equalTo("PROCESSING"));
    }

    @Test
    void filtersByTypeNameAndStates() {
        long task = create("\"TASK\"", "筛选任务");
        long bug = create("\"BUG\"", "筛选问题");
        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"defectId\":" + bug + ",\"defectLogDescribe\":\"关闭\"}")
                .when().post("/system/defect/" + bug + "/close")
                .then().statusCode(200).body("code", equalTo(200));

        given().header("Authorization", admin)
                .when().get("/system/defect/list?projectId=" + projectId + "&defectType=TASK&pageNum=1&pageSize=20")
                .then().statusCode(200)
                .body("rows.defectTypeName", everyItem(equalTo("TASK")))
                .body("rows.defectId", hasItem((int) task));

        List<String> states = given().header("Authorization", admin)
                .queryParam("params[defectStates]", "0,1")
                .when().get("/system/defect/list?projectId=" + projectId + "&pageNum=1&pageSize=20")
                .then().statusCode(200)
                .body("rows.defectStateName", everyItem(isOneOf("PROCESSING", "AUDIT")))
                .extract().jsonPath().getList("rows.defectName");
        assertTrue(states.contains("筛选任务") && !states.contains("筛选问题"), states.toString());

        given().header("Authorization", admin)
                .queryParam("params[defectStates]", "CLOSED")
                .when().get("/system/defect/list?projectId=" + projectId + "&pageNum=1&pageSize=20")
                .then().statusCode(200)
                .body("rows.defectName", hasItem("筛选问题"))
                .body("rows.defectStateName", everyItem(equalTo("CLOSED")));

        given().header("Authorization", admin)
                .queryParam("params[defectStates]", "")
                .when().get("/system/defect/list?projectId=" + projectId + "&defectType=&pageNum=1&pageSize=20")
                .then().statusCode(200)
                .body("rows.defectName", hasItem("筛选问题"))
                .body("rows.defectName", hasItem("筛选任务"));

        given().header("Authorization", admin)
                .when().get("/system/defect/list?projectId=" + projectId + "&defectType=FEATURE")
                .then().statusCode(400);
    }

    private long create(String typeJson, String name) {
        return given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"defectName\":\"" + name + "\",\"projectId\":" + projectId
                        + ",\"defectType\":" + typeJson + ",\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().<Number>path("data.defectId").longValue();
    }
}
