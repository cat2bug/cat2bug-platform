package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class QuarkusSystemExcelExportTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void roleDictDocumentExport() throws Exception {
        assertSheetHeader("/system/role/export", "角色序号");
        assertSheetHeader("/system/dict/data/export", "字典编码");
        assertSheetHeader("/system/dict/type/export", "字典主键");
        assertSheetHeader("/system/document/export?projectId=1", "项目ID");
        assertSheetHeader("/system/statistic/export", "统计模版编码");
        assertSheetHeader("/system/ai/export", "项目ID");
        assertSheetHeader("/system/config/export", "参数主键");
        assertSheetHeader("/system/plan/export", "测试计划名称", "projectId", "1");
        assertSheetHeader("/system/PlanItem/export", "测试计划", "projectId", "1");
        assertSheetHeader("/system/ai/account/export", "AI服务网址");
        assertSheetHeader("/ai/account/export", "AI服务网址");
        assertSheetHeader("/system/module/export", "父模块id", "projectId", "1");
        assertSheetHeader("/system/team/export", "团队名称");
        assertSheetHeader("/system/project/export", "项目名称", "teamId", resolveFirstTeamId(token));
        assertSheetHeader("/system/log/export", "缺陷日志的描述", "defectId", "1");
        assertSheetHeader("/system/report/export", "报告标题", "projectId", "1");
        assertSheetHeader("/system/report/template/export", "交付物类型");
        assertSheetHeader("/system/api/export", "项目id", "projectId", "1");
    }

    private String resolveFirstTeamId(String token) {
        Number teamId = given()
                .header("Authorization", token)
                .when().get("/system/team/my?pageNum=1&pageSize=1")
                .then()
                .statusCode(200)
                .extract().path("rows[0].teamId");
        if (teamId == null) {
            teamId = given()
                    .header("Authorization", token)
                    .contentType("application/json")
                    .body("{\"teamName\":\"Export测试团队\",\"teamKey\":\"tk" + System.nanoTime() + "\"}")
                    .when().post("/system/team")
                    .then()
                    .statusCode(200)
                    .extract().path("data.teamId");
        }
        return String.valueOf(teamId);
    }

    private void assertSheetHeader(String path, String expectedHeader) throws Exception {
        assertSheetHeader(path, expectedHeader, null, null);
    }

    private void assertSheetHeader(String path, String expectedHeader, String formKey, String formValue) throws Exception {
        var request = given()
                .header("Authorization", token)
                .contentType("application/x-www-form-urlencoded");
        if (formKey != null) {
            request.formParam(formKey, formValue);
        }
        byte[] bytes = request
                .when().post(path)
                .then()
                .statusCode(200)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .extract().asByteArray();
        assertTrue(bytes.length > 0);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertEquals(expectedHeader,
                    new DataFormatter().formatCellValue(workbook.getSheetAt(0).getRow(0).getCell(0)));
        }
    }
}
