package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.web.excel.ExcelHeaderCell;
import com.cat2bug.web.excel.ExcelHeaderStyle;
import com.cat2bug.web.excel.ExcelTableModel;
import com.cat2bug.web.excel.ExcelTableWriter;
import com.cat2bug.system.domain.SysPlan;
import com.cat2bug.system.mapper.SysPlanMapper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;/**
 * Native：测试计划统计（方案 A，仅数据表、无图表）。
 */
@Profile("native")
@Service
public class FastExcelPlanStatisticsExcelExportService implements PlanStatisticsExcelExportService {private static final List<ExcelHeaderCell> HEADERS = List.of(
            new ExcelHeaderCell("测试计划名称", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("版本", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("计划开始时间", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("计划结束时间", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("通过", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("未通过", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("未执行", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("总数", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("缺陷总数", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("缺陷发现率", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("缺陷修复率", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("缺陷探测率", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("缺陷严重率", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("缺陷重开率", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("缺陷逃逸率", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("缺陷密度", ExcelHeaderStyle.NORMAL),
            new ExcelHeaderCell("缺陷修复平均时长", ExcelHeaderStyle.NORMAL)
    );@Autowired
    SysPlanMapper sysPlanMapper;@Autowired
    ExcelTableWriter excelTableWriter;@Override
    public byte[] exportPlanStatistics(Long projectId) {
        SysPlan query = new SysPlan();
        query.setProjectId(projectId);
        List<SysPlan> planList = sysPlanMapper.selectSysPlanList(query);
        List<List<String>> rows = new ArrayList<>();
        for (SysPlan plan : planList) {
            rows.add(List.of(
                    text(plan.getPlanName()),
                    text(plan.getPlanVersion()),
                    formatDate(plan.getPlanStartTime()),
                    formatDate(plan.getPlanEndTime()),
                    text(plan.getPassCount()),
                    text(plan.getFailCount()),
                    text(plan.getUnexecutedCount()),
                    text(plan.getItemTotal()),
                    text(plan.getDefectCount()),
                    text(plan.getDefectDiscoveryRate()),
                    text(plan.getDefectRepairRate()),
                    text(plan.getDefectDetectionRate()),
                    text(plan.getDefectSeverityRate()),
                    text(plan.getDefectRestartRate()),
                    text(plan.getDefectEscapeRate()),
                    text(plan.getDefectDensity()),
                    text(plan.getDefectRepairAvgHour())
            ));
        }
        return excelTableWriter.write(
                ExcelTableModel.withHeaders("测试计划统计", HEADERS, rows, 15),
                "导出测试计划统计失败");
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
