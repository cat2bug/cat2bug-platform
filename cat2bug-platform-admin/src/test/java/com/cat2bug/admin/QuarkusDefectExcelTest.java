package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class QuarkusDefectExcelTest {

    @Test
    void defectExcelExportAndImport() throws IOException {
        String token = QuarkusTestAuth.loginAsAdmin();

        prepareProject(token);
        String handlerNickName = QuarkusTestAuth.projectMemberNickName(token, 1, 1);

        byte[] importWorkbook = buildImportWorkbook("Quarkus Excel 导入测试", handlerNickName);

        given()
                .header("Authorization", token)
                .multiPart("file", "defect-import.xlsx", importWorkbook,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .queryParam("updateSupport", false)
                .when().post("/system/defect/import")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("msg", containsString("导入成功"));

        byte[] exportBytes = given()
                .header("Authorization", token)
                .contentType("application/x-www-form-urlencoded")
                .formParam("projectId", 1)
                .when().post("/system/defect/export")
                .then()
                .statusCode(200)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .extract().asByteArray();

        assertTrue(exportBytes.length > 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exportBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertTrue(sheet.getLastRowNum() >= 1);
            Row firstDataRow = sheet.getRow(1);
            assertTrue(firstDataRow != null);
            assertTrue(firstDataRow.getCell(2).getStringCellValue().length() > 0);
        }
    }

    @Test
    void defectImportTemplateStillWorks() {
        String token = QuarkusTestAuth.loginAsAdmin();

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"currentProjectId\":1}")
                .when().put("/system/user-config")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .contentType("application/x-www-form-urlencoded")
                .when().post("/system/defect/importTemplate")
                .then()
                .statusCode(200)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Length", notNullValue());
    }

    @Test
    void defectExportIncludesCustomFieldColumn() throws IOException {
        String token = QuarkusTestAuth.loginAsAdmin();
        String fieldKey = "q_cf_" + (System.currentTimeMillis() % 100000);
        String fieldLabel = "测" + String.format("%05d", System.currentTimeMillis() % 100000);
        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"fieldKey\":\"" + fieldKey + "\",\"fieldLabel\":\"" + fieldLabel + "\",\"fieldType\":\"enum\","
                        + "\"enabled\":1,\"typeConfig\":{\"options\":[{\"key\":\"high\",\"label\":\"高\"}]}}")
                .when().post("/system/project/1/defect-fields")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        prepareProject(token);

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"projectId\":1,\"defectName\":\"Quarkus 自定义字段导出\",\"handleBy\":[1],"
                        + "\"customFields\":{\"" + fieldKey + "\":\"high\"}}")
                .when().post("/system/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        String exportColumns = "["
                + "{\"key\":\"defect.name\",\"prop\":\"defectName\",\"visible\":true},"
                + "{\"key\":\"custom:" + fieldKey + "\",\"prop\":\"custom_" + fieldKey + "\",\"visible\":true}"
                + "]";

        byte[] exportBytes = given()
                .header("Authorization", token)
                .contentType("application/x-www-form-urlencoded")
                .formParam("projectId", 1)
                .formParam("params[exportColumns]", exportColumns)
                .when().post("/system/defect/export")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exportBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            DataFormatter formatter = new DataFormatter();
            boolean foundCustomHeader = false;
            boolean foundCustomValue = false;
            int customCol = -1;
            for (int c = 0; c < header.getLastCellNum(); c++) {
                String text = formatter.formatCellValue(header.getCell(c));
                if (fieldLabel.equals(text)) {
                    foundCustomHeader = true;
                    customCol = c;
                }
            }
            assertTrue(foundCustomHeader, "导出应包含自定义字段列表头");
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || customCol < 0) {
                    continue;
                }
                String value = formatter.formatCellValue(row.getCell(customCol));
                if ("高".equals(value)) {
                    foundCustomValue = true;
                    break;
                }
            }
            assertTrue(foundCustomValue, "导出应包含自定义字段值");
        }
    }

    @Test
    void defectImportUpdateSupportByProjectNum() throws IOException {
        String token = QuarkusTestAuth.loginAsAdmin();
        prepareProject(token);
        String handlerNickName = QuarkusTestAuth.projectMemberNickName(token, 1, 1);

        byte[] importWorkbook = buildImportWorkbook("更新导入原始名称", handlerNickName);
        given()
                .header("Authorization", token)
                .multiPart("file", "defect-insert.xlsx", importWorkbook,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .queryParam("updateSupport", false)
                .when().post("/system/defect/import")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        byte[] exportBytes = given()
                .header("Authorization", token)
                .contentType("application/x-www-form-urlencoded")
                .formParam("projectId", 1)
                .when().post("/system/defect/export")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        long projectNum;
        DataFormatter formatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exportBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row targetRow = null;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row != null && row.getCell(2) != null
                        && "更新导入原始名称".equals(formatter.formatCellValue(row.getCell(2)))) {
                    targetRow = row;
                    break;
                }
            }
            assertTrue(targetRow != null, "应找到刚导入的缺陷");
            projectNum = Long.parseLong(formatter.formatCellValue(targetRow.getCell(0)));
        }

        byte[] updateWorkbook = buildImportWorkbookWithProjectNum(projectNum, "更新导入新名称", handlerNickName);
        given()
                .header("Authorization", token)
                .multiPart("file", "defect-update.xlsx", updateWorkbook,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .queryParam("updateSupport", true)
                .when().post("/system/defect/import")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("msg", containsString("导入成功"));

        byte[] afterUpdate = given()
                .header("Authorization", token)
                .contentType("application/x-www-form-urlencoded")
                .formParam("projectId", 1)
                .when().post("/system/defect/export")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(afterUpdate))) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean foundUpdated = false;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || row.getCell(0) == null || row.getCell(2) == null) {
                    continue;
                }
                long num = Long.parseLong(formatter.formatCellValue(row.getCell(0)));
                if (num == projectNum && "更新导入新名称".equals(formatter.formatCellValue(row.getCell(2)))) {
                    foundUpdated = true;
                    break;
                }
            }
            assertTrue(foundUpdated, "updateSupport 应按 projectNum 更新缺陷名称");
        }
    }

    @Test
    void defectExportUsesEnglishHeadersWhenLanguageHeaderSet() {
        String token = QuarkusTestAuth.loginAsAdmin();
        prepareProject(token);

        byte[] exportBytes = given()
                .header("Authorization", token)
                .header("language", "en_US")
                .contentType("application/x-www-form-urlencoded")
                .formParam("projectId", 1)
                .when().post("/system/defect/export")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exportBytes))) {
            Row header = workbook.getSheetAt(0).getRow(0);
            DataFormatter formatter = new DataFormatter();
            assertEquals("Defect Name", formatter.formatCellValue(header.getCell(2)));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void prepareProject(String token) {
        QuarkusTestAuth.ensureCurrentProject(token, 1);
        QuarkusTestAuth.ensureProjectMember(token, 1, 1);
    }

    private static byte[] buildImportWorkbook(String defectName, String handleBy) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("缺陷数据");
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

    private static byte[] buildImportWorkbookWithProjectNum(long projectNum, String defectName, String handleBy)
            throws IOException {
        byte[] workbook = buildImportWorkbook(defectName, handleBy);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(workbook));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.getSheetAt(0).getRow(1).createCell(0).setCellValue(projectNum);
            wb.write(out);
            return out.toByteArray();
        }
    }
}
