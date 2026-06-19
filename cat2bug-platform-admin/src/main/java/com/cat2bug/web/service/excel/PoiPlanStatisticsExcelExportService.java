package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.system.domain.SysPlan;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.mapper.SysPlanMapper;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.AxisCrossBetween;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.BarGrouping;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.STDLblPos;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;/**
 * 测试计划统计 Excel 导出（与 Spring SysDashboardController#planStatisticsExport 结构一致）。
 */
@Profile("!native")
@Service
@SuppressWarnings("deprecation")
public class PoiPlanStatisticsExcelExportService implements PlanStatisticsExcelExportService {private static final byte[][] ECHARTS_COLORS = {
            new byte[] {(byte) 0x2e, (byte) 0xc7, (byte) 0xc9},
            new byte[] {(byte) 0xb6, (byte) 0xa2, (byte) 0xde},
            new byte[] {(byte) 0x5a, (byte) 0xb1, (byte) 0xef},
            new byte[] {(byte) 0xff, (byte) 0xb9, (byte) 0x80},
            new byte[] {(byte) 0xd8, 0x7a, (byte) 0x80},
            new byte[] {(byte) 0x8d, (byte) 0x98, (byte) 0xb3},
            new byte[] {(byte) 0xe5, (byte) 0xcf, 0x0d},
            new byte[] {(byte) 0x97, (byte) 0xb5, 0x52},
    };@Autowired
    SysPlanMapper sysPlanMapper;public byte[] exportPlanStatistics(Long projectId) {
        SysPlan query = new SysPlan();
        query.setProjectId(projectId);
        List<SysPlan> planList = sysPlanMapper.selectSysPlanList(query);
        String sheetName = "测试计划统计";try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet();
            wb.setSheetName(0, sheetName);
            sheet.setDefaultColumnWidth(12);int rowNum = -1;
            Row rowChart = sheet.createRow(++rowNum);
            rowChart.setHeight((short) 5000);Row rowChart2 = sheet.createRow(++rowNum);
            rowChart2.setHeight((short) 3000);Row rowTitle1 = sheet.createRow(++rowNum);
            XSSFCellStyle titleStyle = createTitleStyle(wb);int titleCellNum = 0;
            int caseTitleStartCellNum = 0;Cell cellPlanNameTitle = rowTitle1.createCell(titleCellNum);
            cellPlanNameTitle.setCellValue("测试计划名名称");
            cellPlanNameTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellVersionTitle = rowTitle1.createCell(++titleCellNum);
            cellVersionTitle.setCellValue("版本");
            cellVersionTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellPlanStartTimeTitle = rowTitle1.createCell(++titleCellNum);
            cellPlanStartTimeTitle.setCellValue("计划开始时间");
            cellPlanStartTimeTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellPlanEndTimeTitle = rowTitle1.createCell(++titleCellNum);
            cellPlanEndTimeTitle.setCellValue("计划结束时间");
            cellPlanEndTimeTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellCaseTitle = rowTitle1.createCell(++titleCellNum);
            cellCaseTitle.setCellValue("测试用例");
            cellCaseTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, titleCellNum, titleCellNum + 3));
            caseTitleStartCellNum = titleCellNum;titleCellNum += 4;
            Cell cellDefectTotalTitle = rowTitle1.createCell(titleCellNum);
            cellDefectTotalTitle.setCellValue("缺陷总数");
            cellDefectTotalTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellDefectDiscoveryRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectDiscoveryRateTitle.setCellValue("缺陷发现率");
            cellDefectDiscoveryRateTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellDefectRepairRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectRepairRateTitle.setCellValue("缺陷修复率");
            cellDefectRepairRateTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellDefectDetectionRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectDetectionRateTitle.setCellValue("缺陷探测率");
            cellDefectDetectionRateTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellDefectSeverityRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectSeverityRateTitle.setCellValue("缺陷严重率");
            cellDefectSeverityRateTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellDefectRestartRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectRestartRateTitle.setCellValue("缺陷重开率");
            cellDefectRestartRateTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellDefectEscapeRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectEscapeRateTitle.setCellValue("缺陷逃逸率");
            cellDefectEscapeRateTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellDefectDensityTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectDensityTitle.setCellValue("缺陷密度");
            cellDefectDensityTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Cell cellDefectRepairAvgHourTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectRepairAvgHourTitle.setCellValue("缺陷修复平均时长");
            cellDefectRepairAvgHourTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + 1, titleCellNum, titleCellNum));Row rowTitle2 = sheet.createRow(++rowNum);
            Cell cellPassTitle = rowTitle2.createCell(caseTitleStartCellNum);
            cellPassTitle.setCellValue("通过");
            cellPassTitle.setCellStyle(titleStyle);Cell cellNotPassTitle = rowTitle2.createCell(caseTitleStartCellNum + 1);
            cellNotPassTitle.setCellValue("未通过");
            cellNotPassTitle.setCellStyle(titleStyle);Cell cellRunTitle = rowTitle2.createCell(caseTitleStartCellNum + 2);
            cellRunTitle.setCellValue("未执行");
            cellRunTitle.setCellStyle(titleStyle);Cell cellNotRunTitle = rowTitle2.createCell(caseTitleStartCellNum + 3);
            cellNotRunTitle.setCellValue("总数");
            cellNotRunTitle.setCellStyle(titleStyle);for (int i = 0; i < caseTitleStartCellNum; i++) {
                Cell emptyCell = rowTitle2.createCell(i);
                emptyCell.setCellStyle(titleStyle);
            }
            for (int i = caseTitleStartCellNum + 4; i <= titleCellNum; i++) {
                Cell emptyCell = rowTitle2.createCell(i);
                emptyCell.setCellStyle(titleStyle);
            }XSSFCellStyle dataStyle = createDataStyle(wb);
            XSSFCellStyle percentageStyle = createPercentageStyle(wb);
            XSSFCellStyle timeStyle = createTimeStyle(wb);for (SysPlan p : planList) {
                int colNum = 0;
                Row row = sheet.createRow(++rowNum);Cell cellPlanName = row.createCell(colNum++);
                cellPlanName.setCellValue(p.getPlanName());
                cellPlanName.setCellStyle(dataStyle);Cell cellPlanVersion = row.createCell(colNum++);
                cellPlanVersion.setCellValue(p.getPlanVersion());
                cellPlanVersion.setCellStyle(dataStyle);Cell cellPlanStartTime = row.createCell(colNum++);
                cellPlanStartTime.setCellValue(p.getPlanStartTime());
                cellPlanStartTime.setCellStyle(timeStyle);Cell cellPlanEndTime = row.createCell(colNum++);
                cellPlanEndTime.setCellValue(p.getPlanEndTime());
                cellPlanEndTime.setCellStyle(timeStyle);Cell cellPass = row.createCell(colNum++);
                cellPass.setCellValue(p.getPassCount());
                cellPass.setCellStyle(dataStyle);Cell cellNotPass = row.createCell(colNum++);
                cellNotPass.setCellValue(p.getFailCount());
                cellNotPass.setCellStyle(dataStyle);Cell cellRun = row.createCell(colNum++);
                cellRun.setCellValue(p.getUnexecutedCount());
                cellRun.setCellStyle(dataStyle);Cell cellNotRun = row.createCell(colNum++);
                cellNotRun.setCellValue(p.getItemTotal());
                cellNotRun.setCellStyle(dataStyle);Cell cellDefectCount = row.createCell(colNum++);
                cellDefectCount.setCellValue(p.getDefectCount());
                cellDefectCount.setCellStyle(dataStyle);Cell cellDiscoveryRate = row.createCell(colNum++);
                cellDiscoveryRate.setCellValue(strPercentage2Double(p.getDefectDiscoveryRate()));
                cellDiscoveryRate.setCellStyle(percentageStyle);Cell cellRepairRate = row.createCell(colNum++);
                cellRepairRate.setCellValue(strPercentage2Double(p.getDefectRepairRate()));
                cellRepairRate.setCellStyle(percentageStyle);Cell cellDefectDetectionRate = row.createCell(colNum++);
                cellDefectDetectionRate.setCellValue(strPercentage2Double(p.getDefectDetectionRate()));
                cellDefectDetectionRate.setCellStyle(percentageStyle);Cell cellDefectSeverityRate = row.createCell(colNum++);
                cellDefectSeverityRate.setCellValue(strPercentage2Double(p.getDefectSeverityRate()));
                cellDefectSeverityRate.setCellStyle(percentageStyle);Cell cellDefectRestartRate = row.createCell(colNum++);
                cellDefectRestartRate.setCellValue(strPercentage2Double(p.getDefectRestartRate()));
                cellDefectRestartRate.setCellStyle(percentageStyle);Cell cellDefectEscapeRate = row.createCell(colNum++);
                cellDefectEscapeRate.setCellValue(strPercentage2Double(p.getDefectEscapeRate()));
                cellDefectEscapeRate.setCellStyle(percentageStyle);Cell cellDefectDensity = row.createCell(colNum++);
                cellDefectDensity.setCellValue(Double.parseDouble(p.getDefectDensity()));
                cellDefectDensity.setCellStyle(dataStyle);Cell cellDefectRepairAvgHour = row.createCell(colNum);
                cellDefectRepairAvgHour.setCellValue(Double.parseDouble(p.getDefectRepairAvgHour()));
                cellDefectRepairAvgHour.setCellStyle(dataStyle);
            }// 固定列宽：autoSizeColumn 依赖 AWT FontMetrics，GraalVM Native 构建无法通过
            final int columnWidth = 15 * 256;
            for (int i = 0; i <= titleCellNum; i++) {
                sheet.setColumnWidth(i, columnWidth);
            }if (!planList.isEmpty()) {
                drawCharts(sheet, titleCellNum, caseTitleStartCellNum, planList.size());
            }wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ServiceException("导出测试计划统计失败");
        }
    }private void drawCharts(XSSFSheet sheet, int titleCellNum, int caseTitleStartCellNum, int planCount) {
        int dataRowStartIndex = 4;
        int dataRowEndIndex = dataRowStartIndex + planCount - 1;List<String> categories = new LinkedList<>();
        for (int i = caseTitleStartCellNum; i < caseTitleStartCellNum + 4; i++) {
            categories.add(sheet.getRow(dataRowStartIndex - 1).getCell(i).getStringCellValue());
        }
        for (int i = caseTitleStartCellNum + 4; i <= titleCellNum; i++) {
            categories.add(sheet.getRow(dataRowStartIndex - 2).getCell(i).getStringCellValue());
        }
        XDDFDataSource<String> category = XDDFDataSourcesFactory.fromArray(categories.toArray(new String[0]));Map<String, XDDFNumericalDataSource<Double>> numValueCellRangeAddressList = new LinkedHashMap<>();
        for (int i = dataRowStartIndex; i <= dataRowEndIndex; i++) {
            String planName = sheet.getRow(i).getCell(0).getStringCellValue();
            String version = sheet.getRow(i).getCell(1).getStringCellValue();
            if (version != null && !version.isBlank()) {
                planName += String.format("(%s)", version);
            }
            XDDFNumericalDataSource<Double> value = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheet, new CellRangeAddress(i, i, caseTitleStartCellNum, caseTitleStartCellNum + 4));
            numValueCellRangeAddressList.put(planName, value);
        }
        drawPlanStatisticsChart(sheet, "数量对比", 0, 1, 0, titleCellNum + 1,
                "数量(个)", category, numValueCellRangeAddressList, new int[] {0, 1, 2, 3, 4}, true);Map<String, XDDFNumericalDataSource<Double>> percentageValueCellRangeAddressList = new LinkedHashMap<>();
        for (int i = dataRowStartIndex; i <= dataRowEndIndex; i++) {
            String planName = sheet.getRow(i).getCell(0).getStringCellValue();
            XDDFNumericalDataSource<Double> value = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheet, new CellRangeAddress(i, i, caseTitleStartCellNum + 5, caseTitleStartCellNum + 10));
            percentageValueCellRangeAddressList.put(planName, value);
        }
        drawPlanStatisticsChart(sheet, "百分比对比", 1, 2, 0, 9,
                "百分比(%)", category, percentageValueCellRangeAddressList,
                new int[] {5, 6, 7, 8, 9, 10}, false);Map<String, XDDFNumericalDataSource<Double>> densityValueCellRangeAddressList = new LinkedHashMap<>();
        for (int i = dataRowStartIndex; i <= dataRowEndIndex; i++) {
            String planName = sheet.getRow(i).getCell(0).getStringCellValue();
            XDDFNumericalDataSource<Double> value = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheet, new CellRangeAddress(i, i, caseTitleStartCellNum + 11, caseTitleStartCellNum + 11));
            densityValueCellRangeAddressList.put(planName, value);
        }
        drawPlanStatisticsChart(sheet, "密度对比", 1, 2, 9, 13,
                "密度", category, densityValueCellRangeAddressList, new int[] {11}, false);Map<String, XDDFNumericalDataSource<Double>> timeValueCellRangeAddressList = new LinkedHashMap<>();
        for (int i = dataRowStartIndex; i <= dataRowEndIndex; i++) {
            String planName = sheet.getRow(i).getCell(0).getStringCellValue();
            XDDFNumericalDataSource<Double> value = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheet, new CellRangeAddress(i, i, caseTitleStartCellNum + 12, caseTitleStartCellNum + 12));
            timeValueCellRangeAddressList.put(planName, value);
        }
        drawPlanStatisticsChart(sheet, "耗时对比", 1, 2, 13, 17,
                "时间(小时)", category, timeValueCellRangeAddressList, new int[] {12}, false);
    }private void drawPlanStatisticsChart(XSSFSheet sheet, String title, int row1, int row2, int col1, int col2,
                                         String yName, XDDFDataSource<String> category,
                                         Map<String, XDDFNumericalDataSource<Double>> valueCellRangeAddressList,
                                         int[] bindDataCols, boolean showLegend) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle(yName);
        leftAxis.setCrossBetween(AxisCrossBetween.BETWEEN);XDDFBarChartData dataChart = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        dataChart.setBarGrouping(BarGrouping.CLUSTERED);
        dataChart.setBarDirection(BarDirection.COL);for (Map.Entry<String, XDDFNumericalDataSource<Double>> ds : valueCellRangeAddressList.entrySet()) {
            addFilteredSeries(dataChart, category, ds.getValue(), ds.getKey(), bindDataCols);
        }
        chart.plot(dataChart);
        applyBarChartsColors(chart, dataChart);for (CTBarSer ctBarSer : chart.getCTChart().getPlotArea().getBarChartArray(0).getSerArray()) {
            CTDLbls ctdLbls = ctBarSer.addNewDLbls();
            ctdLbls.addNewShowVal().setVal(true);
            ctdLbls.addNewShowLegendKey().setVal(false);
            ctdLbls.addNewShowCatName().setVal(false);
            ctdLbls.addNewShowSerName().setVal(false);
            ctdLbls.addNewDLblPos().setVal(STDLblPos.OUT_END);
        }if (showLegend) {
            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.RIGHT);
        }
    }

    private static void addFilteredSeries(XDDFBarChartData chartData,
                                          XDDFDataSource<String> categories,
                                          XDDFNumericalDataSource<Double> values,
                                          String title,
                                          int[] includedIndices) {
        String[] filteredCategories = new String[includedIndices.length];
        for (int i = 0; i < includedIndices.length; i++) {
            filteredCategories[i] = categories.getPointAt(includedIndices[i]);
        }
        XDDFDataSource<String> filteredCatSource = XDDFDataSourcesFactory.fromArray(filteredCategories);
        XDDFChartData.Series series = chartData.addSeries(filteredCatSource, values);
        series.setTitle(title, null);
    }private void applyBarChartsColors(XSSFChart chart, XDDFChartData data) {
        List<XDDFChartData.Series> seriesList = data.getSeries();
        for (int i = 0; i < seriesList.size(); i++) {
            setSeriesColor(seriesList.get(i), ECHARTS_COLORS[i % ECHARTS_COLORS.length]);
        }
    }private void setSeriesColor(XDDFChartData.Series series, byte[] hexColor) {
        XDDFColor color = XDDFColor.from(hexColor);
        XDDFSolidFillProperties fill = new XDDFSolidFillProperties(color);
        XDDFLineProperties line = new XDDFLineProperties();
        line.setFillProperties(fill);
        line.setWidth(2.0);
        XDDFShapeProperties properties = series.getShapeProperties();
        if (properties == null) {
            properties = new XDDFShapeProperties();
        }
        properties.setLineProperties(line);
        properties.setFillProperties(fill);
        series.setShapeProperties(properties);
    }

    private static double strPercentage2Double(String value) {
        return Double.parseDouble(value.replace("%", "")) * 0.01;
    }private XSSFCellStyle createTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFillForegroundColor(new XSSFColor(new byte[] {(byte) 0x60, (byte) 0x62, (byte) 0x66}, null));
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font titleFont = wb.createFont();
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setBorderBottom(BorderStyle.THIN);
        titleStyle.setBorderLeft(BorderStyle.THIN);
        titleStyle.setBorderRight(BorderStyle.THIN);
        titleStyle.setBorderTop(BorderStyle.THIN);
        return titleStyle;
    }private XSSFCellStyle createDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle dataStyle = wb.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.RIGHT);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        return dataStyle;
    }private XSSFCellStyle createPercentageStyle(XSSFWorkbook wb) {
        XSSFCellStyle percentageStyle = wb.createCellStyle();
        percentageStyle.setDataFormat(wb.createDataFormat().getFormat("0.00%"));
        percentageStyle.setAlignment(HorizontalAlignment.RIGHT);
        percentageStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        percentageStyle.setBorderBottom(BorderStyle.THIN);
        percentageStyle.setBorderLeft(BorderStyle.THIN);
        percentageStyle.setBorderRight(BorderStyle.THIN);
        percentageStyle.setBorderTop(BorderStyle.THIN);
        return percentageStyle;
    }private XSSFCellStyle createTimeStyle(XSSFWorkbook wb) {
        XSSFCellStyle timeStyle = wb.createCellStyle();
        timeStyle.setAlignment(HorizontalAlignment.RIGHT);
        timeStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        timeStyle.setBorderBottom(BorderStyle.THIN);
        timeStyle.setBorderLeft(BorderStyle.THIN);
        timeStyle.setBorderRight(BorderStyle.THIN);
        timeStyle.setBorderTop(BorderStyle.THIN);
        DataFormat dataFormat = wb.createDataFormat();
        timeStyle.setDataFormat(dataFormat.getFormat("yyyy-MM-dd HH:mm:ss"));
        return timeStyle;
    }
}
