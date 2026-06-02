package com.cat2bug.web.service;

import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.system.domain.SysDefectLine;
import com.cat2bug.system.domain.SysMemberOfDefectsLine;
import com.cat2bug.system.service.ISysDashboardService;
import com.cat2bug.system.util.SysDefectTrendLineHelper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xddf.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

import static com.cat2bug.common.utils.poi.ExcelUtil.applyLineChartsColors;

/**
 * 缺陷 / 成员走势 Excel 导出（与 SysDashboardController 导出结构一致）
 */
@Service
public class DefectTrendExcelExport {

    @Autowired
    private ISysDashboardService sysDashboardService;

    public void exportDefectStateLine(HttpServletResponse response, Long projectId, String timeType) throws Exception {
        List<SysDefectLine> defectLineList = sysDashboardService.defectLine(projectId, timeType);
        String sheetName;
        Set<String> timeSet = new LinkedHashSet<>();
        if (SysDefectTrendLineHelper.MONTH_TYPE.equals(timeType)) {
            sheetName = MessageUtils.message("dashboard.defect-line.month");
            buildMonthTimeSet(timeSet, defectLineList);
        } else {
            sheetName = MessageUtils.message("dashboard.defect-line.day");
            buildDayTimeSet(timeSet, defectLineList);
        }
        Map<SysDefectStateEnum, List<SysDefectLine>> defectStateGroup = defectLineList.stream()
                .collect(Collectors.groupingBy(SysDefectLine::getDefectState));
        List<Map<String, Object>> list = new LinkedList<>();
        for (SysDefectStateEnum state : SysDefectStateEnum.values()) {
            if (!defectStateGroup.containsKey(state)) {
                continue;
            }
            List<SysDefectLine> defectLines = defectStateGroup.get(state);
            String strState = state.name();
            Map<String, Object> map = new HashMap<>();
            map.put("name", strState);
            list.add(map);
            for (String s : timeSet) {
                Optional<SysDefectLine> defectLine = defectLines.stream()
                        .filter(d -> d.getDefectTime().equals(s)).findFirst();
                map.put(s, defectLine.isPresent() ? defectLine.get().getDefectCount() : 0L);
            }
        }
        writeDefectStateLineWorkbook(response, sheetName, timeSet, list);
    }

    public void exportMemberDefectLine(HttpServletResponse response, Long projectId, String timeType) throws Exception {
        List<SysMemberOfDefectsLine> rawList = sysDashboardService.memberOfDefectsLine(projectId, timeType);
        String sheetName;
        Set<String> timeSet = new LinkedHashSet<>();
        if (SysDefectTrendLineHelper.MONTH_TYPE.equals(timeType)) {
            sheetName = MessageUtils.message("dashboard.member-defect-line.month");
            buildMemberMonthTimeSet(timeSet, rawList);
        } else {
            sheetName = MessageUtils.message("dashboard.member-defect-line.day");
            buildMemberDayTimeSet(timeSet, rawList);
        }
        writeMemberDefectLineWorkbook(response, sheetName, timeSet, rawList);
    }

    private void writeDefectStateLineWorkbook(HttpServletResponse response, String sheetName,
                                             Set<String> timeSet, List<Map<String, Object>> list) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet();
        wb.setSheetName(0, sheetName);
        sheet.setDefaultColumnWidth(12);
        int rowNum = -1;
        sheet.createRow(++rowNum).setHeight((short) 5000);
        Row rowTitle = sheet.createRow(++rowNum);
        XSSFCellStyle titleStyle = createTitleStyle(wb);
        XSSFCellStyle dataStyle = createDataStyle(wb);
        Cell cell1 = rowTitle.createCell(0);
        cell1.setCellValue(MessageUtils.message("defect.state"));
        cell1.setCellStyle(titleStyle);
        int colNum = 1;
        for (String s : timeSet) {
            Cell cell = rowTitle.createCell(colNum++);
            cell.setCellValue(s);
            cell.setCellStyle(titleStyle);
        }
        for (Map<String, Object> map : list) {
            colNum = 0;
            Row row = sheet.createRow(++rowNum);
            Cell cellName = row.createCell(colNum++);
            cellName.setCellValue(MessageUtils.message(String.valueOf(map.get("name"))));
            cellName.setCellStyle(dataStyle);
            for (String s : timeSet) {
                Cell cell = row.createCell(colNum++);
                cell.setCellValue((Long) map.get(s));
                cell.setCellStyle(dataStyle);
            }
        }
        addZeroConditionalFormat(sheet, rowNum, timeSet.size());
        drawStateLineChart(sheet, sheetName, timeSet, rowNum, list);
        wb.write(response.getOutputStream());
        IOUtils.closeQuietly(wb);
    }

    private void writeMemberDefectLineWorkbook(HttpServletResponse response, String sheetName,
                                               Set<String> timeSet, List<SysMemberOfDefectsLine> list) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet();
        wb.setSheetName(0, sheetName);
        sheet.setDefaultColumnWidth(12);
        int rowNum = -1;
        sheet.createRow(++rowNum).setHeight((short) 5000);
        Row rowTitle = sheet.createRow(++rowNum);
        XSSFCellStyle titleStyle = createTitleStyle(wb);
        XSSFCellStyle dataStyle = createDataStyle(wb);
        Cell cell1 = rowTitle.createCell(0);
        cell1.setCellValue(MessageUtils.message("member"));
        cell1.setCellStyle(titleStyle);
        int colNum = 1;
        for (String s : timeSet) {
            Cell cell = rowTitle.createCell(colNum++);
            cell.setCellValue(s);
            cell.setCellStyle(titleStyle);
        }
        Cell cellTotal = rowTitle.createCell(colNum);
        cellTotal.setCellValue(MessageUtils.message("total"));
        cellTotal.setCellStyle(titleStyle);
        Map<String, List<SysMemberOfDefectsLine>> map = list.stream().collect(Collectors.groupingBy(
                SysMemberOfDefectsLine::getNickName, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<SysMemberOfDefectsLine>> item : map.entrySet()) {
            colNum = 0;
            Row row = sheet.createRow(++rowNum);
            Cell cellName = row.createCell(colNum++);
            cellName.setCellValue(item.getKey());
            cellName.setCellStyle(dataStyle);
            Map<String, SysMemberOfDefectsLine> mdlMap = item.getValue().stream()
                    .collect(Collectors.toMap(SysMemberOfDefectsLine::getCreateTime, o -> o, (a, b) -> a));
            for (String s : timeSet) {
                Cell cell = row.createCell(colNum++);
                int count = mdlMap.containsKey(s) ? mdlMap.get(s).getDefectTodayCount() : 0;
                cell.setCellValue(count);
                cell.setCellStyle(dataStyle);
            }
            Cell cell = row.createCell(colNum);
            cell.setCellStyle(dataStyle);
            String startColString = CellReference.convertNumToColString(1);
            String endColString = CellReference.convertNumToColString(colNum - 1);
            cell.setCellFormula("SUM(" + startColString + (rowNum + 1) + ":" + endColString + (rowNum + 1) + ")");
        }
        addZeroConditionalFormat(sheet, rowNum, timeSet.size() + 1);
        drawMemberLineChart(sheet, sheetName, timeSet, rowNum, map);
        wb.write(response.getOutputStream());
        IOUtils.closeQuietly(wb);
    }

    private void drawStateLineChart(XSSFSheet sheet, String sheetName, Set<String> timeSet, int rowNum,
                                    List<Map<String, Object>> list) throws Exception {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 0, timeSet.size() + 1, 1);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(sheetName);
        chart.setTitleOverlay(false);
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP);
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle(MessageUtils.message("defect.count"));
        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        XDDFDataSource<String> dates = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                new CellRangeAddress(1, 1, 1, timeSet.size()));
        int seriesRowNum = 1;
        for (Map<String, Object> map : list) {
            seriesRowNum++;
            String title = MessageUtils.message(String.valueOf(map.get("name")));
            XDDFNumericalDataSource<Double> stateData = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                    new CellRangeAddress(seriesRowNum, seriesRowNum, 1, timeSet.size()));
            XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(dates, stateData);
            series.setTitle(title, null);
            series.setSmooth(true);
            series.setMarkerStyle(MarkerStyle.CIRCLE);
        }
        applyLineChartsColors(chart, data);
        chart.plot(data);
    }

    private void drawMemberLineChart(XSSFSheet sheet, String sheetName, Set<String> timeSet, int rowNum,
                                     Map<String, List<SysMemberOfDefectsLine>> map) throws Exception {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 0, timeSet.size() + 2, 1);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(sheetName);
        chart.setTitleOverlay(false);
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP);
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle(MessageUtils.message("defect.count"));
        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        XDDFDataSource<String> dates = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                new CellRangeAddress(1, 1, 1, timeSet.size()));
        int seriesRowNum = 1;
        for (Map.Entry<String, List<SysMemberOfDefectsLine>> item : map.entrySet()) {
            seriesRowNum++;
            XDDFNumericalDataSource<Double> stateData = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                    new CellRangeAddress(seriesRowNum, seriesRowNum, 1, timeSet.size()));
            XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(dates, stateData);
            series.setTitle(item.getKey(), null);
            series.setSmooth(true);
            series.setMarkerStyle(MarkerStyle.CIRCLE);
        }
        applyLineChartsColors(chart, data);
        chart.plot(data);
    }

    private XSSFCellStyle createTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x60, (byte) 0x62, (byte) 0x66}, null));
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font titleFont = wb.createFont();
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.RIGHT);
        titleStyle.setBorderBottom(BorderStyle.THIN);
        titleStyle.setBorderLeft(BorderStyle.THIN);
        titleStyle.setBorderRight(BorderStyle.THIN);
        titleStyle.setBorderTop(BorderStyle.THIN);
        return titleStyle;
    }

    private XSSFCellStyle createDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle dataStyle = wb.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.RIGHT);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        return dataStyle;
    }

    private void addZeroConditionalFormat(XSSFSheet sheet, int rowNum, int colSpan) {
        XSSFSheetConditionalFormatting cf = sheet.getSheetConditionalFormatting();
        XSSFConditionalFormattingRule rule = cf.createConditionalFormattingRule(ComparisonOperator.LE, "0");
        PatternFormatting fillLE0 = rule.createPatternFormatting();
        XSSFColor redColor = new XSSFColor(new byte[]{(byte) 0xF5, (byte) 0x6C, (byte) 0x6C}, null);
        XSSFColor whiteColor = new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null);
        fillLE0.setFillBackgroundColor(redColor);
        fillLE0.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());
        XSSFFontFormatting fontLE0 = rule.createFontFormatting();
        fontLE0.setFontColor(whiteColor);
        CellRangeAddress[] regions = {new CellRangeAddress(2, rowNum, 1, colSpan)};
        cf.addConditionalFormatting(regions, rule);
    }

    private void buildDayTimeSet(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildDefectStateLineResult(defectLineList, "day");
        timeSet.addAll((Collection<? extends String>) built.get("times"));
    }

    private void buildMonthTimeSet(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildDefectStateLineResult(defectLineList, SysDefectTrendLineHelper.MONTH_TYPE);
        timeSet.addAll((Collection<? extends String>) built.get("times"));
    }

    private void buildMemberDayTimeSet(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildMemberDefectLineResult(list, "day");
        timeSet.addAll((Collection<? extends String>) built.get("time"));
    }

    private void buildMemberMonthTimeSet(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildMemberDefectLineResult(list, SysDefectTrendLineHelper.MONTH_TYPE);
        timeSet.addAll((Collection<? extends String>) built.get("time"));
    }
}
