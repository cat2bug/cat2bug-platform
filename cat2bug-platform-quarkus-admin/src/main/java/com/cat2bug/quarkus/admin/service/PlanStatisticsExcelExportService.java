package com.cat2bug.quarkus.admin.service;

import com.cat2bug.quarkus.common.domain.SysPlan;
import com.cat2bug.quarkus.common.excel.ExcelChart;
import com.cat2bug.quarkus.common.excel.ExcelChartSeries;
import com.cat2bug.quarkus.common.excel.FastExcelSupport;
import com.cat2bug.quarkus.common.excel.XlsxPatcher;
import com.cat2bug.quarkus.system.mapper.SysPlanMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.dhatim.fastexcel.BorderStyle;
import org.dhatim.fastexcel.StyleSetter;
import org.dhatim.fastexcel.Worksheet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 测试计划统计 Excel 导出（与 Spring SysDashboardController#planStatisticsExport 结构一致）。
 */
@ApplicationScoped
public class PlanStatisticsExcelExportService {

    /** 首行 / 次行预留给统计图的高度（磅）。 */
    private static final double CHART_ROW_HEIGHT = 250;
    private static final double CHART_ROW2_HEIGHT = 150;
    private static final double COLUMN_WIDTH = 15;
    private static final String TITLE_FILL = "606266";
    private static final String TITLE_FONT = "FFFFFF";
    private static final String PERCENTAGE_FORMAT = "0.00%";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /** 计划数据首行（前两行占位图表、再两行表头）。 */
    private static final int DATA_ROW_START = 4;
    /** 用例统计列起点（计划名/版本/开始/结束之后）。 */
    private static final int CASE_COL_START = 4;
    /** 图表分类：测试用例四项 + 缺陷九项，顺序与数据列一致。 */
    private static final List<String> CHART_CATEGORIES = List.of(
            "通过", "未通过", "未执行", "总数", "缺陷总数", "缺陷发现率", "缺陷修复率", "缺陷探测率",
            "缺陷严重率", "缺陷重开率", "缺陷逃逸率", "缺陷密度", "缺陷修复平均时长");

    @Inject
    SysPlanMapper sysPlanMapper;

    public byte[] exportPlanStatistics(Long projectId) {
        SysPlan query = new SysPlan();
        query.setProjectId(projectId);
        List<SysPlan> planList = sysPlanMapper.selectSysPlanList(query, 0, Integer.MAX_VALUE);
        String sheetName = "测试计划统计";

        List<double[]> chartValues = new ArrayList<>();
        byte[] xlsx = FastExcelSupport.writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet(sheetName);

            int rowNum = -1;
            reserveChartRow(sheet, ++rowNum, CHART_ROW_HEIGHT);
            reserveChartRow(sheet, ++rowNum, CHART_ROW2_HEIGHT);

            int titleRow1 = ++rowNum;

            int titleCellNum = 0;
            int caseTitleStartCellNum = 0;

            writeTitleCell(sheet, titleRow1, titleCellNum, "测试计划名名称");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "版本");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "计划开始时间");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "计划结束时间");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "测试用例");
            sheet.range(titleRow1, titleCellNum, titleRow1, titleCellNum + 3).merge();
            caseTitleStartCellNum = titleCellNum;

            titleCellNum += 4;
            writeTitleCell(sheet, titleRow1, titleCellNum, "缺陷总数");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "缺陷发现率");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "缺陷修复率");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "缺陷探测率");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "缺陷严重率");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "缺陷重开率");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "缺陷逃逸率");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "缺陷密度");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            writeTitleCell(sheet, titleRow1, ++titleCellNum, "缺陷修复平均时长");
            sheet.range(titleRow1, titleCellNum, titleRow1 + 1, titleCellNum).merge();

            int titleRow2 = ++rowNum;
            writeTitleCell(sheet, titleRow2, caseTitleStartCellNum, "通过");
            writeTitleCell(sheet, titleRow2, caseTitleStartCellNum + 1, "未通过");
            writeTitleCell(sheet, titleRow2, caseTitleStartCellNum + 2, "未执行");
            writeTitleCell(sheet, titleRow2, caseTitleStartCellNum + 3, "总数");

            for (int i = 0; i < caseTitleStartCellNum; i++) {
                applyTitleStyle(sheet, titleRow2, i);
            }
            for (int i = caseTitleStartCellNum + 4; i <= titleCellNum; i++) {
                applyTitleStyle(sheet, titleRow2, i);
            }

            for (SysPlan p : planList) {
                int colNum = 0;
                int row = ++rowNum;

                writeText(sheet, row, colNum++, p.getPlanName());
                writeText(sheet, row, colNum++, p.getPlanVersion());
                writeTime(sheet, row, colNum++, p.getPlanStartTime());
                writeTime(sheet, row, colNum++, p.getPlanEndTime());

                writeNumber(sheet, row, colNum++, p.getPassCount());
                writeNumber(sheet, row, colNum++, p.getFailCount());
                writeNumber(sheet, row, colNum++, p.getUnexecutedCount());
                writeNumber(sheet, row, colNum++, p.getItemTotal());
                writeNumber(sheet, row, colNum++, p.getDefectCount());

                writePercentage(sheet, row, colNum++, strPercentage2Double(p.getDefectDiscoveryRate()));
                writePercentage(sheet, row, colNum++, strPercentage2Double(p.getDefectRepairRate()));
                writePercentage(sheet, row, colNum++, strPercentage2Double(p.getDefectDetectionRate()));
                writePercentage(sheet, row, colNum++, strPercentage2Double(p.getDefectSeverityRate()));
                writePercentage(sheet, row, colNum++, strPercentage2Double(p.getDefectRestartRate()));
                writePercentage(sheet, row, colNum++, strPercentage2Double(p.getDefectEscapeRate()));

                writeNumber(sheet, row, colNum++, Double.parseDouble(p.getDefectDensity()));
                writeNumber(sheet, row, colNum, Double.parseDouble(p.getDefectRepairAvgHour()));
                chartValues.add(new double[] {
                        toDouble(p.getPassCount()), toDouble(p.getFailCount()), toDouble(p.getUnexecutedCount()),
                        toDouble(p.getItemTotal()), toDouble(p.getDefectCount()),
                        strPercentage2Double(p.getDefectDiscoveryRate()),
                        strPercentage2Double(p.getDefectRepairRate()),
                        strPercentage2Double(p.getDefectDetectionRate()),
                        strPercentage2Double(p.getDefectSeverityRate()),
                        strPercentage2Double(p.getDefectRestartRate()),
                        strPercentage2Double(p.getDefectEscapeRate()),
                        Double.parseDouble(p.getDefectDensity()),
                        Double.parseDouble(p.getDefectRepairAvgHour())});
            }

            // 固定列宽：autoSizeColumn 依赖 AWT FontMetrics，GraalVM Native 构建无法通过
            for (int i = 0; i <= titleCellNum; i++) {
                sheet.width(i, COLUMN_WIDTH);
            }
        }, "导出测试计划统计失败");
        if (planList.isEmpty()) {
            return xlsx;
        }
        int lastCol = CASE_COL_START + CHART_CATEGORIES.size() - 1;
        return XlsxPatcher.of()
                .chart(1, barChart(sheetName, "数量对比", 0, 0, lastCol + 1, 1, "数量(个)",
                        planList, chartValues, new int[] {0, 1, 2, 3, 4}, true, ExcelChart.Legend.RIGHT))
                .chart(1, barChart(sheetName, "百分比对比", 0, 1, 9, 2, "百分比(%)",
                        planList, chartValues, new int[] {5, 6, 7, 8, 9, 10}, false, ExcelChart.Legend.NONE))
                .chart(1, barChart(sheetName, "密度对比", 9, 1, 13, 2, "密度",
                        planList, chartValues, new int[] {11}, false, ExcelChart.Legend.NONE))
                .chart(1, barChart(sheetName, "耗时对比", 13, 1, 17, 2, "时间(小时)",
                        planList, chartValues, new int[] {12}, false, ExcelChart.Legend.NONE))
                .apply(xlsx, "导出测试计划统计失败");
    }

    /**
     * 每个计划一条序列；分类按 {@code bindIndices} 从 {@link #CHART_CATEGORIES} 中挑选，
     * 数值引用该计划所在行的连续数据列。
     */
    private ExcelChart barChart(String sheetName, String title, int col1, int row1, int col2, int row2,
                                String yName, List<SysPlan> planList, List<double[]> chartValues,
                                int[] bindIndices, boolean withVersion, ExcelChart.Legend legend) {
        List<String> categories = Arrays.stream(bindIndices).mapToObj(CHART_CATEGORIES::get).toList();
        int firstCol = CASE_COL_START + bindIndices[0];
        int lastCol = CASE_COL_START + bindIndices[bindIndices.length - 1];
        List<ExcelChartSeries> series = new ArrayList<>();
        for (int i = 0; i < planList.size(); i++) {
            SysPlan plan = planList.get(i);
            String name = plan.getPlanName() == null ? "" : plan.getPlanName();
            if (withVersion && plan.getPlanVersion() != null && !plan.getPlanVersion().isBlank()) {
                name += String.format("(%s)", plan.getPlanVersion());
            }
            double[] row = chartValues.get(i);
            List<Double> values = Arrays.stream(bindIndices).mapToObj(idx -> row[idx]).toList();
            int sheetRow = DATA_ROW_START + i;
            series.add(new ExcelChartSeries(name, null, categories,
                    ExcelChartSeries.ref(sheetName, sheetRow, firstCol, sheetRow, lastCol), values));
        }
        return new ExcelChart(ExcelChart.Type.BAR, title, col1, row1, col2, row2, yName, legend, series);
    }

    private static double toDouble(Number value) {
        return value == null ? 0 : value.doubleValue();
    }

    /** 空行不会被 FastExcel 写出，需放一个无值单元格才能保留行高。 */
    private void reserveChartRow(Worksheet sheet, int row, double height) {
        sheet.rowHeight(row, height);
        sheet.style(row, 0).set();
    }

    private void writeTitleCell(Worksheet sheet, int row, int col, String text) {
        sheet.value(row, col, text);
        applyTitleStyle(sheet, row, col);
    }

    private void applyTitleStyle(Worksheet sheet, int row, int col) {
        sheet.style(row, col)
                .fillColor(TITLE_FILL)
                .fontColor(TITLE_FONT)
                .horizontalAlignment("center")
                .verticalAlignment("center")
                .borderStyle(BorderStyle.THIN)
                .set();
    }

    private void writeText(Worksheet sheet, int row, int col, String value) {
        if (value != null) {
            sheet.value(row, col, value);
        }
        dataStyle(sheet, row, col).set();
    }

    private void writeNumber(Worksheet sheet, int row, int col, Number value) {
        sheet.value(row, col, value);
        dataStyle(sheet, row, col).set();
    }

    private void writePercentage(Worksheet sheet, int row, int col, double value) {
        sheet.value(row, col, value);
        dataStyle(sheet, row, col).format(PERCENTAGE_FORMAT).set();
    }

    private void writeTime(Worksheet sheet, int row, int col, Date value) {
        if (value != null) {
            sheet.value(row, col, value);
        }
        dataStyle(sheet, row, col).format(TIME_FORMAT).set();
    }

    private StyleSetter dataStyle(Worksheet sheet, int row, int col) {
        return sheet.style(row, col)
                .horizontalAlignment("right")
                .verticalAlignment("center")
                .borderStyle(BorderStyle.THIN);
    }

    private static double strPercentage2Double(String value) {
        return Double.parseDouble(value.replace("%", "")) * 0.01;
    }
}
