package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.web.excel.ExcelHeaderCell;
import com.cat2bug.web.excel.ExcelHeaderStyle;
import com.cat2bug.web.excel.ExcelTableModel;
import com.cat2bug.web.excel.ExcelTableWriter;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.service.ISysCaseService;
import com.cat2bug.system.service.ISysModuleService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;/**
 * 用例 Excel 数据导出。
 */
@Service
public class CaseExcelExportService {@Autowired
    ISysCaseService sysCaseService;@Autowired
    ISysModuleService sysModuleService;@Autowired
    ExcelTableWriter excelTableWriter;public byte[] buildExportWorkbook(SysCase query, Map<String, Object> params, Locale locale) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.putIfAbsent("type", "export");
        List<SysCase> list = sysCaseService.selectSysCaseList(query);
        for (SysCase item : list) {
            item.setCaseExportUpdateTime(item.getUpdateTime());
        }
        Long projectId = query != null ? query.getProjectId() : null;
        Map<Long, String> modulePaths = loadModulePaths(projectId);
        List<CaseExcelColumnSupport.ColumnDef> columns =
                CaseExcelColumnSupport.resolveExportColumns(params, locale);List<ExcelHeaderCell> headers = columns.stream()
                .map(column -> new ExcelHeaderCell(column.header(),
                        column.required() ? ExcelHeaderStyle.REQUIRED : ExcelHeaderStyle.NORMAL))
                .toList();
        List<List<String>> rows = list.stream()
                .map(item -> columns.stream()
                        .map(column -> CaseExcelColumnSupport.formatExportValue(item, column, modulePaths))
                        .toList())
                .toList();
        Set<Integer> wrapTextColumns = new HashSet<>();
        for (int i = 0; i < columns.size(); i++) {
            if ("caseStep".equals(columns.get(i).fieldName())) {
                wrapTextColumns.add(i);
            }
        }
        ExcelTableModel model = ExcelTableModel.withHeaders(CaseExcelColumnSupport.SHEET_NAME, headers, rows, 20)
                .withWrapTextColumns(wrapTextColumns);
        return excelTableWriter.write(model, "导出用例数据失败");
    }private Map<Long, String> loadModulePaths(Long projectId) {
        if (projectId == null) {
            return Map.of();
        }
        return sysModuleService.selectSysModulePathList(projectId).stream()
                .collect(Collectors.toMap(SysModule::getModuleId, SysModule::getModulePath, (a, b) -> a));
    }
}
