package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 用例提示词导出：列与 Spring 线一致，行数与同条件列表一致，无权限返回 403。 */
@QuarkusTest
class QuarkusCasePromptExportTest {

    private static final String PASSWORD = "Cat2bug@123";

    @Test
    void exportsPromptsOfProjectAndRejectsUserWithoutPermission() throws Exception {
        String admin = QuarkusTestAuth.loginAsAdmin();
        long projectId = 900_000_000L + (System.currentTimeMillis() % 100_000_000L);
        List<String> promptIds = new ArrayList<>();
        Long userId = null;
        try {
            for (String content : List.of("导出提示词一", "导出提示词二")) {
                given().header("Authorization", admin)
                        .contentType(ContentType.JSON)
                        .body("{\"casePromptContent\":\"" + content + "\",\"projectId\":" + projectId + "}")
                        .when().post("/system/CasePrompt")
                        .then().statusCode(200).body("code", equalTo(200));
            }
            var list = given().header("Authorization", admin)
                    .when().get("/system/CasePrompt/list?pageNum=1&pageSize=100&projectId=" + projectId)
                    .then().statusCode(200).body("code", equalTo(200))
                    .extract().jsonPath();
            promptIds.addAll(list.getList("rows.casePromptId", String.class));
            assertEquals(2, list.getInt("total"));

            byte[] bytes = given().header("Authorization", admin)
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("projectId", projectId)
                    .when().post("/system/CasePrompt/export")
                    .then().statusCode(200)
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .extract().asByteArray();
            try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();
                assertEquals("提示词内容", formatter.formatCellValue(sheet.getRow(0).getCell(0)));
                assertEquals("创建人ID", formatter.formatCellValue(sheet.getRow(0).getCell(1)));
                assertEquals(2, sheet.getLastRowNum());
                assertEquals("1", formatter.formatCellValue(sheet.getRow(1).getCell(1)));
            }

            String userName = "cpx_" + System.currentTimeMillis();
            given().header("Authorization", admin)
                    .contentType(ContentType.JSON)
                    .body("{\"userName\":\"" + userName + "\",\"nickName\":\"" + userName + "\",\"password\":\""
                            + PASSWORD + "\",\"status\":\"0\"}")
                    .when().post("/system/user")
                    .then().statusCode(200).body("code", equalTo(200));
            userId = given().header("Authorization", admin)
                    .queryParam("userName", userName)
                    .when().get("/system/user/list?pageNum=1&pageSize=10")
                    .then().statusCode(200)
                    .extract().jsonPath().getLong("rows.find { it.userName == '" + userName + "' }.userId");
            String token = given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + userName + "\",\"password\":\"" + PASSWORD + "\"}")
                    .when().post("/login")
                    .then().statusCode(200).body("code", equalTo(200))
                    .extract().path("token");

            given().header("Authorization", token)
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("projectId", projectId)
                    .when().post("/system/CasePrompt/export")
                    .then().statusCode(200)
                    .body("code", equalTo(403));
        } finally {
            if (!promptIds.isEmpty()) {
                given().header("Authorization", admin)
                        .when().delete("/system/CasePrompt/" + String.join(",", promptIds));
            }
            if (userId != null) {
                given().header("Authorization", admin).when().delete("/system/user/" + userId);
            }
        }
    }
}
