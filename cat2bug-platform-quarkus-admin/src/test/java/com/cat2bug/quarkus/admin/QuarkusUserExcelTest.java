package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class QuarkusUserExcelTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void userExportImportTemplateAndMemberAlias() throws Exception {
        byte[] exportBytes = given()
                .header("Authorization", token)
                .when().post("/system/user/export")
                .then()
                .statusCode(200)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .extract().asByteArray();
        assertTrue(exportBytes.length > 0);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(exportBytes))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("用户序号", new DataFormatter().formatCellValue(sheet.getRow(0).getCell(0)));
        }

        byte[] templateBytes = given()
                .header("Authorization", token)
                .when().post("/system/member/importTemplate")
                .then()
                .statusCode(200)
                .extract().asByteArray();
        assertTrue(templateBytes.length > 0);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(templateBytes))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("部门编号", new DataFormatter().formatCellValue(sheet.getRow(0).getCell(0)));
            assertEquals("登录名称", new DataFormatter().formatCellValue(sheet.getRow(0).getCell(1)));
        }

        byte[] memberExport = given()
                .header("Authorization", token)
                .when().post("/system/member/export")
                .then()
                .statusCode(200)
                .extract().asByteArray();
        assertTrue(memberExport.length > 0);
    }

    /** 前端 download() 以表单 POST 下载模板，无参接口也必须接受表单请求。 */
    @Test
    void importTemplateAcceptsFormPost() {
        for (String path : new String[] {"/system/member/importTemplate", "/system/user/importTemplate"}) {
            given()
                    .header("Authorization", token)
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("_t", "1")
                    .when().post(path)
                    .then()
                    .statusCode(200)
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
    }

    @Test
    void languageSwitch() {
        given()
                .header("Authorization", token)
                .when().post("/zh-CN")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }
}
