package com.cat2bug.admin.service;

import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.domain.SysModule;
import com.cat2bug.common.excel.ExcelHeaderCell;
import com.cat2bug.common.excel.ExcelHeaderStyle;
import com.cat2bug.common.excel.ExcelImageEmbedSupport;
import com.cat2bug.common.excel.FastExcelSupport;
import com.cat2bug.system.service.SysCaseService;
import com.cat2bug.system.service.SysModuleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.dhatim.fastexcel.Worksheet;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        String assetHost = params.get("host") != null ? String.valueOf(params.get("host")).trim() : null;
        return FastExcelSupport.writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet(CaseExcelColumnSupport.SHEET_NAME);
            FastExcelSupport.writeHeaders(sheet, headers, 0);
            for (int rowIndex = 0; rowIndex < list.size(); rowIndex++) {
                SysCase item = list.get(rowIndex);
                int excelRow = rowIndex + 1;
                for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                    CaseExcelColumnSupport.ColumnDef column = columns.get(colIndex);
                    String value = CaseExcelColumnSupport.formatExportValue(item, column, modulePaths);
                    if (value == null || value.isEmpty()) {
                        continue;
                    }
                    if (CaseExcelColumnSupport.isImageColumn(column)
                            && ExcelImageEmbedSupport.embedInCell(sheet, excelRow, colIndex, value, assetHost)) {
                        continue;
                    }
                    sheet.value(excelRow, colIndex, value);
                    if ("caseStep".equals(column.fieldName())) {
                        sheet.style(excelRow, colIndex).wrapText(true).set();
                    }
                }
            }
            for (int i = 0; i < columns.size(); i++) {
                sheet.width(i, CaseExcelColumnSupport.isImageColumn(columns.get(i))
                        ? ExcelImageEmbedSupport.IMAGE_COLUMN_WIDTH_CHARS
                        : SimpleExcelExportSupport.DEFAULT_COLUMN_WIDTH);
            }
        }, "导出用例数据失败");
    }

    private Map<Long, String> loadModulePaths(Long projectId) {
        if (projectId == null) {
            return Map.of();
        }
        return sysModuleService.selectSysModulePathList(projectId).stream()
                .collect(Collectors.toMap(SysModule::getModuleId, SysModule::getModulePath, (a, b) -> a));
    }
}
