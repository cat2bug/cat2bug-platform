package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.*;
import com.cat2bug.system.service.ISysDashboardService;
import com.cat2bug.system.service.ISysPlanService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.cat2bug.common.utils.poi.ExcelUtil.applyLineChartsColors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-01-19 04:49
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/system/dashboard")
public class SysDashboardController {
    private final static String MONTH_TYPE = "month";
    @Autowired
    ISysDashboardService sysDashboardService;
    @Autowired
    ISysPlanService sysPlanService;

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/case")
    public AjaxResult caseStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.caseStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/defect")
    public AjaxResult defectStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.defectStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/module")
    public AjaxResult moduleStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.moduleStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/report")
    public AjaxResult reportStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.reportStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/document")
    public AjaxResult documentStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.documentStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/member")
    public AjaxResult memberStatisticsChart(@PathVariable("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.memberStatistics(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/defect-line")
    public AjaxResult defectLine(@PathVariable("projectId") Long projectId, @RequestParam("timeType") String timeType) {
        List<SysDefectLine> defectLineList = sysDashboardService.defectLine(projectId, timeType);
        Set<String> timeSet = new LinkedHashSet<>();
        switch (timeType) {
            case MONTH_TYPE:
                setCountOfMonth(timeSet, defectLineList);
                break;
            default:
                setCountOfDay(timeSet, defectLineList);
                break;
        }
        Map<SysDefectStateEnum, List<SysDefectLine>> defectStateGroup = defectLineList.stream().collect(Collectors.groupingBy(SysDefectLine::getDefectState));
        Map<String, List<Long>> defectStateLines = new HashMap<>();
        defectStateGroup.forEach((k,v)->{
            defectStateLines.put(k.name(), new ArrayList<>());
            for (String s : timeSet) {
                Optional<SysDefectLine> defectLine = v.stream().filter(d->d.getDefectTime().equals(s)).findFirst();
                if(defectLine.isPresent()) {
                    defectStateLines.get(k.name()).add(defectLine.get().getDefectCount());
                } else {
                    defectStateLines.get(k.name()).add(0L);
                }
            }
        });
        Map<String, Object> ret = new HashMap<>();
        ret.put("data", defectStateLines);              // 按状态类型分组的日完成数量
        ret.put("types", SysDefectStateEnum.values());  // 状态分类
        ret.put("times", timeSet);                      // 时间数组
        return AjaxResult.success(ret);
    }

    /**
     * 缺陷状态30天走势图
     */
    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @Log(title = "缺陷", businessType = BusinessType.EXPORT)
    @PostMapping("/{projectId}/defect-line/export")
    public void defectLineExport(HttpServletResponse response, @PathVariable("projectId") Long projectId, @RequestParam("timeType") String timeType) {
        // 统计数据
        List<SysDefectLine> defectLineList = sysDashboardService.defectLine(projectId, timeType);
        String sheetName = null;
        Set<String> timeSet = new LinkedHashSet<>();
        switch (timeType) {
            case MONTH_TYPE:
                sheetName = MessageUtils.message("dashboard.defect-line.month");
                setCountOfMonth(timeSet, defectLineList);
                break;
            default:
                sheetName = MessageUtils.message("dashboard.defect-line.day");
                setCountOfDay(timeSet, defectLineList);
                break;
        }
        Map<SysDefectStateEnum, List<SysDefectLine>> defectStateGroup = defectLineList.stream().collect(Collectors.groupingBy(SysDefectLine::getDefectState));
        Map<String, List<Long>> defectStateLines = new HashMap<>();
        List<Map> list = new LinkedList<>();
        for(SysDefectStateEnum state : SysDefectStateEnum.values()) {
            if(defectStateGroup.containsKey(state)==false) continue;
            List<SysDefectLine> defectLines = defectStateGroup.get(state);
            String strState = state.name();
            Map<String, Object> map = new HashMap<>();
            map.put("name", strState);
            list.add(map);
            defectStateLines.put(strState, new ArrayList<>());
            for (String s : timeSet) {
                Optional<SysDefectLine> defectLine = defectLines.stream().filter(d -> d.getDefectTime().equals(s)).findFirst();
                if (defectLine.isPresent()) {
                    map.put(s, defectLine.get().getDefectCount());
                    defectStateLines.get(strState).add(defectLine.get().getDefectCount());
                } else {
                    map.put(s, 0L);
                    defectStateLines.get(strState).add(0L);
                }
            }
        }

        // 数据转excel
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet();
        wb.setSheetName(0, sheetName);
        sheet.setDefaultColumnWidth(12);
        try
        {
            int rowNum = -1;
            // 创建图表行
            Row rowChart = sheet.createRow(++rowNum);
            rowChart.setHeight((short)5000);
            // 创建标题行
            Row rowTitle = sheet.createRow(++rowNum);
            // 创建一个单元格样式
            XSSFCellStyle titleStyle = wb.createCellStyle();
            // 设置单元格背景色
            titleStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x60,(byte)0x62,(byte)0x66}, null));
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            // 设置标题字体
            Font titleFont = wb.createFont();
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.RIGHT);
            titleStyle.setBorderBottom(BorderStyle.THIN);
            titleStyle.setBorderLeft(BorderStyle.THIN);
            titleStyle.setBorderRight(BorderStyle.THIN);
            titleStyle.setBorderTop(BorderStyle.THIN);
            // 创建标题行的列
            Cell cell1 = rowTitle.createCell(0);
            cell1.setCellValue(MessageUtils.message("defect.state"));
            cell1.setCellStyle(titleStyle);
            int colNum = 1;
            for (String s : timeSet) {
                Cell cell = rowTitle.createCell(colNum++);
                cell.setCellValue(s);
                cell.setCellStyle(titleStyle);
            }

            // 创建数据行
            // 数据样式
            XSSFCellStyle dataStyle = wb.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.RIGHT);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            for(Map map : list) {
                colNum = 0;
                Row row = sheet.createRow(++rowNum);
                Cell cellName = row.createCell(colNum++);
                cellName.setCellValue(MessageUtils.message(String.valueOf(map.get("name"))));
                cellName.setCellStyle(dataStyle);
                for (String s : timeSet) {
                    Cell cell = row.createCell(colNum++);
                    cell.setCellValue((Long)map.get(s));
                    cell.setCellStyle(dataStyle);
                }
            }

            // 设置条件
            // 创建等于0的规则（灰色背景）
            XSSFSheetConditionalFormatting cf = sheet.getSheetConditionalFormatting();
            XSSFConditionalFormattingRule rule = cf.createConditionalFormattingRule(
                    ComparisonOperator.LE,
                    "0"); // 小于等于0
            PatternFormatting fillLE0 = rule.createPatternFormatting();
            XSSFColor redColor = new XSSFColor(new byte[]{(byte)0xF5, (byte)0x6C, (byte)0x6C}, null);
            XSSFColor whiteColor = new XSSFColor(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF}, null);
            fillLE0.setFillBackgroundColor(redColor);
            fillLE0.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());
            // 设置字体颜色为白色
            XSSFFontFormatting fontLE0 = rule.createFontFormatting();
            fontLE0.setFontColor(whiteColor);
            // 应用条件格式的范围
            CellRangeAddress[] regions = { new CellRangeAddress(2, rowNum, 1, timeSet.size())};
            // 添加条件格式规则到工作表
            cf.addConditionalFormatting(regions, rule);

            // 绘制图表
            // 创建一个画布
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 0 ,timeSet.size()+1, 1);
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText(sheetName);
            chart.setTitleOverlay(false);
            // 图例位置
            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.TOP);

            // 分类轴标(X轴),标题位置
            XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            // 值(Y轴)轴,标题位置
            XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
            leftAxis.setTitle(MessageUtils.message("defect.count"));
            // 设置网格颜色
            XDDFLineProperties markerLine = new XDDFLineProperties();
            XDDFColor color = XDDFColor.from(new byte[]{(byte)0xf0,(byte)0xf0,(byte)0xf0});
            XDDFSolidFillProperties fill = new XDDFSolidFillProperties(color);
            markerLine.setFillProperties(fill);
            markerLine.setWidth(1.0);
            leftAxis.getOrAddMajorGridProperties().setLineProperties(markerLine);

            // LINE：折线图，
            XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
            // 分类轴标-日期数据
            XDDFDataSource<String> dates = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(1, 1, 1, timeSet.size()));
            int seriesRowNum = 1;
            for(Map map : list) {
                seriesRowNum++;
                String title = MessageUtils.message(String.valueOf(map.get("name")));
                // 某个状态在不同时间的数据
                XDDFNumericalDataSource<Double> stateData = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(seriesRowNum, seriesRowNum, 1, timeSet.size()));
                // 图·表加载数据
                XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(dates, stateData);
                // 折线图例标题
                series.setTitle(title, null);
                // 直线
                series.setSmooth(true);
                // 设置标记样式，空心圆
                series.setMarkerStyle(MarkerStyle.CIRCLE);
            }
            // 设置图表中的颜色
            applyLineChartsColors(chart, data);
            // 绘制
            chart.plot(data);
            wb.write(response.getOutputStream());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            IOUtils.closeQuietly(wb);
        }
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/actions")
    public AjaxResult actionList(@PathVariable("projectId") Long projectId, @RequestParam("type") String type) {
        List<SysAction> actionList = sysDashboardService.actonList(projectId, type);
        Map<String, String> stateMap = Arrays.stream(SysDefectStateEnum.values()).collect(Collectors.toMap(v->String.valueOf(v.ordinal()), v->v.name()));
        Map<Long, List<SysAction>> actionMap = actionList.stream()
                .map(a->{
                    // 设置缺陷的状态名称
                    if("defect".equals(a.getType()) && StringUtils.isNotBlank(a.getState()) && stateMap.containsKey(a.getState())) {
                        a.setState(stateMap.get(a.getState()));
                    }
                    return a;
                })
                // 分组
                .collect(Collectors.groupingBy(a->((Double)(Math.floor(a.getTime().getTime()/(1000*60*60*24)))).longValue()*(1000*60*60*24)))
                // 排序
                .entrySet().stream().sorted(Map.Entry.<Long, List<SysAction>>comparingByKey().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> newValue,
                        TreeMap::new
                ));
        Map<String, Object> ret = new HashMap<>();
        ret.put("actions", actionMap);              // 按状态类型分组的日完成数量
        return AjaxResult.success(ret);
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/plan/{planId}")
    public AjaxResult planBurndown(@PathVariable("projectId") Long projectId, @PathVariable("planId") String planId) {
        SysPlan sysPlan = sysPlanService.selectSysPlanByPlanId(planId);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        List<SysColumnsInChart> sysColumnsInChartList =  sysDashboardService.planBurndown(planId);
        // 获取最大最小日期的操作对象
        LongSummaryStatistics summaryStatistics = sysColumnsInChartList.stream().map(x-> {
            try {
                return format.parse(x.getKey()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));

        Map<String, Long> sysColumnsInChartMap = sysColumnsInChartList.stream().collect(Collectors.toMap(SysColumnsInChart::getKey, SysColumnsInChart::getValue));
        List<SysColumnsInChart> list = new LinkedList<>();
        final long DAY_SIZE = 1000*60*60*24;    // 一天的毫秒时长
        final long DEFAULT_DAY = 30;            // 默认最大天数
        long dayCount = (summaryStatistics.getMax() - summaryStatistics.getMin())/DAY_SIZE;
        if(sysColumnsInChartList.size()==0) {
            for(long i=DEFAULT_DAY;i>=0;i--) {
                String day = format.format(new Date(System.currentTimeMillis()-i*DAY_SIZE));
                list.add(new SysColumnsInChart(day, sysPlan!=null?sysPlan.getItemTotal():0));
            }
        } else if(dayCount<DEFAULT_DAY) {
            long prevCount = sysPlan.getItemTotal();
            for(long i=0;i<=DEFAULT_DAY;i++) {
                String day = format.format(new Date(summaryStatistics.getMin()+i*DAY_SIZE));
                long count = sysColumnsInChartMap.containsKey(day)?prevCount-sysColumnsInChartMap.get(day):prevCount;
                list.add(new SysColumnsInChart(day,count));
                prevCount = count;
            }
        } else {
            // 如果缺陷数据大于30天，就显示30的的
            long prevCount = sysPlan.getItemTotal();
            for(long i=DEFAULT_DAY;i>=0;i--) {
                String day = format.format(new Date(summaryStatistics.getMax()-i*DAY_SIZE));
                long count =  sysColumnsInChartMap.containsKey(day)?prevCount-sysColumnsInChartMap.get(day):prevCount;
                list.add(new SysColumnsInChart(day, count));
                prevCount = count;
            }
        }
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/member-defect")
    public AjaxResult memberRankOfDefects(@PathVariable("projectId") Long projectId) {
        List<SysMemberRankOfDefects> list = sysDashboardService.memberRankOfDefects(projectId);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/member-defect-line")
    public AjaxResult memberOfDefectsLine(@PathVariable("projectId") Long projectId,  @RequestParam("timeType") String timeType) {
        List<SysMemberOfDefectsLine> list = sysDashboardService.memberOfDefectsLine(projectId, timeType);
        Set<String> timeSet = new LinkedHashSet<>();
        switch (timeType) {
            case MONTH_TYPE:
                setMemberDefectCountOfMonth(timeSet, list);
                break;
            default:
                setMemberDefectCountOfDay(timeSet, list);
                break;
        }

        Map<String, Object> ret = new HashMap<>();
        ret.put("time", timeSet);              // 日期数组
        ret.put("data", list);
        return AjaxResult.success(ret);
    }

    /**
     * 缺陷状态30天走势图
     */
    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @PostMapping("/{projectId}/member-defect-line/export")
    public void memberDefectLineExport(HttpServletResponse response, @PathVariable("projectId") Long projectId, @RequestParam("timeType") String timeType) {
        // 统计数据
        String sheetName = null;
        List<SysMemberOfDefectsLine> list = sysDashboardService.memberOfDefectsLine(projectId, timeType);
        Set<String> timeSet = new LinkedHashSet<>();
        SimpleDateFormat format;
        switch (timeType) {
            case MONTH_TYPE:
                sheetName = MessageUtils.message("dashboard.member-defect-line.month");
                setMemberDefectCountOfMonth(timeSet, list);
                break;
            default:
                sheetName = MessageUtils.message("dashboard.member-defect-line.day");
                setMemberDefectCountOfDay(timeSet, list);
                break;
        }

        // 数据转excel
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet();
        wb.setSheetName(0, sheetName);
        sheet.setDefaultColumnWidth(12);
        try
        {
            int rowNum = -1;
            // 创建图表行
            Row rowChart = sheet.createRow(++rowNum);
            rowChart.setHeight((short)5000);
            // 创建标题行
            Row rowTitle = sheet.createRow(++rowNum);
            // 创建一个单元格样式
            XSSFCellStyle titleStyle = wb.createCellStyle();
            // 设置单元格背景色
            titleStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x60,(byte)0x62,(byte)0x66}, null));
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            // 设置标题字体
            Font titleFont = wb.createFont();
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.RIGHT);
            titleStyle.setBorderBottom(BorderStyle.THIN);
            titleStyle.setBorderLeft(BorderStyle.THIN);
            titleStyle.setBorderRight(BorderStyle.THIN);
            titleStyle.setBorderTop(BorderStyle.THIN);
            // 创建标题行的列
            Cell cell1 = rowTitle.createCell(0);
            cell1.setCellValue(MessageUtils.message("member"));
            cell1.setCellStyle(titleStyle);
            int colNum = 1;
            for (String s : timeSet) {
                Cell cell = rowTitle.createCell(colNum++);
                cell.setCellValue(s);
                cell.setCellStyle(titleStyle);
            }
            Cell cellTotal = rowTitle.createCell(colNum++);
            cellTotal.setCellValue(MessageUtils.message("total"));
            cellTotal.setCellStyle(titleStyle);

            // 创建数据行
            // 数据样式
            XSSFCellStyle dataStyle = wb.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.RIGHT);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            Map<String, List<SysMemberOfDefectsLine>> map = list.stream().collect(Collectors.groupingBy(
                    SysMemberOfDefectsLine::getNickName,
                    LinkedHashMap::new,
                    Collectors.toList()));
            for(Map.Entry<String, List<SysMemberOfDefectsLine>> item : map.entrySet()) {
                colNum = 0;
                Row row = sheet.createRow(++rowNum);
                // 创建人名列
                Cell cellName = row.createCell(colNum++);
                cellName.setCellValue(item.getKey());
                cellName.setCellStyle(dataStyle);
                Map<String, SysMemberOfDefectsLine> mdlMap = item.getValue().stream().collect(Collectors.toMap(SysMemberOfDefectsLine::getCreateTime,o->o));
                // 创建每日或每月的处理缺陷数量列
                // 每人处理缺陷总数
                for (String s : timeSet) {
                    Cell cell = row.createCell(colNum++);
                    // 当日或当月处理缺陷数
                    int count = mdlMap.containsKey(s)?mdlMap.get(s).getDefectTodayCount():0;
                    cell.setCellValue(count);
                    cell.setCellStyle(dataStyle);
                    // 累加总数
                }
                // 创建总数列
                Cell cell = row.createCell(colNum);
                cell.setCellStyle(dataStyle);
                // 设置求和公式
                String startColString = CellReference.convertNumToColString(1);  // 起始列长度转成ABC列
                String endColString = CellReference.convertNumToColString(colNum-1);  // 结束列长度转成ABC列
                String sumstring = "SUM(" + startColString + (rowNum+1) + ":" + endColString + (rowNum+1) + ")";//求和公式
                cell.setCellFormula(sumstring);
            }
            // 设置条件
            // 创建等于0的规则（灰色背景）
            XSSFSheetConditionalFormatting cf = sheet.getSheetConditionalFormatting();
            XSSFConditionalFormattingRule rule = cf.createConditionalFormattingRule(
                    ComparisonOperator.LE,
                    "0"); // 小于等于0
            PatternFormatting fillLE0 = rule.createPatternFormatting();
            XSSFColor redColor = new XSSFColor(new byte[]{(byte)0xF5, (byte)0x6C, (byte)0x6C}, null);
            XSSFColor whiteColor = new XSSFColor(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF}, null);
            fillLE0.setFillBackgroundColor(redColor);
            fillLE0.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());
            // 设置字体颜色为白色
            XSSFFontFormatting fontLE0 = rule.createFontFormatting();
            fontLE0.setFontColor(whiteColor);
            // 应用条件格式的范围
            CellRangeAddress[] regions = { new CellRangeAddress(2, rowNum, 1, timeSet.size()+1)};
            // 添加条件格式规则到工作表
            cf.addConditionalFormatting(regions, rule);

            // 绘制图表
            // 创建一个画布
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 0 ,timeSet.size()+2, 1);
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText(sheetName);
            chart.setTitleOverlay(false);
            // 图例位置
            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.TOP);

            // 分类轴标(X轴),标题位置
            XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            // 值(Y轴)轴,标题位置
            XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
            leftAxis.setTitle(MessageUtils.message("defect.count"));
            // 设置网格颜色
            XDDFLineProperties markerLine = new XDDFLineProperties();
            XDDFColor color = XDDFColor.from(new byte[]{(byte)0xf0,(byte)0xf0,(byte)0xf0});
            XDDFSolidFillProperties fill = new XDDFSolidFillProperties(color);
            markerLine.setFillProperties(fill);
            markerLine.setWidth(1.0);
            leftAxis.getOrAddMajorGridProperties().setLineProperties(markerLine);

            // LINE：折线图，
            XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
            // 分类轴标-日期数据
            XDDFDataSource<String> dates = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(1, 1, 1, timeSet.size()));
            int seriesRowNum = 1;
            for(Map.Entry<String, List<SysMemberOfDefectsLine>> item : map.entrySet()) {
                seriesRowNum++;
                String title = item.getKey();
                // 某个状态在不同时间的数据
                XDDFNumericalDataSource<Double> stateData = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(seriesRowNum, seriesRowNum, 1, timeSet.size()));
                // 图·表加载数据
                XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(dates, stateData);
                // 折线图例标题
                series.setTitle(title, null);
                // 直线
                series.setSmooth(true);
                // 设置标记大小
//                series.setMarkerSize((short) 5);
                // 设置标记样式，空心圆
                series.setMarkerStyle(MarkerStyle.CIRCLE);
            }
            // 设置图表中的颜色
            applyLineChartsColors(chart, data);
            // 绘制
            chart.plot(data);
            wb.write(response.getOutputStream());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            IOUtils.closeQuietly(wb);
        }
    }

    /**
     * 缺陷状态30天走势图
     */
    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @Log(title = "缺陷", businessType = BusinessType.EXPORT)
    @PostMapping("/{projectId}/plan-statistics/export")
    public void planStatisticsExport(HttpServletResponse response, @PathVariable("projectId") Long projectId) {
        // 统计数据
        SysPlan plan = new SysPlan();
        plan.setProjectId(projectId);
        List<SysPlan> planList = sysPlanService.selectSysPlanList(plan);
        String sheetName = MessageUtils.message("dashboard.plan-statistics");

        // 数据转excel
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet();
        wb.setSheetName(0, sheetName);
        sheet.setDefaultColumnWidth(12);
        try
        {
            int rowNum = -1;
            // 创建图表行
            Row rowChart = sheet.createRow(++rowNum);
            rowChart.setHeight((short)5000);

            Row rowChart2 = sheet.createRow(++rowNum);
            rowChart2.setHeight((short)3000);
            // 创建标题行
            Row rowTitle1 = sheet.createRow(++rowNum);
            // 创建一个单元格样式
            XSSFCellStyle titleStyle = wb.createCellStyle();
            // 设置单元格背景色
            titleStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x60,(byte)0x62,(byte)0x66}, null));
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            // 设置标题字体
            Font titleFont = wb.createFont();
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setBorderBottom(BorderStyle.THIN);
            titleStyle.setBorderLeft(BorderStyle.THIN);
            titleStyle.setBorderRight(BorderStyle.THIN);
            titleStyle.setBorderTop(BorderStyle.THIN);
            // 创建标题行的列
            // 标题列索引
            int titleCellNum = 0;
            // 测试数据的起始列索引
            int caseTitleStartCellNum = 0;
            // 测试计划名称
            Cell cellPlanNameTitle = rowTitle1.createCell(titleCellNum);
            cellPlanNameTitle.setCellValue(MessageUtils.message("plan.name"));
            cellPlanNameTitle.setCellStyle(titleStyle);
            CellRangeAddress regionPlanNameTitle = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionPlanNameTitle);
            // 测试版本
            Cell cellVersionTitle = rowTitle1.createCell(++titleCellNum);
            cellVersionTitle.setCellValue(MessageUtils.message("version"));
            cellVersionTitle.setCellStyle(titleStyle);
            CellRangeAddress regionVersionTitle = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionVersionTitle);
            // 测试计划开始时间
            Cell cellPlanStartTimeTitle = rowTitle1.createCell(++titleCellNum);
            cellPlanStartTimeTitle.setCellValue(MessageUtils.message("plan.start-time"));
            cellPlanStartTimeTitle.setCellStyle(titleStyle);
            CellRangeAddress regionPlanStartTimeTitle = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionPlanStartTimeTitle);
            // 测试计划结束时间
            Cell cellPlanEndTimeTitle = rowTitle1.createCell(++titleCellNum);
            cellPlanEndTimeTitle.setCellValue(MessageUtils.message("plan.end-time"));
            cellPlanEndTimeTitle.setCellStyle(titleStyle);
            CellRangeAddress regionPlanEndTimeTitle = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionPlanEndTimeTitle);
            // 测试用例列
            Cell cellCaseTitle = rowTitle1.createCell(++titleCellNum);
            cellCaseTitle.setCellValue(MessageUtils.message("case.test_case"));
            CellRangeAddress regionCaseTitle = new CellRangeAddress(rowNum, rowNum, titleCellNum, titleCellNum+3);
            cellCaseTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(regionCaseTitle);
            caseTitleStartCellNum = titleCellNum;
            // 缺陷总数
            titleCellNum+=4;
            Cell cellDefectTotalTitle = rowTitle1.createCell(titleCellNum);
            cellDefectTotalTitle.setCellValue(MessageUtils.message("defect.total"));
            CellRangeAddress regionDefectTotal = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionDefectTotal);
            cellDefectTotalTitle.setCellStyle(titleStyle);
            // 缺陷发现率
            Cell cellDefectDiscoveryRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectDiscoveryRateTitle.setCellValue(MessageUtils.message("defect.discovery-rate"));
            cellDefectDiscoveryRateTitle.setCellStyle(titleStyle);
            CellRangeAddress regionDefectDiscoveryRate = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionDefectDiscoveryRate);
            // 缺陷修复率
            Cell cellDefectRepairRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectRepairRateTitle.setCellValue(MessageUtils.message("defect.repair-rate"));
            cellDefectRepairRateTitle.setCellStyle(titleStyle);
            CellRangeAddress regionDefectRepairRate = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionDefectRepairRate);
            // 缺陷探测率
            Cell cellDefectDetectionRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectDetectionRateTitle.setCellValue(MessageUtils.message("defect.detection-rate"));
            cellDefectDetectionRateTitle.setCellStyle(titleStyle);
            CellRangeAddress regionDefectDetectionRate = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionDefectDetectionRate);
            // 缺陷严重率
            Cell cellDefectSeverityRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectSeverityRateTitle.setCellValue(MessageUtils.message("defect.severity-rate"));
            cellDefectSeverityRateTitle.setCellStyle(titleStyle);
            CellRangeAddress regionDefectSeverityRate = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionDefectSeverityRate);
            // 缺陷重开率
            Cell cellDefectRestartRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectRestartRateTitle.setCellValue(MessageUtils.message("defect.restart-rate"));
            cellDefectRestartRateTitle.setCellStyle(titleStyle);
            CellRangeAddress regionDefectRestartRate = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionDefectRestartRate);
            // 缺陷逃逸率
            Cell cellDefectEscapeRateTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectEscapeRateTitle.setCellValue(MessageUtils.message("defect.escape-rate"));
            cellDefectEscapeRateTitle.setCellStyle(titleStyle);
            CellRangeAddress regionDefectEscapeRate = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionDefectEscapeRate);
            // 缺陷密度
            Cell cellDefectDensityTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectDensityTitle.setCellValue(MessageUtils.message("defect.density"));
            cellDefectDensityTitle.setCellStyle(titleStyle);
            CellRangeAddress regionDefectDensity = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionDefectDensity);
            // 缺陷修复平均时长
            Cell cellDefectRepairAvgHourTitle = rowTitle1.createCell(++titleCellNum);
            cellDefectRepairAvgHourTitle.setCellValue(MessageUtils.message("defect.repair-avg-hour"));
            cellDefectRepairAvgHourTitle.setCellStyle(titleStyle);
            CellRangeAddress regionDefectRepairAvgHour = new CellRangeAddress(rowNum, rowNum+1, titleCellNum, titleCellNum);
            sheet.addMergedRegion(regionDefectRepairAvgHour);

            // 创建测试用例标题列
            Row rowTitle2 = sheet.createRow(++rowNum);
            // 通过
            Cell cellPassTitle = rowTitle2.createCell(caseTitleStartCellNum);
            cellPassTitle.setCellValue(MessageUtils.message("pass"));
            cellPassTitle.setCellStyle(titleStyle);
            // 未通过
            Cell cellNotPassTitle = rowTitle2.createCell(caseTitleStartCellNum+1);
            cellNotPassTitle.setCellValue(MessageUtils.message("not-pass"));
            cellNotPassTitle.setCellStyle(titleStyle);
            // 执行列
            Cell cellRunTitle = rowTitle2.createCell(caseTitleStartCellNum+2);
            cellRunTitle.setCellValue(MessageUtils.message("not-run"));
            cellRunTitle.setCellStyle(titleStyle);
            // 未执行列
            Cell cellNotRunTitle = rowTitle2.createCell(caseTitleStartCellNum+3);
            cellNotRunTitle.setCellValue(MessageUtils.message("total"));
            cellNotRunTitle.setCellStyle(titleStyle);
            // 设置第二行标题默认样式
            for(int i=0;i<caseTitleStartCellNum;i++) {
                Cell emptyCell = rowTitle2.createCell(i);
                emptyCell.setCellStyle(titleStyle);
            }
            for(int i=caseTitleStartCellNum+4;i<=titleCellNum;i++) {
                Cell emptyCell = rowTitle2.createCell(i);
                emptyCell.setCellStyle(titleStyle);
            }
            // 创建数据行
            // 数据样式
            XSSFCellStyle dataStyle = wb.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.RIGHT);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            // 百分比数据样式
            XSSFCellStyle percentageStyle = wb.createCellStyle();
            percentageStyle.setDataFormat(wb.createDataFormat().getFormat("0.00%"));
            percentageStyle.setAlignment(HorizontalAlignment.RIGHT);
            percentageStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            percentageStyle.setBorderBottom(BorderStyle.THIN);
            percentageStyle.setBorderLeft(BorderStyle.THIN);
            percentageStyle.setBorderRight(BorderStyle.THIN);
            percentageStyle.setBorderTop(BorderStyle.THIN);
            // 日期格式的样式
            XSSFCellStyle timeStyle = wb.createCellStyle();
            timeStyle.setAlignment(HorizontalAlignment.RIGHT);
            timeStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            timeStyle.setBorderBottom(BorderStyle.THIN);
            timeStyle.setBorderLeft(BorderStyle.THIN);
            timeStyle.setBorderRight(BorderStyle.THIN);
            timeStyle.setBorderTop(BorderStyle.THIN);
            String dateFormatString = "yyyy-MM-dd HH:mm:ss";
            DataFormat dataFormat = wb.createDataFormat();
            short dateFormat = dataFormat.getFormat(dateFormatString);
            timeStyle.setDataFormat(dateFormat);
            for(SysPlan p : planList) {
                int colNum = 0;
                Row row = sheet.createRow(++rowNum);
                // 测试计划名称
                Cell cellPlanName = row.createCell(colNum++);
                cellPlanName.setCellValue(p.getPlanName());
                cellPlanName.setCellStyle(dataStyle);
                // 测试计划版本
                Cell cellPlanVersion = row.createCell(colNum++);
                cellPlanVersion.setCellValue(p.getPlanVersion());
                cellPlanVersion.setCellStyle(dataStyle);
                // 测试计划开始时间
                Cell cellPlanStartTime = row.createCell(colNum++);
                cellPlanStartTime.setCellValue(p.getPlanStartTime());
                cellPlanStartTime.setCellStyle(timeStyle);
                // 测试计划结束时间
                Cell cellPlanEndTime = row.createCell(colNum++);
                cellPlanEndTime.setCellValue(p.getPlanEndTime());
                cellPlanEndTime.setCellStyle(timeStyle);
                // 通过
                Cell cellPass = row.createCell(colNum++);
                cellPass.setCellValue(p.getPassCount());
                cellPass.setCellStyle(dataStyle);
                // 未通过
                Cell cellNotPass = row.createCell(colNum++);
                cellNotPass.setCellValue(p.getFailCount());
                cellNotPass.setCellStyle(dataStyle);
                // 未执行
                Cell cellRun = row.createCell(colNum++);
                cellRun.setCellValue(p.getUnexecutedCount());
                cellRun.setCellStyle(dataStyle);
                // 缺陷总数
                Cell cellNotRun = row.createCell(colNum++);
                cellNotRun.setCellValue(p.getItemTotal());
                cellNotRun.setCellStyle(dataStyle);
                // 缺陷总数
                Cell cellDefectCount = row.createCell(colNum++);
                cellDefectCount.setCellValue(p.getDefectCount());
                cellDefectCount.setCellStyle(dataStyle);
                // 缺陷发现率
                Cell cellDiscoveryRate = row.createCell(colNum++);
                cellDiscoveryRate.setCellValue(strPercentage2Double(p.getDefectDiscoveryRate()));
                cellDiscoveryRate.setCellStyle(percentageStyle);
                // 缺陷修复率
                Cell cellRepairRate = row.createCell(colNum++);
                cellRepairRate.setCellValue(strPercentage2Double(p.getDefectRepairRate()));
                cellRepairRate.setCellStyle(percentageStyle);
                // 缺陷探测率
                Cell cellDefectDetectionRate = row.createCell(colNum++);
                cellDefectDetectionRate.setCellValue(strPercentage2Double(p.getDefectDetectionRate()));
                cellDefectDetectionRate.setCellStyle(percentageStyle);
                // 缺陷严重率
                Cell cellDefectSeverityRate = row.createCell(colNum++);
                cellDefectSeverityRate.setCellValue(strPercentage2Double(p.getDefectSeverityRate()));
                cellDefectSeverityRate.setCellStyle(percentageStyle);
                // 缺陷重开率
                Cell cellDefectRestartRate = row.createCell(colNum++);
                cellDefectRestartRate.setCellValue(strPercentage2Double(p.getDefectRestartRate()));
                cellDefectRestartRate.setCellStyle(percentageStyle);
                // 缺陷逃逸率
                Cell cellDefectEscapeRate = row.createCell(colNum++);
                cellDefectEscapeRate.setCellValue(strPercentage2Double(p.getDefectEscapeRate()));
                cellDefectEscapeRate.setCellStyle(percentageStyle);
                // 缺陷密度
                Cell cellDefectDensity = row.createCell(colNum++);
                cellDefectDensity.setCellValue(Double.valueOf(p.getDefectDensity()));
                cellDefectDensity.setCellStyle(dataStyle);
                // 缺陷修复平均时长
                Cell cellDefectRepairAvgHour = row.createCell(colNum++);
                cellDefectRepairAvgHour.setCellValue(Double.valueOf(p.getDefectRepairAvgHour()));
                cellDefectRepairAvgHour.setCellStyle(dataStyle);
            }
            // 设置所有列为自适应宽度
            for(int i = 0;i <= titleCellNum;i++) {
                sheet.autoSizeColumn(i, true);
                // 获取当前列宽并增加20%
                int currentWidth = sheet.getColumnWidth(i);
                // 设置列宽加30%
                sheet.setColumnWidth(i, (int)(currentWidth * 1.3));
            }

            // 绘制图表
            // 设置图表数据区域
            int dataRowStartIndex = 4;
            int dataRowEndIndex = dataRowStartIndex+planList.size()-1;
            // 获取x坐标分类字段名集合
            List<String> categories = new LinkedList<>();
            for(int i=caseTitleStartCellNum;i<caseTitleStartCellNum+4;i++) {
                categories.add(sheet.getRow(dataRowStartIndex-1).getCell(i).getStringCellValue());
            }
            for(int i=caseTitleStartCellNum+4;i<=titleCellNum;i++) {
                categories.add(sheet.getRow(dataRowStartIndex-2).getCell(i).getStringCellValue());
            }
            XDDFDataSource<String> category = XDDFDataSourcesFactory.fromArray(
                    categories.toArray(new String[0])
            );
            // 统计数量数据
            Map<String,XDDFNumericalDataSource<Double>> numValueCellRangeAddressList = new LinkedHashMap<>();
            for(int i=dataRowStartIndex;i<=dataRowEndIndex;i++) {
                String planName = sheet.getRow(i).getCell(0).getStringCellValue();
                String version = sheet.getRow(i).getCell(1).getStringCellValue();
                if(StringUtils.isNotBlank(version)){
                    planName+=String.format("(%s)",version);
                }
                XDDFNumericalDataSource<Double> value = XDDFDataSourcesFactory.fromNumericCellRange(
                        sheet, new CellRangeAddress(i, i, caseTitleStartCellNum, caseTitleStartCellNum+4));
                numValueCellRangeAddressList.put(planName, value);
            }
            // 绘制画布
            drawPlanStatisticsChart(sheet, MessageUtils.message("dashboard.plan-statistics.quantity-comparison"),
                    0,1,0,titleCellNum + 1,
                    MessageUtils.message("dashboard.plan-statistics.quantity"),
                    category, numValueCellRangeAddressList, new int[]{0, 1, 2,3,4}, true);
            // 统计百分比数据
            Map<String,XDDFNumericalDataSource<Double>> percentageValueCellRangeAddressList = new LinkedHashMap<>();
            for(int i=dataRowStartIndex;i<=dataRowEndIndex;i++) {
                String planName = sheet.getRow(i).getCell(0).getStringCellValue();
                XDDFNumericalDataSource<Double> value = XDDFDataSourcesFactory.fromNumericCellRange(
                        sheet, new CellRangeAddress(i, i, caseTitleStartCellNum+5, caseTitleStartCellNum+10));
                percentageValueCellRangeAddressList.put(planName, value);
            }
            drawPlanStatisticsChart(sheet, MessageUtils.message("dashboard.plan-statistics.percentage-comparison"),
                    1,2,0,9,
                    MessageUtils.message("dashboard.plan-statistics.percentage"),
                    category, percentageValueCellRangeAddressList, new int[]{5,6,7,8,9,10}, false);
            // 统计密度数据
            Map<String,XDDFNumericalDataSource<Double>> densityValueCellRangeAddressList = new LinkedHashMap<>();
            for(int i=dataRowStartIndex;i<=dataRowEndIndex;i++) {
                String planName = sheet.getRow(i).getCell(0).getStringCellValue();
                XDDFNumericalDataSource<Double> value = XDDFDataSourcesFactory.fromNumericCellRange(
                        sheet, new CellRangeAddress(i, i, caseTitleStartCellNum+11, caseTitleStartCellNum+11));
                densityValueCellRangeAddressList.put(planName, value);
            }
            drawPlanStatisticsChart(sheet, MessageUtils.message("dashboard.plan-statistics.density-comparison"),
                    1,2,9,13,
                    MessageUtils.message("dashboard.plan-statistics.density"),
                    category, densityValueCellRangeAddressList, new int[]{11}, false);

            // 统计时间数据
            Map<String,XDDFNumericalDataSource<Double>> timeValueCellRangeAddressList = new LinkedHashMap<>();
            for(int i=dataRowStartIndex;i<=dataRowEndIndex;i++) {
                String planName = sheet.getRow(i).getCell(0).getStringCellValue();
                XDDFNumericalDataSource<Double> value = XDDFDataSourcesFactory.fromNumericCellRange(
                        sheet, new CellRangeAddress(i, i, caseTitleStartCellNum+12, caseTitleStartCellNum+12));
                timeValueCellRangeAddressList.put(planName, value);
            }
            drawPlanStatisticsChart(sheet, MessageUtils.message("dashboard.plan-statistics.time-comparison"),
                    1,2,13,17,
                    MessageUtils.message("dashboard.plan-statistics.time"),
                    category, timeValueCellRangeAddressList, new int[]{12}, false);

            wb.write(response.getOutputStream());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            IOUtils.closeQuietly(wb);
        }
    }

    /**
     * 绘制测试计划矩形图表
     * @param sheet 表格对象
     * @param title 标题
     * @param row1  展示起始行
     * @param row2  展示结束行
     * @param col1  展示起始列
     * @param col2  展示结束列
     * @param yName Y轴名称
     * @param category  x坐标分类对象
     * @param valueCellRangeAddressList 数据值
     * @param bindDataCols 绑定的数据列
     * @param showLegend 是否显示图例
     */
    private void drawPlanStatisticsChart(XSSFSheet sheet, String title, int row1, int row2, int col1, int col2,
                                         String yName,
                                         XDDFDataSource<String> category, Map<String,XDDFNumericalDataSource<Double>> valueCellRangeAddressList,
                                         int[] bindDataCols, boolean showLegend
                           ) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col1, row1 ,col2, row2);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        // 分类轴标(X轴),标题位置
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        // 值(Y轴)轴,标题位置
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        // 设置Y轴标题
        leftAxis.setTitle(yName);
        leftAxis.setCrossBetween(AxisCrossBetween.BETWEEN);
        // 创建图表数据并设置系列
        XDDFBarChartData dataChart = (XDDFBarChartData)chart.createData(
                ChartTypes.BAR,
                bottomAxis,
                leftAxis);
        // 设置类型
        dataChart.setBarGrouping(BarGrouping.CLUSTERED);
        // 设置方向
        dataChart.setBarDirection(BarDirection.COL);

        for(Map.Entry<String, XDDFNumericalDataSource<Double>> ds : valueCellRangeAddressList.entrySet()) {
            addFilteredSeries(dataChart, category, ds.getValue(), ds.getKey(), bindDataCols);
        }
        // 绘制
        chart.plot(dataChart);
        // 设置颜色
        applyLineChartsColors(chart,dataChart);

        for(CTBarSer ctBarSer : chart.getCTChart().getPlotArea().getBarChartArray(0).getSerArray()) {
            CTDLbls ctdLbls = ctBarSer.addNewDLbls();
            ctdLbls.addNewShowVal().setVal(true); // 显示数值
            ctdLbls.addNewShowLegendKey().setVal(false);
            ctdLbls.addNewShowCatName().setVal(false);
            ctdLbls.addNewShowSerName().setVal(false);
            // 设置标签位置（外部末端）
            ctdLbls.addNewDLblPos().setVal(STDLblPos.OUT_END);

        }

        // 设置图例位置
        if(showLegend) {
            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.RIGHT);
        }
    }

    /**
     * 字符百分比转Double
     * @param value
     * @return
     */
    private static double strPercentage2Double(String value) {
        return Double.parseDouble(value.replace("%", ""))*0.01;
    }
    private static void addFilteredSeries(XDDFBarChartData chartData,
                                          XDDFDataSource<String> categories,
                                          XDDFNumericalDataSource<Double> values,
                                          String title,
                                          int[] includedIndices) {
        // 创建筛选后的数据源
        String[] filteredCategories = new String[includedIndices.length];
//        Double[] filteredValues = new Double[includedIndices.length];

        for (int i = 0; i < includedIndices.length; i++) {
            filteredCategories[i] = categories.getPointAt(includedIndices[i]);
//            filteredValues[i] = values.getPointAt(includedIndices[i])==null?0:values.getPointAt(includedIndices[i]);
        }

        // 使用筛选后的数据创建系列
        XDDFDataSource<String> filteredCatSource = XDDFDataSourcesFactory.fromArray(filteredCategories);
//        XDDFNumericalDataSource<Double> filteredValSource = XDDFDataSourcesFactory.fromArray(filteredValues);

        XDDFChartData.Series series =  chartData.addSeries(
                filteredCatSource, values);
        series.setTitle(title, null);
    }

    private void setMemberDefectCountOfDay(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        LongSummaryStatistics summaryStatistics = list.stream().map(x-> {
            try {
                return format.parse(x.getCreateTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));

        // 如果缺陷数据小于30天，就显示30天的，没有的天数补齐
        final long DAY_SIZE = 1000*60*60*24;    // 一天的毫秒时长
        final long DEFAULT_DAY = 30;            // 默认最大天数
        long dayCount = (summaryStatistics.getMax() - summaryStatistics.getMin())/DAY_SIZE;
        if(list.size()==0) {
            for(long i=DEFAULT_DAY;i>=0;i--) {
                timeSet.add(format.format(new Date(System.currentTimeMillis()-i*DAY_SIZE)));
            }
        } else if(dayCount<DEFAULT_DAY) {
            for(long i=0;i<=DEFAULT_DAY;i++) {
                timeSet.add(format.format(new Date(summaryStatistics.getMin()+i*DAY_SIZE)));
            }
        } else {
            // 如果缺陷数据大于30天，就显示30的的
            for(long i=DEFAULT_DAY;i>=0;i--) {
                timeSet.add(format.format(new Date(summaryStatistics.getMax()-i*DAY_SIZE)));
            }
        }
    }

    private void setMemberDefectCountOfMonth(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        LongSummaryStatistics summaryStatistics = list.stream().map(x-> {
            try {
                return format.parse(x.getCreateTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));

        // 如果缺陷数据小于30天，就显示30天的，没有的天数补齐
        final int DEFAULT_MONTH = 12;            // 默认最大天数
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(new Date(summaryStatistics.getMin()));
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(new Date(summaryStatistics.getMax()));
        int monthCount = endCalendar.get(Calendar.MONTH)-startCalendar.get(Calendar.MONTH);

        // 如果缺陷数据小于12个月，就显示12个月的，没有的天数补齐
        if(list.size()==0) {
            startCalendar.setTime(new Date());
            startCalendar.add(Calendar.MONTH, -DEFAULT_MONTH);
            for(long i=DEFAULT_MONTH;i>=0;i--) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else if(monthCount<DEFAULT_MONTH) {
            timeSet.add(format.format(startCalendar.getTime()));
            for(int i=1;i<DEFAULT_MONTH;i++) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else {
            // 如果缺陷数据大于12个月，就显示30的的
            endCalendar.add(Calendar.MONTH, -DEFAULT_MONTH);
            timeSet.add(format.format(endCalendar.getTime()));
            for(int i=1;i<=DEFAULT_MONTH;i++) {
                endCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(endCalendar.getTime()));
            }
        }
    }

    private void setCountOfDay(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        LongSummaryStatistics summaryStatistics = defectLineList.stream().map(x-> {
            try {
                return format.parse(x.getDefectTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));
        final long DAY_SIZE = 1000*60*60*24;    // 一天的毫秒时长
        final long DEFAULT_DAY = 30;            // 默认最大天数
        long dayCount = (summaryStatistics.getMax() - summaryStatistics.getMin())/DAY_SIZE;

        // 如果缺陷数据小于30天，就显示30天的，没有的天数补齐
        if(defectLineList.size()==0) {
            for(long i=DEFAULT_DAY;i>=0;i--) {
                timeSet.add(format.format(new Date(System.currentTimeMillis()-i*DAY_SIZE)));
            }
        } else if(dayCount<DEFAULT_DAY) {
            for(long i=0;i<=DEFAULT_DAY;i++) {
                timeSet.add(format.format(new Date(summaryStatistics.getMin()+i*DAY_SIZE)));
            }
        } else {
            // 如果缺陷数据大于30天，就显示30的的
            for(long i=DEFAULT_DAY;i>=0;i--) {
                timeSet.add(format.format(new Date(summaryStatistics.getMax()-i*DAY_SIZE)));
            }
        }
    }

    private void setCountOfMonth(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        LongSummaryStatistics summaryStatistics = defectLineList.stream().map(x-> {
            try {
                return format.parse(x.getDefectTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));
        final int DEFAULT_MONTH = 12;            // 默认最大天数
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(new Date(summaryStatistics.getMin()));
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(new Date(summaryStatistics.getMax()));
        int monthCount = endCalendar.get(Calendar.MONTH)-startCalendar.get(Calendar.MONTH);

        // 如果缺陷数据小于12个月，就显示12个月的，没有的天数补齐
        if(defectLineList.size()==0) {
            startCalendar.setTime(new Date());
            startCalendar.add(Calendar.MONTH, -DEFAULT_MONTH);
            for(long i=DEFAULT_MONTH;i>=0;i--) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else if(monthCount<DEFAULT_MONTH) {
            timeSet.add(format.format(startCalendar.getTime()));
            for(int i=1;i<DEFAULT_MONTH;i++) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else {
            // 如果缺陷数据大于12个月，就显示12个月的
            endCalendar.add(Calendar.MONTH, -DEFAULT_MONTH);
            timeSet.add(format.format(endCalendar.getTime()));
            for(int i=1;i<=DEFAULT_MONTH;i++) {
                endCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(endCalendar.getTime()));
            }
        }
    }
}
