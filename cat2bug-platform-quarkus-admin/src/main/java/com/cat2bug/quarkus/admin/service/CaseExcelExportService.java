package com.cat2bug.quarkus.admin.service;

import com.cat2bug.quarkus.common.domain.SysCase;
import com.cat2bug.quarkus.common.domain.SysModule;
import com.cat2bug.quarkus.common.excel.ExcelHeaderCell;
import com.cat2bug.quarkus.common.excel.ExcelHeaderStyle;
import com.cat2bug.quarkus.common.excel.ExcelTableModel;
import com.cat2bug.quarkus.system.service.SysCaseService;
import com.cat2bug.quarkus.system.service.SysModuleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用例 Excel 数据导出。
 */
@ApplicationScoped
public class CaseExcelExportService {

    @Inject
    SysCaseService sysCaseService;

    @Inject
    SysModuleService sysModuleService;

    public byte[] buildExportWorkbook(SysCase query, Map<String, Object> params, Locale locale) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.putIfAbsent("type", "export");
        List<SysCase> list = sysCaseService.selectSysCaseListAll(query);
        for (SysCase item : list) {
            item.setCaseExportUpdateTime(item.getUpdateTime());
        }
        Long projectId = query != null ? query.getProjectId() : null;
        Map<Long, String> modulePaths = loadModulePaths(projectId);
        List<CaseExcelColumnSupport.ColumnDef> columns =
                CaseExcelColumnSupport.resolveExportColumns(params, locale);

        List<ExcelHeaderCell> headers = columns.stream()
                .map(column -> new ExcelHeaderCell(column.header(),
                        column.required() ? ExcelHeaderStyle.REQUIRED : ExcelHeaderStyle.NORMAL))
                .toList();
        Set<Integer> wrapColumns = new HashSet<>();
        for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
            if ("caseStep".equals(columns.get(colIndex).fieldName())) {
                wrapColumns.add(colIndex);
            }
        }
        List<List<String>> rows = new ArrayList<>(list.size());
        for (SysCase item : list) {
            List<String> line = new ArrayList<>(columns.size());
            for (CaseExcelColumnSupport.ColumnDef column : columns) {
                line.add(CaseExcelColumnSupport.formatExportValue(item, column, modulePaths));
            }
            rows.add(line);
        }
        ExcelTableModel model = ExcelTableModel.withHeaders(CaseExcelColumnSupport.SHEET_NAME, headers, rows, 20)
                .withWrapTextColumns(wrapColumns);
        return SimpleExcelExportSupport.write(model, col -> SimpleExcelExportSupport.DEFAULT_COLUMN_WIDTH,
                "导出用例数据失败");
    }

    private Map<Long, String> loadModulePaths(Long projectId) {
        if (projectId == null) {
            return Map.of();
        }
        return sysModuleService.selectSysModulePathList(projectId).stream()
                .collect(Collectors.toMap(SysModule::getModuleId, SysModule::getModulePath, (a, b) -> a));
    }
}
