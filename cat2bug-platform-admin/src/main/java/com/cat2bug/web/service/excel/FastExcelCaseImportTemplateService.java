package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.web.excel.ExcelHeaderCell;
import com.cat2bug.web.excel.ExcelHeaderStyle;
import com.cat2bug.web.excel.FastExcelTemplateSupport;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.service.ISysModuleService;
import org.dhatim.fastexcel.Range;
import org.dhatim.fastexcel.Worksheet;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;/**
 * Native：用例导入模版（表头 + 隐藏列表 + 下拉校验）。
 */
@Profile("native")
@Service
public class FastExcelCaseImportTemplateService implements CaseImportTemplateService {@Autowired
    ISysModuleService sysModuleService;@Override
    public byte[] buildTemplateWorkbook(Long projectId, Map<String, Object> params, Locale locale) {
        if (projectId == null || projectId <= 0) {
            throw new ServiceException("项目ID不能为空");
        }
        List<CaseExcelColumnSupport.ColumnDef> columns =
                CaseExcelColumnSupport.resolveTemplateColumns(params, locale);
        List<String> modulePaths = sysModuleService.selectSysModulePathList(projectId).stream()
                .map(SysModule::getModulePath)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());List<ExcelHeaderCell> headers = columns.stream()
                .map(column -> new ExcelHeaderCell(column.header(),
                        column.required() ? ExcelHeaderStyle.REQUIRED : ExcelHeaderStyle.NORMAL))
                .toList();
        Map<Integer, Integer> widths = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            String field = columns.get(i).fieldName();
            int width = "caseStepScript".equals(field) || "caseData".equals(field) || "caseExpect".equals(field)
                    ? 40 : 20;
            widths.put(i, width);
        }int moduleCol = CaseExcelColumnSupport.moduleColumnIndex(columns);
        int levelCol = CaseExcelColumnSupport.levelColumnIndex(columns);try {
            return FastExcelTemplateSupport.writeWorkbook("Cat2Bug", CaseExcelColumnSupport.TEMPLATE_SHEET_NAME, ctx -> {
                Worksheet sheet = ctx.dataSheet();
                FastExcelTemplateSupport.writeHeaders(sheet, headers);
                Worksheet listSheet = FastExcelTemplateSupport.newHiddenListSheet(ctx.workbook(),
                        CaseExcelColumnSupport.LIST_SHEET_NAME);int listColumn = 0;
                if (moduleCol >= 0) {
                    Range moduleRange = FastExcelTemplateSupport.writeListColumn(listSheet, listColumn++, modulePaths);
                    FastExcelTemplateSupport.applyListValidation(sheet, moduleCol, moduleRange, 1,
                            CaseExcelColumnSupport.DATA_ROWS);
                }
                if (levelCol >= 0) {
                    Range levelRange = FastExcelTemplateSupport.writeListColumn(listSheet, listColumn,
                            CaseExcelColumnSupport.levelLabels());
                    FastExcelTemplateSupport.applyListValidation(sheet, levelCol, levelRange, 1,
                            CaseExcelColumnSupport.DATA_ROWS);
                }FastExcelTemplateSupport.applyColumnWidths(sheet, widths, 20);
            });
        } catch (IOException e) {
            throw new ServiceException("生成用例导入模版失败");
        }
    }
}
