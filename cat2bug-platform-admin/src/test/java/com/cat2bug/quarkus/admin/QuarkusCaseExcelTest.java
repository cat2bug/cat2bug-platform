package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class QuarkusCaseExcelTest {

    @Test
    void caseImportTemplateDownload() {
        String token = QuarkusTestAuth.loginAsAdmin();

        byte[] workbook = given()
                .header("Authorization", token)
                .contentType("application/x-www-form-urlencoded")
                .when().post("/system/case/importTemplate?projectId=1")
                .then()
                .statusCode(200)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .extract().asByteArray();

        try (var wb = WorkbookFactory.create(new ByteArrayInputStream(workbook))) {
            var sheet = wb.getSheetAt(0);
            var header = new DataFormatter().formatCellValue(sheet.getRow(0).getCell(0));
            assertTrue(header.contains("用例名称"));
        } catch (Exception e) {
            throw new AssertionError("导入模版解析失败", e);
        }
    }

    @Test
    void caseImportTemplateUsesEnglishHeadersWhenLanguageHeaderSet() {
        String token = QuarkusTestAuth.loginAsAdmin();

        byte[] workbook = given()
                .header("Authorization", token)
                .header("language", "en_US")
                .contentType("application/x-www-form-urlencoded")
                .when().post("/system/case/importTemplate?projectId=1")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        try (var wb = WorkbookFactory.create(new ByteArrayInputStream(workbook))) {
            var header = new DataFormatter().formatCellValue(wb.getSheetAt(0).getRow(0).getCell(0));
            assertTrue(header.contains("Case Name"));
        } catch (Exception e) {
            throw new AssertionError("英文表头解析失败", e);
        }
    }

    @Test
    void caseExportDownload() {
        String token = QuarkusTestAuth.loginAsAdmin();

        given()
                .header("Authorization", token)
                .contentType("application/x-www-form-urlencoded")
                .body("projectId=1")
                .when().post("/system/case/export")
                .then()
                .statusCode(200)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Test
    void caseImportDataRejectsEmptyFile() {
        String token = QuarkusTestAuth.loginAsAdmin();

        given()
                .header("Authorization", token)
                .contentType(ContentType.MULTIPART)
                .multiPart("projectId", "1")
                .when().post("/system/case/importData")
                .then()
                .statusCode(200)
                .body("code", equalTo(500));
    }

    @Test
    void caseExportRequiresPermission() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .body("projectId=1")
                .when().post("/system/case/export")
                .then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.anyOf(equalTo(401), equalTo(403)));
    }

    @Test
    void caseImportDataValidatesRequiredFields() throws Exception {
        String token = QuarkusTestAuth.loginAsAdmin();
        byte[] template = given()
                .header("Authorization", token)
                .contentType("application/x-www-form-urlencoded")
                .when().post("/system/case/importTemplate?projectId=1")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        try (var wb = WorkbookFactory.create(new ByteArrayInputStream(template))) {
            var sheet = wb.getSheetAt(0);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("仅名称无交付物");
            var out = new java.io.ByteArrayOutputStream();
            wb.write(out);
            template = out.toByteArray();
        }

        given()
                .header("Authorization", token)
                .contentType(ContentType.MULTIPART)
                .multiPart("file", "case-template.xlsx", template,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .multiPart("projectId", "1")
                .when().post("/system/case/importData")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.message", notNullValue())
                .body("data.rows.size()", greaterThan(0));
    }
}
