package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 列表排序（orderByColumn/isAsc）与 Spring PageHelper 对等，覆盖前端表格可点击排序的列。
 */
@QuarkusTest
class QuarkusListSortTest {

    private String token;
    private Number moduleId;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void defectListHonorsSortDirection() {
        String suffix = String.valueOf(System.nanoTime());
        createDefect("排序A" + suffix);
        createDefect("排序B" + suffix);

        List<Integer> asc = defectNums("ascending");
        List<Integer> desc = defectNums("descending");
        assertTrue(asc.size() >= 2);
        for (int i = 1; i < asc.size(); i++) {
            assertTrue(asc.get(i - 1) <= asc.get(i), "升序: " + asc);
        }
        for (int i = 1; i < desc.size(); i++) {
            assertTrue(desc.get(i - 1) >= desc.get(i), "降序: " + desc);
        }
    }

    @Test
    void invalidOrderColumnIsRejected() {
        given()
                .header("Authorization", token)
                .when().get("/system/defect/list?projectId=1&orderByColumn=defect_id'&isAsc=ascending")
                .then()
                .statusCode(200)
                .body("code", equalTo(500))
                .body("msg", equalTo("参数不符合规范，不能进行查询"));
    }

    @Test
    void frontendSortableColumnsWorkOnEveryList() {
        String planId = createPlanWithCase();
        Map<String, List<String>> lists = Map.of(
                "/system/defect/list?projectId=1", List.of("createMember", "defectDescribe", "defectLevel",
                        "defectName", "defectState", "defectType", "handleBy", "moduleName", "moduleVersion",
                        "planEndTime", "planStartTime", "projectNum", "updateTime", "createTime", "custom_x"),
                "/system/case/list?projectId=1", List.of("caseData", "caseExpect", "caseLevel", "caseName",
                        "caseNum", "casePreconditions", "caseStep", "defectProcessingCount", "moduleName", "updateTime"),
                "/system/plan/list?projectId=1", List.of("planName", "planNumber", "planStartTime", "planVersion",
                        "remark", "updateById", "updateTime"),
                "/system/PlanItem/list?projectId=1&planId=" + planId, List.of("annexUrls", "caseData",
                        "caseExpect", "caseLevel", "caseName", "caseNum", "casePreconditions", "caseStep", "imgUrls",
                        "moduleName", "planItemState", "updateBy", "updateTime"),
                "/system/PlanItem/case/list?projectId=1&params%5BplanId%5D=" + planId, List.of("caseName",
                        "caseNum", "caseLevel", "moduleName"),
                "/system/plan/" + planId + "/defect/list?projectId=1", List.of("projectNum", "defectName",
                        "defectState", "updateTime"),
                "/system/document/list?projectId=1", List.of("docName", "fileExtension", "updateByName", "updateTime"),
                "/monitor/operlog/list", List.of("costTime", "operName", "operTime"),
                "/monitor/logininfor/list", List.of("loginTime", "userName"));
        lists.forEach((url, columns) -> {
            for (String column : columns) {
                for (String direction : List.of("ascending", "descending")) {
                    Integer code = given()
                            .header("Authorization", token)
                            .when().get(url + (url.contains("?") ? "&" : "?") + "pageNum=1&pageSize=5&orderByColumn=" + column + "&isAsc=" + direction)
                            .then()
                            .statusCode(200)
                            .extract().path("code");
                    assertEquals(200, code, url + " " + column + " " + direction);
                }
            }
        });
    }

    @Test
    void planItemDefectIdsUseCommaSeparatedFormat() {
        String planId = createPlanWithCase();
        String planItemId = given()
                .header("Authorization", token)
                .when().get("/system/PlanItem/list?projectId=1&planId=" + planId + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows[0].planItemId", notNullValue())
                .extract().path("rows[0].planItemId");
        Number first = createDefect("计划关联缺陷1");
        Number second = createDefect("计划关联缺陷2");

        updatePlanItem("{\"planItemId\":\"" + planItemId + "\",\"params\":{\"defectId\":" + first + "}}");
        updatePlanItem("{\"planItemId\":\"" + planItemId + "\",\"params\":{\"defectId\":" + second + "}}");
        given()
                .header("Authorization", token)
                .when().get("/system/plan/" + planId + "/defect/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows.defectId", hasItem(first.intValue()))
                .body("rows.defectId", hasItem(second.intValue()));

        updatePlanItem("{\"planItemId\":\"" + planItemId + "\",\"defectIds\":[" + second + "]}");
        updatePlanItem("{\"planItemId\":\"" + planItemId + "\",\"params\":{\"defectId\":" + first + "}}");
        given()
                .header("Authorization", token)
                .when().get("/system/PlanItem/list?projectId=1&planId=" + planId + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows[0].defectIds", containsInAnyOrder(second.intValue(), first.intValue()));

        updatePlanItem("{\"planItemId\":\"" + planItemId + "\",\"params\":{\"deleteDefectId\":" + first + "}}");
        given()
                .header("Authorization", token)
                .when().get("/system/PlanItem/list?projectId=1&planId=" + planId + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("rows[0].defectIds", contains(second.intValue()));
    }

    private List<Integer> defectNums(String direction) {
        return given()
                .header("Authorization", token)
                .when().get("/system/defect/list?projectId=1&pageNum=1&pageSize=50&orderByColumn=projectNum&isAsc=" + direction)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("rows.projectNum");
    }

    private Number createDefect(String name) {
        return given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"" + name + "\",\"projectId\":1,\"handleBy\":[1]"
                        + (moduleId == null ? "" : ",\"moduleId\":" + moduleId) + "}")
                .when().post("/system/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data.defectId");
    }

    private String createPlanWithCase() {
        moduleId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"projectId\":1,\"modulePid\":0,\"moduleName\":\"排序模块" + System.nanoTime() + "\"}")
                .when().post("/system/module")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data[0].moduleId");
        Number caseId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("[{\"projectId\":1,\"moduleId\":" + moduleId + ",\"caseName\":\"排序用例" + System.nanoTime()
                        + "\",\"caseType\":1,\"caseLevel\":1}]")
                .when().post("/system/case/batch")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data[0].caseId");
        assertEquals(true, caseId != null, "批量新增用例应返回 caseId");
        return given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"planName\":\"排序测试计划\",\"planVersion\":\"1.0\",\"projectId\":1,"
                        + "\"sysPlanItemList\":[{\"caseId\":" + caseId + "}]}")
                .when().post("/system/plan")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.planId", notNullValue())
                .extract().path("data.planId");
    }

    private void updatePlanItem(String body) {
        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body(body)
                .when().put("/system/PlanItem")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }
}
