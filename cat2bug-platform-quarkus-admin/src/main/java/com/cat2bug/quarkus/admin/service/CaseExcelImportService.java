package com.cat2bug.quarkus.admin.service;

import com.cat2bug.quarkus.common.domain.SysCase;
import com.cat2bug.quarkus.common.domain.SysModule;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.excel.ExcelSheetTable;
import com.cat2bug.quarkus.common.excel.FastExcelSupport;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.system.service.SysModuleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用例 Excel 导入解析。
 */
@ApplicationScoped
public class CaseExcelImportService {

    @Inject
    SysModuleService sysModuleService;

    public List<SysCase> parseWorkbook(InputStream input, Long projectId, Map<String, Object> params, Locale locale) {
        if (params == null) {
            params = Map.of();
        }
        List<CaseExcelColumnSupport.ColumnDef> templateColumns =
                CaseExcelColumnSupport.resolveTemplateColumns(params, locale);
        Map<String, Long> modulePathToId = sysModuleService.selectSysModulePathList(projectId).stream()
                .collect(Collectors.toMap(SysModule::getModulePath, SysModule::getModuleId, (a, b) -> a));
        ExcelSheetTable table;
        try {
            table = FastExcelSupport.readFirstSheet(input);
        } catch (IOException e) {
            throw new ApiException("解析用例 Excel 失败");
        }
        if (table.rowCount() == 0) {
            throw new ApiException("导入用例数据为空或格式不合法");
        }
        Map<Integer, CaseExcelColumnSupport.ColumnDef> columnByIndex = new HashMap<>();
        for (int colIndex = 0; colIndex < table.columnCount(0); colIndex++) {
            CaseExcelColumnSupport.ColumnDef column =
                    CaseExcelColumnSupport.findByHeader(cellText(table, 0, colIndex), templateColumns);
            if (column != null) {
                columnByIndex.put(colIndex, column);
            }
        }
        if (columnByIndex.isEmpty()) {
            throw new ApiException("导入用例数据为空或格式不合法");
        }
        List<SysCase> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < table.rowCount(); rowIndex++) {
            if (isBlankRow(table, rowIndex, columnByIndex)) {
                continue;
            }
            SysCase item = new SysCase();
            for (Map.Entry<Integer, CaseExcelColumnSupport.ColumnDef> entry : columnByIndex.entrySet()) {
                String value = cellText(table, rowIndex, entry.getKey());
                CaseExcelColumnSupport.applyImportValue(item, entry.getValue(), value, modulePathToId);
            }
            rows.add(item);
        }
        return rows;
    }

    private boolean isBlankRow(ExcelSheetTable table, int rowIndex,
                               Map<Integer, CaseExcelColumnSupport.ColumnDef> columnByIndex) {
        for (Integer colIndex : columnByIndex.keySet()) {
            if (StringUtils.isNotBlank(cellText(table, rowIndex, colIndex))) {
                return false;
            }
        }
        return true;
    }

    private static String cellText(ExcelSheetTable table, int rowIndex, int colIndex) {
        return UserExcelImportService.normalizeNumericText(table.cell(rowIndex, colIndex));
    }
}
