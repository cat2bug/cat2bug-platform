package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class QuarkusDefectStatisticExportTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void defectStateLineExportReturnsXlsx() throws Exception {
        assertLineChartWhenDataPresent(export("/system/defect/statistic/defect-state-line/1/export?timeType=day"));
        assertLineChartWhenDataPresent(export("/system/defect/statistic/defect-state-line/1/export?timeType=month"));
    }

    @Test
    void memberDefectLineExportReturnsXlsx() throws Exception {
        assertLineChartWhenDataPresent(export("/system/defect/statistic/member-defect-line/1/export?timeType=day"));
        assertLineChartWhenDataPresent(export("/system/defect/statistic/member-defect-line/1/export?timeType=month"));
    }

    @Test
    void planStatisticsExportContainsFourBarCharts() throws Exception {
        String planId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"planName\":\"图表计划\",\"planVersion\":\"2.0\",\"projectId\":1}")
                .when().post("/system/plan")
                .then().statusCode(200)
                .extract().path("data.planId");
        try {
            try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(
                    export("/system/dashboard/1/plan-statistics/export")))) {
                XSSFDrawing drawing = wb.getSheetAt(0).getDrawingPatriarch();
                assertNotNull(drawing);
                List<XSSFChart> charts = drawing.getCharts();
                assertEquals(List.of("数量对比", "百分比对比", "密度对比", "耗时对比"),
                        charts.stream().map(c -> c.getTitleText().getString()).toList());
                assertEquals(5, charts.get(0).getChartSeries().get(0).getSeries().get(0)
                        .getCategoryData().getPointCount());
            }
        } finally {
            given().header("Authorization", token).when().delete("/system/plan/" + planId);
        }
    }

    private byte[] export(String path) {
        return given()
                .header("Authorization", token)
                .when().post(path)
                .then()
                .statusCode(200)
                .contentType(XLSX)
                .extract().asByteArray();
    }

    /** 与原 POI 实现一致：有数据行才画走势图。 */
    private static void assertLineChartWhenDataPresent(byte[] xlsx) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            XSSFSheet sheet = wb.getSheetAt(0);
            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            if (sheet.getLastRowNum() < 2) {
                assertEquals(null, drawing);
                return;
            }
            assertNotNull(drawing);
            XSSFChart chart = drawing.getCharts().get(0);
            assertEquals(sheet.getSheetName(), chart.getTitleText().getString());
            assertEquals(sheet.getLastRowNum() - 1, chart.getChartSeries().get(0).getSeriesCount());
        }
    }
}
