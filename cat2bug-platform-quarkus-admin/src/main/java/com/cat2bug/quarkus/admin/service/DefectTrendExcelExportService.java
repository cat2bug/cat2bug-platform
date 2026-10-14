package com.cat2bug.quarkus.admin.service;

import com.cat2bug.quarkus.common.constant.DefectConstants;
import com.cat2bug.quarkus.common.domain.dashboard.SysDefectLine;
import com.cat2bug.quarkus.common.domain.dashboard.SysMemberOfDefectsLine;
import com.cat2bug.quarkus.common.excel.ExcelChart;
import com.cat2bug.quarkus.common.excel.ExcelChartSeries;
import com.cat2bug.quarkus.common.excel.FastExcelSupport;
import com.cat2bug.quarkus.common.excel.XlsxPatcher;
import com.cat2bug.quarkus.system.mapper.SysDashboardMapper;
import com.cat2bug.quarkus.system.util.SysDefectTrendLineHelper;
import com.cat2bug.quarkus.system.util.SysMemberDefectLineHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.dhatim.fastexcel.BorderStyle;
import org.dhatim.fastexcel.ConditionalFormattingExpressionRule;
import org.dhatim.fastexcel.Worksheet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 缺陷 / 成员走势 Excel 导出（与 Spring DefectTrendExcelExport 结构一致）。
 */
@ApplicationScoped
public class DefectTrendExcelExportService {

    private static final Map<String, String> STATE_LABELS = Map.of(
            "PROCESSING", "处理中",
            "AUDIT", "待验证",
            "RESOLVED", "已解决",
            "REJECTED", "已驳回",
            "CLOSED", "已关闭"
    );

    private static final double DEFAULT_COLUMN_WIDTH = 12;
    /** 首行预留给走势图的高度（磅）。 */
    private static final double CHART_ROW_HEIGHT = 250;
    private static final String TITLE_FILL = "606266";
    private static final String TITLE_FONT = "FFFFFF";
    private static final String ZERO_FILL = "F56C6C";
    private static final String ZERO_FONT = "FFFFFF";

    @Inject
    SysDashboardMapper sysDashboardMapper;

    public byte[] exportDefectStateLine(Long projectId, String timeType) {
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
        Map<Integer, List<SysDefectLine>> defectStateGroup = defectLineList.stream()
                .collect(Collectors.groupingBy(SysDefectLine::getDefectState));
        List<Map<String, Object>> list = new LinkedList<>();
        for (int i = 0; i < DefectConstants.DEFECT_STATES.length; i++) {
            if (!defectStateGroup.containsKey(i)) {
                continue;
            }
            List<SysDefectLine> defectLines = defectStateGroup.get(i);
            String strState = DefectConstants.DEFECT_STATES[i];
            Map<String, Object> map = new HashMap<>();
            map.put("name", strState);
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
        if (SysMemberDefectLineHelper.MONTH_TYPE.equals(timeType)) {
            sheetName = "成员处理缺陷月走势图";
            buildMemberMonthTimeSet(timeSet, rawList);
        } else {
            sheetName = "成员处理缺陷30天走势图";
            buildMemberDayTimeSet(timeSet, rawList);
        }
        return writeMemberDefectLineWorkbook(sheetName, timeSet, rawList);
    }

    private byte[] writeDefectStateLineWorkbook(String sheetName, Set<String> timeSet,
                                                List<Map<String, Object>> list) {
        List<ExcelChartSeries> series = new ArrayList<>();
        byte[] xlsx = FastExcelSupport.writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet(sheetName);
            int rowNum = -1;
            reserveChartRow(sheet, ++rowNum);
            int titleRow = ++rowNum;
            writeTitleCell(sheet, titleRow, 0, "状态");
            int colNum = 1;
            for (String s : timeSet) {
                writeTitleCell(sheet, titleRow, colNum++, s);
            }
            for (Map<String, Object> map : list) {
                colNum = 0;
                int row = ++rowNum;
                sheet.value(row, colNum, STATE_LABELS.getOrDefault(String.valueOf(map.get("name")),
                        String.valueOf(map.get("name"))));
                applyDataStyle(sheet, row, colNum++);
                List<Long> values = new ArrayList<>();
                for (String s : timeSet) {
                    Long count = (Long) map.get(s);
                    values.add(count);
                    sheet.value(row, colNum, count);
                    applyDataStyle(sheet, row, colNum++);
                }
                series.add(new ExcelChartSeries(
                        STATE_LABELS.getOrDefault(String.valueOf(map.get("name")), String.valueOf(map.get("name"))),
                        ExcelChartSeries.ref(sheetName, titleRow, 1, titleRow, timeSet.size()),
                        new ArrayList<>(timeSet),
                        ExcelChartSeries.ref(sheetName, row, 1, row, timeSet.size()),
                        values));
            }
            setColumnWidths(sheet, timeSet.size() + 1);
            addZeroConditionalFormat(sheet, rowNum, timeSet.size());
        }, "导出缺陷状态走势失败");
        return patchLineChart(xlsx, sheetName, timeSet.size() + 1, series, "导出缺陷状态走势失败");
    }

    private byte[] writeMemberDefectLineWorkbook(String sheetName, Set<String> timeSet,
                                                 List<SysMemberOfDefectsLine> list) {
        List<ExcelChartSeries> series = new ArrayList<>();
        byte[] xlsx = FastExcelSupport.writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet(sheetName);
            int rowNum = -1;
            reserveChartRow(sheet, ++rowNum);
            int titleRow = ++rowNum;
            writeTitleCell(sheet, titleRow, 0, "成员");
            int colNum = 1;
            for (String s : timeSet) {
                writeTitleCell(sheet, titleRow, colNum++, s);
            }
            writeTitleCell(sheet, titleRow, colNum, "总数");
            Map<String, List<SysMemberOfDefectsLine>> map = list.stream().collect(Collectors.groupingBy(
                    SysMemberOfDefectsLine::getNickName, LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<String, List<SysMemberOfDefectsLine>> item : map.entrySet()) {
                colNum = 0;
                int row = ++rowNum;
                sheet.value(row, colNum, item.getKey());
                applyDataStyle(sheet, row, colNum++);
                Map<String, SysMemberOfDefectsLine> mdlMap = item.getValue().stream()
                        .collect(Collectors.toMap(SysMemberOfDefectsLine::getCreateTime, o -> o, (a, b) -> a));
                List<Integer> values = new ArrayList<>();
                for (String s : timeSet) {
                    int count = mdlMap.containsKey(s) ? mdlMap.get(s).getDefectTodayCount() : 0;
                    values.add(count);
                    sheet.value(row, colNum, count);
                    applyDataStyle(sheet, row, colNum++);
                }
                series.add(new ExcelChartSeries(item.getKey(),
                        ExcelChartSeries.ref(sheetName, titleRow, 1, titleRow, timeSet.size()),
                        new ArrayList<>(timeSet),
                        ExcelChartSeries.ref(sheetName, row, 1, row, timeSet.size()),
                        values));
                String startColString = columnName(1);
                String endColString = columnName(colNum - 1);
                sheet.formula(row, colNum,
                        "SUM(" + startColString + (rowNum + 1) + ":" + endColString + (rowNum + 1) + ")");
                applyDataStyle(sheet, row, colNum);
            }
            setColumnWidths(sheet, timeSet.size() + 2);
            addZeroConditionalFormat(sheet, rowNum, timeSet.size() + 1);
        }, "导出成员缺陷走势失败");
        return patchLineChart(xlsx, sheetName, timeSet.size() + 2, series, "导出成员缺陷走势失败");
    }

    /** 走势图占满首行（第 0 行）、横跨 0..chartCols 列；无数据时与原实现一致不画图。 */
    private byte[] patchLineChart(byte[] xlsx, String sheetName, int chartCols,
                                  List<ExcelChartSeries> series, String errorMessage) {
        XlsxPatcher patcher = XlsxPatcher.of().baseColumnWidth(1, DEFAULT_COLUMN_WIDTH);
        if (!series.isEmpty() && !series.get(0).categories().isEmpty()) {
            patcher.chart(1, new ExcelChart(ExcelChart.Type.LINE, sheetName, 0, 0, chartCols, 1,
                    "缺陷数量", ExcelChart.Legend.TOP, series));
        }
        return patcher.apply(xlsx, errorMessage);
    }

    /** 空行不会被 FastExcel 写出，需放一个无值单元格才能保留行高。 */
    private void reserveChartRow(Worksheet sheet, int row) {
        sheet.rowHeight(row, CHART_ROW_HEIGHT);
        sheet.style(row, 0).set();
    }

    private void writeTitleCell(Worksheet sheet, int row, int col, String text) {
        sheet.value(row, col, text);
        sheet.style(row, col)
                .fillColor(TITLE_FILL)
                .fontColor(TITLE_FONT)
                .horizontalAlignment("right")
                .borderStyle(BorderStyle.THIN)
                .set();
    }

    private void applyDataStyle(Worksheet sheet, int row, int col) {
        sheet.style(row, col)
                .horizontalAlignment("right")
                .borderStyle(BorderStyle.THIN)
                .set();
    }

    private void setColumnWidths(Worksheet sheet, int columns) {
        for (int col = 0; col < columns; col++) {
            sheet.width(col, DEFAULT_COLUMN_WIDTH);
        }
    }

    private void addZeroConditionalFormat(Worksheet sheet, int rowNum, int colSpan) {
        if (rowNum < 2 || colSpan < 1) {
            return;
        }
        // FastExcel 不转义条件表达式，写成 0>=X 以避开 XML 中的 '<'
        String expression = "0>=" + columnName(1) + 3;
        sheet.range(2, 1, rowNum, colSpan).style()
                .fillColor(ZERO_FILL)
                .fontColor(ZERO_FONT)
                .set(new ConditionalFormattingExpressionRule(expression, false));
    }

    private static String columnName(int columnIndex) {
        StringBuilder name = new StringBuilder();
        int index = columnIndex + 1;
        while (index > 0) {
            int rem = (index - 1) % 26;
            name.insert(0, (char) ('A' + rem));
            index = (index - 1) / 26;
        }
        return name.toString();
    }

    private void buildDayTimeSet(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildDefectStateLineResult(defectLineList, "day");
        timeSet.addAll((Collection<? extends String>) built.get("times"));
    }

    private void buildMonthTimeSet(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildDefectStateLineResult(defectLineList,
                SysDefectTrendLineHelper.MONTH_TYPE);
        timeSet.addAll((Collection<? extends String>) built.get("times"));
    }

    private void buildMemberDayTimeSet(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        Map<String, Object> built = SysMemberDefectLineHelper.buildMemberDefectLineResult(list, "day");
        timeSet.addAll((Collection<? extends String>) built.get("time"));
    }

    private void buildMemberMonthTimeSet(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        Map<String, Object> built = SysMemberDefectLineHelper.buildMemberDefectLineResult(list,
                SysMemberDefectLineHelper.MONTH_TYPE);
        timeSet.addAll((Collection<? extends String>) built.get("time"));
    }
}
