package com.cat2bug.admin.service;

import com.cat2bug.common.domain.SysModule;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.excel.ExcelHeaderCell;
import com.cat2bug.common.excel.ExcelHeaderStyle;
import com.cat2bug.common.excel.FastExcelSupport;
import com.cat2bug.system.service.SysModuleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.dhatim.fastexcel.DataValidationErrorStyle;
import org.dhatim.fastexcel.Range;
import org.dhatim.fastexcel.VisibilityState;
import org.dhatim.fastexcel.Worksheet;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用例 Excel 导入模版（FastExcel 最小实现，不依赖 Spring ExcelUtil）。
 */
@ApplicationScoped
public class CaseImportTemplateService {

    @Inject
    SysModuleService sysModuleService;

    public byte[] buildTemplateWorkbook(Long projectId, Map<String, Object> params, Locale locale) {
        if (projectId == null || projectId <= 0) {
            throw new ApiException("项目ID不能为空");
        }
        List<CaseExcelColumnSupport.ColumnDef> columns =
                CaseExcelColumnSupport.resolveTemplateColumns(params, locale);
        List<String> modulePaths = sysModuleService.selectSysModulePathList(projectId).stream()
                .map(SysModule::getModulePath)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<ExcelHeaderCell> headers = columns.stream()
                .map(column -> new ExcelHeaderCell(column.header(),
                        column.required() ? ExcelHeaderStyle.REQUIRED : ExcelHeaderStyle.NORMAL))
                .toList();

        return FastExcelSupport.writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet(CaseExcelColumnSupport.TEMPLATE_SHEET_NAME);
            Worksheet listSheet = workbook.newWorksheet(CaseExcelColumnSupport.LIST_SHEET_NAME);
            listSheet.setVisibilityState(VisibilityState.HIDDEN);

            FastExcelSupport.writeHeaders(sheet, headers, 0);
            int listColumn = 0;
            int moduleCol = CaseExcelColumnSupport.moduleColumnIndex(columns);
            int levelCol = CaseExcelColumnSupport.levelColumnIndex(columns);
            if (moduleCol >= 0) {
                listColumn = writeListColumn(listSheet, listColumn, "moduleList", modulePaths);
                applyListValidation(sheet, moduleCol, "moduleList");
            }
            if (levelCol >= 0) {
                writeListColumn(listSheet, listColumn, "levelList", CaseExcelColumnSupport.levelLabels());
                applyListValidation(sheet, levelCol, "levelList");
            }

            for (int i = 0; i < columns.size(); i++) {
                CaseExcelColumnSupport.ColumnDef column = columns.get(i);
                double width = "caseStepScript".equals(column.fieldName()) || "caseData".equals(column.fieldName())
                        || "caseExpect".equals(column.fieldName())
                        ? SimpleExcelExportSupport.WIDE_COLUMN_WIDTH : SimpleExcelExportSupport.DEFAULT_COLUMN_WIDTH;
                sheet.width(i, width);
            }
        }, "生成用例导入模版失败");
    }

    private int writeListColumn(Worksheet listSheet, int columnIndex, String rangeName, List<String> values) {
        List<String> safeValues = values == null || values.isEmpty() ? List.of("") : values;
        for (int rowIndex = 0; rowIndex < safeValues.size(); rowIndex++) {
            listSheet.value(rowIndex, columnIndex, safeValues.get(rowIndex));
        }
        Range namedRange = listSheet.range(0, columnIndex, safeValues.size() - 1, columnIndex);
        namedRange.setFolderScope(true);
        namedRange.setName(rangeName);
        return columnIndex + 1;
    }

    /** FastExcel 默认错误样式为 INFORMATION，且未设置标题/内容时会写出字面量 "null"。 */
    private void applyListValidation(Worksheet sheet, int columnIndex, String rangeName) {
        sheet.range(1, columnIndex, CaseExcelColumnSupport.DATA_ROWS, columnIndex)
                .validateWithListByFormula(rangeName)
                .showDropdown(true)
                .allowBlank(true)
                .errorStyle(DataValidationErrorStyle.STOP)
                .showErrorMessage(true)
                .errorTitle("")
                .error("");
    }
}
