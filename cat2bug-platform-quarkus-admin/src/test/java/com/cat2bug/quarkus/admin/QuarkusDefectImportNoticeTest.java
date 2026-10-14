package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
class QuarkusDefectImportNoticeTest {

    @Test
    void defectImportDispatchesImNotice() throws IOException {
        String token = QuarkusTestAuth.loginAsAdmin();

        QuarkusTestAuth.ensureCurrentProject(token, 1);
        QuarkusTestAuth.ensureProjectMember(token, 1, 1);
        String handlerNickName = QuarkusTestAuth.projectMemberNickName(token, 1, 1);

        Number noticeTotalBefore = given()
                .header("Authorization", token)
                .queryParam("groupName", "defect")
                .when().get("/system/notice/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("total");

        byte[] importWorkbook = buildImportWorkbook("Excel导入IM通知", handlerNickName);

        given()
                .header("Authorization", token)
                .multiPart("file", "defect-import-notice.xlsx", importWorkbook,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .queryParam("updateSupport", false)
                .when().post("/system/defect/import")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("msg", containsString("导入成功"));

        given()
                .header("Authorization", token)
                .queryParam("groupName", "defect")
                .when().get("/system/notice/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThan(noticeTotalBefore.intValue()));
    }

    private static byte[] buildImportWorkbook(String defectName, String handleBy) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("缺陷数据");
            Row header = sheet.createRow(0);
            String[] headers = {"编号", "类型", "缺陷名称", "描述", "缺陷等级", "缺陷状态", "交付物", "处理人"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row row = sheet.createRow(1);
            row.createCell(1).setCellValue("BUG");
            row.createCell(2).setCellValue(defectName);
            row.createCell(4).setCellValue("middle");
            row.createCell(5).setCellValue("PROCESSING");
            row.createCell(7).setCellValue(handleBy);
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
