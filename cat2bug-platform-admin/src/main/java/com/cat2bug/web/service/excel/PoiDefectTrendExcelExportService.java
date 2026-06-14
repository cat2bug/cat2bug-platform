package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.system.domain.SysDefectLine;
import com.cat2bug.system.domain.SysMemberOfDefectsLine;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.mapper.SysDashboardMapper;
import com.cat2bug.system.util.SysDefectTrendLineHelper;
import com.cat2bug.system.util.SysDefectTrendLineHelper;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingRule;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFontFormatting;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSheetConditionalFormatting;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;/**
 * 缺陷 / 成员走势 Excel 导出（与 Spring DefectTrendExcelExport 结构一致）。
 */
@Profile("!native")
@Service
public class PoiDefectTrendExcelExportService implements DefectTrendExcelExportService {private static final byte[][] ECHARTS_COLORS = {
            new byte[] {(byte) 0x2e, (byte) 0xc7, (byte) 0xc9},
            new byte[] {(byte) 0xb6, (byte) 0xa2, (byte) 0xde},
            new byte[] {(byte) 0x5a, (byte) 0xb1, (byte) 0xef},
            new byte[] {(byte) 0xff, (byte) 0xb9, (byte) 0x80},
            new byte[] {(byte) 0xd8, 0x7a, (byte) 0x80},
            new byte[] {(byte) 0x8d, (byte) 0x98, (byte) 0xb3},
            new byte[] {(byte) 0xe5, (byte) 0xcf, 0x0d},
            new byte[] {(byte) 0x97, (byte) 0xb5, 0x52},
    };private static final Map<String, String> STATE_LABELS = Map.of(
            "PROCESSING", "处理中",
            "AUDIT", "待验证",
            "RESOLVED", "已解决",
            "REJECTED", "已驳回",
            "CLOSED", "已关闭"
    );@Autowired
    SysDashboardMapper sysDashboardMapper;public byte[] exportDefectStateLine(Long projectId, String timeType) {
        List<SysDefectLine> defectLineList = sysDashboardMapper.defectLine(projectId, timeType);
        String sheetName;
        Set<String> timeSet = new LinkedHashSet<>();
        if (SysDefectTrendLineHelper.MONTH_TYPE.equals(timeType)) {
            sheetName = "缺陷状态变化月走势图";
            buildMonthTimeSet(timeSet, defectLineList);
        } else {
            sheetName = "缺陷状态变化30天走势图";
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
            Map<String, Object> map = new HashMap<>();
            map.put("name", state.name());
            list.add(map);
            for (String s : timeSet) {
                Optional<SysDefectLine> defectLine = defectLines.stream()
                        .filter(d -> d.getDefectTime().equals(s)).findFirst();
                map.put(s, defectLine.isPresent() ? defectLine.get().getDefectCount() : 0L);
            }
        }
        return writeDefectStateLineWorkbook(sheetName, timeSet, list);
    }

    public byte[] exportMemberDefectLine(Long projectId, String timeType) {
        List<SysMemberOfDefectsLine> rawList = sysDashboardMapper.memberOfDefectsLine(projectId, timeType);
        String sheetName;
        Set<String> timeSet = new LinkedHashSet<>();
        if (SysDefectTrendLineHelper.MONTH_TYPE.equals(timeType)) {
            sheetName = "成员处理缺陷月走势图";
            buildMemberMonthTimeSet(timeSet, rawList);
        } else {
            sheetName = "成员处理缺陷30天走势图";
            buildMemberDayTimeSet(timeSet, rawList);
        }
        return writeMemberDefectLineWorkbook(sheetName, timeSet, rawList);
    }private byte[] writeDefectStateLineWorkbook(String sheetName, Set<String> timeSet,
                                                List<Map<String, Object>> list) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet();
            wb.setSheetName(0, sheetName);
            sheet.setDefaultColumnWidth(12);
            int rowNum = -1;
            sheet.createRow(++rowNum).setHeight((short) 5000);
            Row rowTitle = sheet.createRow(++rowNum);
            XSSFCellStyle titleStyle = createTitleStyle(wb);
            XSSFCellStyle dataStyle = createDataStyle(wb);
            Cell cell1 = rowTitle.createCell(0);
            cell1.setCellValue("状态");
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
                cellName.setCellValue(STATE_LABELS.getOrDefault(String.valueOf(map.get("name")),
                        String.valueOf(map.get("name"))));
                cellName.setCellStyle(dataStyle);
                for (String s : timeSet) {
                    Cell cell = row.createCell(colNum++);
                    cell.setCellValue((Long) map.get(s));
                    cell.setCellStyle(dataStyle);
                }
            }
            addZeroConditionalFormat(sheet, rowNum, timeSet.size());
            drawStateLineChart(sheet, sheetName, timeSet, list);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ServiceException("导出缺陷状态走势失败");
        }
    }private byte[] writeMemberDefectLineWorkbook(String sheetName, Set<String> timeSet,
                                                 List<SysMemberOfDefectsLine> list) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet();
            wb.setSheetName(0, sheetName);
            sheet.setDefaultColumnWidth(12);
            int rowNum = -1;
            sheet.createRow(++rowNum).setHeight((short) 5000);
            Row rowTitle = sheet.createRow(++rowNum);
            XSSFCellStyle titleStyle = createTitleStyle(wb);
            XSSFCellStyle dataStyle = createDataStyle(wb);
            Cell cell1 = rowTitle.createCell(0);
            cell1.setCellValue("成员");
            cell1.setCellStyle(titleStyle);
            int colNum = 1;
            for (String s : timeSet) {
                Cell cell = rowTitle.createCell(colNum++);
                cell.setCellValue(s);
                cell.setCellStyle(titleStyle);
            }
            Cell cellTotal = rowTitle.createCell(colNum);
            cellTotal.setCellValue("总数");
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
            drawMemberLineChart(sheet, sheetName, timeSet, map);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ServiceException("导出成员缺陷走势失败");
        }
    }private void drawStateLineChart(XSSFSheet sheet, String sheetName, Set<String> timeSet,
                                    List<Map<String, Object>> list) {
        if (list.isEmpty() || timeSet.isEmpty()) {
            return;
        }
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 0, timeSet.size() + 1, 1);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(sheetName);
        chart.setTitleOverlay(false);
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP);
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("缺陷数量");
        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        XDDFDataSource<String> dates = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                new CellRangeAddress(1, 1, 1, timeSet.size()));
        int seriesRowNum = 1;
        for (Map<String, Object> stateMap : list) {
            seriesRowNum++;
            String title = STATE_LABELS.getOrDefault(String.valueOf(stateMap.get("name")),
                    String.valueOf(stateMap.get("name")));
            XDDFNumericalDataSource<Double> stateData = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                    new CellRangeAddress(seriesRowNum, seriesRowNum, 1, timeSet.size()));
            XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(dates, stateData);
            series.setTitle(title, null);
            series.setSmooth(true);
            series.setMarkerStyle(MarkerStyle.CIRCLE);
        }
        applyLineChartsColors(chart, data);
        chart.plot(data);
    }private void drawMemberLineChart(XSSFSheet sheet, String sheetName, Set<String> timeSet,
                                     Map<String, List<SysMemberOfDefectsLine>> map) {
        if (map.isEmpty() || timeSet.isEmpty()) {
            return;
        }
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 0, timeSet.size() + 2, 1);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(sheetName);
        chart.setTitleOverlay(false);
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP);
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("缺陷数量");
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
    }private void applyLineChartsColors(XSSFChart chart, XDDFChartData data) {
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
    }private XSSFCellStyle createTitleStyle(XSSFWorkbook wb) {
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
    }private XSSFCellStyle createDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle dataStyle = wb.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.RIGHT);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        return dataStyle;
    }private void addZeroConditionalFormat(XSSFSheet sheet, int rowNum, int colSpan) {
        if (rowNum < 2 || colSpan < 1) {
            return;
        }
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
    }private void buildDayTimeSet(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildDefectStateLineResult(defectLineList, "day");
        timeSet.addAll((Collection<? extends String>) built.get("times"));
    }private void buildMonthTimeSet(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildDefectStateLineResult(defectLineList,
                SysDefectTrendLineHelper.MONTH_TYPE);
        timeSet.addAll((Collection<? extends String>) built.get("times"));
    }private void buildMemberDayTimeSet(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildMemberDefectLineResult(list, "day");
        timeSet.addAll((Collection<? extends String>) built.get("time"));
    }private void buildMemberMonthTimeSet(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildMemberDefectLineResult(list,
                SysDefectTrendLineHelper.MONTH_TYPE);
        timeSet.addAll((Collection<? extends String>) built.get("time"));
    }
}
