package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.service.ISysModuleService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;/**
 * 用例 Excel 导入模版（POI 最小实现，不依赖 Spring ExcelUtil）。
 */
@Profile("!native")
@Service
public class PoiCaseImportTemplateService implements CaseImportTemplateService {@Autowired
    ISysModuleService sysModuleService;public byte[] buildTemplateWorkbook(Long projectId, Map<String, Object> params, Locale locale) {
        if (projectId == null || projectId <= 0) {
            throw new ServiceException("项目ID不能为空");
        }
        List<CaseExcelColumnSupport.ColumnDef> columns =
                CaseExcelColumnSupport.resolveTemplateColumns(params, locale);
        List<String> modulePaths = sysModuleService.selectSysModulePathList(projectId).stream()
                .map(SysModule::getModulePath)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(CaseExcelColumnSupport.TEMPLATE_SHEET_NAME);
            Sheet listSheet = workbook.createSheet(CaseExcelColumnSupport.LIST_SHEET_NAME);
            workbook.setSheetHidden(workbook.getSheetIndex(listSheet), true);writeHeaderRow(workbook, sheet, columns);
            int listColumn = 0;
            int moduleCol = CaseExcelColumnSupport.moduleColumnIndex(columns);
            int levelCol = CaseExcelColumnSupport.levelColumnIndex(columns);
            if (moduleCol >= 0) {
                listColumn = writeListColumn(workbook, listSheet, listColumn, "moduleList", modulePaths);
                applyListValidation(sheet, moduleCol, "moduleList");
            }
            if (levelCol >= 0) {
                writeListColumn(workbook, listSheet, listColumn, "levelList", CaseExcelColumnSupport.levelLabels());
                applyListValidation(sheet, levelCol, "levelList");
            }for (int i = 0; i < columns.size(); i++) {
                CaseExcelColumnSupport.ColumnDef column = columns.get(i);
                int width = "caseStepScript".equals(column.fieldName()) || "caseData".equals(column.fieldName())
                        || "caseExpect".equals(column.fieldName()) ? 10000 : 5000;
                sheet.setColumnWidth(i, width);
            }workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ServiceException("生成用例导入模版失败");
        }
    }private void writeHeaderRow(Workbook workbook, Sheet sheet, List<CaseExcelColumnSupport.ColumnDef> columns) {
        Row headerRow = sheet.createRow(0);
        CellStyle requiredStyle = createHeaderStyle(workbook, true);
        CellStyle normalStyle = createHeaderStyle(workbook, false);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns.get(i).header());
            cell.setCellStyle(columns.get(i).required() ? requiredStyle : normalStyle);
        }
    }private CellStyle createHeaderStyle(Workbook workbook, boolean required) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        if (required) {
            font.setColor(IndexedColors.RED.getIndex());
        }
        style.setFont(font);
        return style;
    }private int writeListColumn(Workbook workbook, Sheet listSheet, int columnIndex, String rangeName,
                                List<String> values) {
        List<String> safeValues = values == null || values.isEmpty() ? List.of("") : values;
        for (int rowIndex = 0; rowIndex < safeValues.size(); rowIndex++) {
            Row row = listSheet.getRow(rowIndex);
            if (row == null) {
                row = listSheet.createRow(rowIndex);
            }
            row.createCell(columnIndex).setCellValue(safeValues.get(rowIndex));
        }
        Name namedRange = workbook.createName();
        namedRange.setNameName(rangeName);
        char columnLetter = (char) ('A' + columnIndex);
        namedRange.setRefersToFormula("'" + CaseExcelColumnSupport.LIST_SHEET_NAME + "'!$" + columnLetter + "$1:$"
                + columnLetter + "$" + safeValues.size());
        return columnIndex + 1;
    }private void applyListValidation(Sheet sheet, int columnIndex, String rangeName) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(rangeName);
        CellRangeAddressList regions = new CellRangeAddressList(1, CaseExcelColumnSupport.DATA_ROWS,
                columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }
}
