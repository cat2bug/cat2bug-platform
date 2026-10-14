package com.cat2bug.admin;

import com.cat2bug.common.excel.ExcelChart;
import com.cat2bug.common.excel.ExcelChartSeries;
import com.cat2bug.common.excel.FastExcelSupport;
import com.cat2bug.common.excel.XlsxPatcher;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 用 POI 独立读回 {@link XlsxPatcher} 注入的图表，确认 OOXML 结构合法、引用与缓存正确。 */
class XlsxPatcherTest {

    private static final String SHEET = "走势's图";

    @Test
    void injectsLineAndBarChartsReadableByPoi() throws Exception {
        byte[] xlsx = FastExcelSupport.writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet(SHEET);
            sheet.value(1, 1, "10-01");
            sheet.value(1, 2, "10-02");
            sheet.value(2, 0, "A<&>");
            sheet.value(2, 1, 3);
            sheet.value(2, 2, 5);
            workbook.newWorksheet("第二页").value(0, 0, "x");
        }, "err");

        ExcelChartSeries lineSeries = new ExcelChartSeries("A<&>",
                ExcelChartSeries.ref(SHEET, 1, 1, 1, 2), List.of("10-01", "10-02"),
                ExcelChartSeries.ref(SHEET, 2, 1, 2, 2), List.of(3, 5));
        ExcelChartSeries barSeries = new ExcelChartSeries("计划(v1)", null, List.of("通过", "未通过"),
                ExcelChartSeries.ref(SHEET, 2, 1, 2, 2), List.of(3.0, 5.5));
        byte[] patched = XlsxPatcher.of()
                .baseColumnWidth(1, 12)
                .chart(1, new ExcelChart(ExcelChart.Type.LINE, "走势", 0, 0, 3, 1, "缺陷数量",
                        ExcelChart.Legend.TOP, List.of(lineSeries)))
                .chart(1, new ExcelChart(ExcelChart.Type.BAR, "数量对比", 3, 0, 6, 1, "数量(个)",
                        ExcelChart.Legend.RIGHT, List.of(barSeries)))
                .apply(xlsx, "err");

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(patched))) {
            XSSFSheet sheet = wb.getSheet(SHEET);
            assertEquals(12, sheet.getDefaultColumnWidth());
            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            assertNotNull(drawing);
            List<XSSFChart> charts = drawing.getCharts();
            assertEquals(2, charts.size());

            XSSFChart line = charts.get(0);
            assertEquals("走势", line.getTitleText().getString());
            XDDFChartData lineData = line.getChartSeries().get(0);
            assertTrue(lineData instanceof XDDFLineChartData);
            XDDFChartData.Series ls = lineData.getSeries().get(0);
            assertEquals("A<&>", line.getCTChart().getPlotArea().getLineChartArray(0).getSerArray(0).getTx().getV());
            assertEquals("'走势''s图'!$B$3:$C$3", ls.getValuesData().getDataRangeReference());
            assertEquals("'走势''s图'!$B$2:$C$2", ls.getCategoryData().getDataRangeReference());
            assertTrue(line.getCTChart().getPlotArea().getLineChartArray(0).getSerArray(0).getSmooth().getVal());

            XSSFChart bar = charts.get(1);
            XDDFChartData barData = bar.getChartSeries().get(0);
            assertTrue(barData instanceof XDDFBarChartData);
            XDDFChartData.Series bs = barData.getSeries().get(0);
            assertEquals(2, bs.getCategoryData().getPointCount());
            assertEquals("未通过", bs.getCategoryData().getPointAt(1));
            assertEquals(5.5, ((Number) bs.getValuesData().getPointAt(1)).doubleValue(), 1e-9);
            assertTrue(bar.getCTChart().getPlotArea().getBarChartArray(0).getSerArray(0).getDLbls()
                    .getShowVal().getVal());

            assertTrue(wb.getSheet("第二页").getDrawingPatriarch() == null);
        }
    }
}
