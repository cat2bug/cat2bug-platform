package com.cat2bug.web.service.excel;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.system.domain.SysDefectLine;
import com.cat2bug.system.domain.SysMemberOfDefectsLine;
import com.cat2bug.system.service.ISysDashboardService;
import com.cat2bug.system.util.SysDefectTrendLineHelper;
import com.cat2bug.web.excel.ExcelHeaderCell;
import com.cat2bug.web.excel.ExcelHeaderStyle;
import com.cat2bug.web.excel.ExcelTableModel;
import com.cat2bug.web.excel.ExcelTableWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
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
import java.util.stream.Collectors;/**
 * Native：缺陷/成员走势（方案 A，仅数据表、无图表）。
 */
@Profile("native")
@Service
public class FastExcelDefectTrendExcelExportService implements DefectTrendExcelExportService {private static final Map<String, String> STATE_LABELS = Map.of(
            "PROCESSING", "处理中",
            "AUDIT", "待验证",
            "RESOLVED", "已解决",
            "REJECTED", "已驳回",
            "CLOSED", "已关闭",
            "REOPEN", "重新打开",
            "HANG", "挂起"
    );@Autowired
    private ISysDashboardService sysDashboardService;@Autowired
    private ExcelTableWriter excelTableWriter;@Override
    public byte[] exportDefectStateLine(Long projectId, String timeType) {
        List<SysDefectLine> defectLineList = sysDashboardService.defectLine(projectId, timeType);
        String sheetName = SysDefectTrendLineHelper.MONTH_TYPE.equals(timeType)
                ? MessageUtils.message("dashboard.defect-line.month")
                : MessageUtils.message("dashboard.defect-line.day");
        Set<String> timeSet = new LinkedHashSet<>();
        if (SysDefectTrendLineHelper.MONTH_TYPE.equals(timeType)) {
            buildMonthTimeSet(timeSet, defectLineList);
        } else {
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
        List<ExcelHeaderCell> headers = new ArrayList<>();
        headers.add(new ExcelHeaderCell("状态", ExcelHeaderStyle.NORMAL));
        timeSet.forEach(time -> headers.add(new ExcelHeaderCell(time, ExcelHeaderStyle.NORMAL)));
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> map : list) {
            List<String> row = new ArrayList<>();
            row.add(STATE_LABELS.getOrDefault(String.valueOf(map.get("name")), String.valueOf(map.get("name"))));
            for (String time : timeSet) {
                row.add(String.valueOf(map.get(time)));
            }
            rows.add(row);
        }
        return excelTableWriter.write(ExcelTableModel.withHeaders(sheetName, headers, rows, 12),
                "导出缺陷状态走势失败");
    }@Override
    public byte[] exportMemberDefectLine(Long projectId, String timeType) {
        List<SysMemberOfDefectsLine> rawList = sysDashboardService.memberOfDefectsLine(projectId, timeType);
        String sheetName = SysDefectTrendLineHelper.MONTH_TYPE.equals(timeType)
                ? MessageUtils.message("dashboard.member-defect-line.month")
                : MessageUtils.message("dashboard.member-defect-line.day");
        Set<String> timeSet = new LinkedHashSet<>();
        if (SysDefectTrendLineHelper.MONTH_TYPE.equals(timeType)) {
            buildMemberMonthTimeSet(timeSet, rawList);
        } else {
            buildMemberDayTimeSet(timeSet, rawList);
        }
        List<ExcelHeaderCell> headers = new ArrayList<>();
        headers.add(new ExcelHeaderCell("成员", ExcelHeaderStyle.NORMAL));
        timeSet.forEach(time -> headers.add(new ExcelHeaderCell(time, ExcelHeaderStyle.NORMAL)));
        headers.add(new ExcelHeaderCell("总数", ExcelHeaderStyle.NORMAL));
        Map<String, List<SysMemberOfDefectsLine>> grouped = rawList.stream().collect(Collectors.groupingBy(
                SysMemberOfDefectsLine::getNickName, LinkedHashMap::new, Collectors.toList()));
        List<List<String>> rows = new ArrayList<>();
        for (Map.Entry<String, List<SysMemberOfDefectsLine>> item : grouped.entrySet()) {
            List<String> row = new ArrayList<>();
            row.add(item.getKey());
            Map<String, SysMemberOfDefectsLine> byTime = item.getValue().stream()
                    .collect(Collectors.toMap(SysMemberOfDefectsLine::getCreateTime, o -> o, (a, b) -> a));
            int total = 0;
            for (String time : timeSet) {
                int count = byTime.containsKey(time) ? byTime.get(time).getDefectTodayCount() : 0;
                total += count;
                row.add(String.valueOf(count));
            }
            row.add(String.valueOf(total));
            rows.add(row);
        }
        return excelTableWriter.write(ExcelTableModel.withHeaders(sheetName, headers, rows, 12),
                "导出成员缺陷走势失败");
    }

    private static void buildDayTimeSet(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildDefectStateLineResult(defectLineList, "day");
        timeSet.addAll((Collection<? extends String>) built.get("times"));
    }

    private static void buildMonthTimeSet(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildDefectStateLineResult(defectLineList,
                SysDefectTrendLineHelper.MONTH_TYPE);
        timeSet.addAll((Collection<? extends String>) built.get("times"));
    }

    private static void buildMemberDayTimeSet(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildMemberDefectLineResult(list, "day");
        timeSet.addAll((Collection<? extends String>) built.get("time"));
    }

    private static void buildMemberMonthTimeSet(Set<String> timeSet, List<SysMemberOfDefectsLine> list) {
        Map<String, Object> built = SysDefectTrendLineHelper.buildMemberDefectLineResult(list,
                SysDefectTrendLineHelper.MONTH_TYPE);
        timeSet.addAll((Collection<? extends String>) built.get("time"));
    }
}
