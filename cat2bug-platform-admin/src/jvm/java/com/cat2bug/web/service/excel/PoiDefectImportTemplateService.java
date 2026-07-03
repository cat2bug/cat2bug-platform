package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.core.domain.entity.SysDictData;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.poi.ExcelColumnExportSupport;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.service.ISysDictTypeService;
import com.cat2bug.system.service.ISysModuleService;
import com.cat2bug.system.service.ISysProjectDefectFieldService;
import com.cat2bug.system.service.ISysUserConfigService;
import com.cat2bug.system.service.ISysUserProjectService;
import com.cat2bug.system.util.DefectCustomFieldFastExcelSupport;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;/**
 * 缺陷 Excel 导入模版（Phase 3.4，POI 直写，不依赖 Spring ExcelUtil）。
 */
@Profile("!native")
@Service
public class PoiDefectImportTemplateService implements DefectImportTemplateService {private static final String LIST_SHEET_NAME = "_lists";@Autowired
    ISysUserConfigService sysUserConfigService;@Autowired
    ISysModuleService sysModuleService;@Autowired
    ISysUserProjectService sysUserProjectService;@Autowired
    ISysDictTypeService sysDictTypeService;@Autowired
    ISysProjectDefectFieldService sysProjectDefectFieldService;public byte[] buildTemplateWorkbook(Map<String, Object> params, Locale locale) {
        LoginUser loginUser = requireLoginUser();
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(loginUser.getUserId());
        Long projectId = userConfig != null ? userConfig.getCurrentProjectId() : null;
        if (projectId == null || projectId <= 0) {
            throw new ServiceException("当前项目不能为空");
        }
        return buildTemplateWorkbook(projectId, params, locale);
    }

    public byte[] buildTemplateWorkbook(Long projectId, Map<String, Object> params, Locale locale) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.putIfAbsent(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE, ExcelColumnExportSupport.SCOPE_IMPORT_TEMPLATE);List<String> modulePaths = sysModuleService.selectSysModulePathList(projectId).stream()
                .map(SysModule::getModulePath)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<String> memberNames = sysUserProjectService.selectSysUserListByProjectId(projectId, new SysUser()).stream()
                .map(SysUser::getNickName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());List<SysProjectDefectField> customFieldDefs =
                sysProjectDefectFieldService.selectEnabledListByProjectId(projectId);
        List<DefectExcelColumnSupport.ColumnDef> columns =
                DefectExcelColumnSupport.resolveTemplateColumns(params, locale, customFieldDefs);try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(DefectExcelColumnSupport.SHEET_NAME);
            Sheet listSheet = workbook.createSheet(LIST_SHEET_NAME);
            workbook.setSheetHidden(workbook.getSheetIndex(listSheet), true);writeHeaderRow(workbook, sheet, columns);
            int listColumn = 0;
            listColumn = writeListColumn(workbook, listSheet, listColumn, "typeList", defectTypeLabels());
            listColumn = writeListColumn(workbook, listSheet, listColumn, "levelList", defectLevelLabels());
            listColumn = writeListColumn(workbook, listSheet, listColumn, "stateList", defectStateLabels());
            listColumn = writeListColumn(workbook, listSheet, listColumn, "moduleList", modulePaths);
            writeListColumn(workbook, listSheet, listColumn, "memberList", memberNames);applyListValidation(sheet, DefectExcelColumnSupport.columnIndex(columns, "defectTypeImportName"), "typeList");
            applyListValidation(sheet, DefectExcelColumnSupport.columnIndex(columns, "defectLevel"), "levelList");
            applyListValidation(sheet, DefectExcelColumnSupport.columnIndex(columns, "defectStateImportName"), "stateList");
            applyListValidation(sheet, DefectExcelColumnSupport.columnIndex(columns, "moduleName"), "moduleList");
            applyListValidation(sheet, DefectExcelColumnSupport.columnIndex(columns, "handleByNames"), "memberList");for (int i = 0; i < columns.size(); i++) {
                DefectExcelColumnSupport.ColumnDef column = columns.get(i);
                if (column.custom() && "enum".equals(column.customDef().getFieldType())) {
                    applyExplicitListValidation(sheet, i, DefectCustomFieldFastExcelSupport.resolveEnumComboLabels(column.customDef()));
                }
            }for (int i = 0; i < columns.size(); i++) {
                sheet.setColumnWidth(i, "defectDescribe".equals(columns.get(i).fieldName()) ? 10000 : 5000);
            }workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ServiceException("生成缺陷导入模版失败");
        }
    }private void writeHeaderRow(Workbook workbook, Sheet sheet, List<DefectExcelColumnSupport.ColumnDef> columns) {
        Row headerRow = sheet.createRow(0);
        CellStyle requiredStyle = createHeaderStyle(workbook, true);
        CellStyle normalStyle = createHeaderStyle(workbook, false);
        for (int i = 0; i < columns.size(); i++) {
            DefectExcelColumnSupport.ColumnDef column = columns.get(i);
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(column.header());
            cell.setCellStyle(column.required() ? requiredStyle : normalStyle);
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
    }private int writeListColumn(Workbook workbook, Sheet listSheet, int columnIndex, String rangeName, List<String> values) {
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
        namedRange.setRefersToFormula("'" + LIST_SHEET_NAME + "'!$" + columnLetter + "$1:$"
                + columnLetter + "$" + safeValues.size());
        return columnIndex + 1;
    }private void applyListValidation(Sheet sheet, int columnIndex, String rangeName) {
        if (columnIndex < 0) {
            return;
        }
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(rangeName);
        CellRangeAddressList regions = new CellRangeAddressList(1, DefectExcelColumnSupport.DATA_ROWS, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }private void applyExplicitListValidation(Sheet sheet, int columnIndex, String[] values) {
        if (columnIndex < 0 || values == null || values.length == 0) {
            return;
        }
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        CellRangeAddressList regions = new CellRangeAddressList(1, DefectExcelColumnSupport.DATA_ROWS, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }private List<String> defectTypeLabels() {
        List<String> labels = new ArrayList<>();
        DefectConstants.defectTypeLabels().forEach(labels::add);
        return labels;
    }private List<String> defectStateLabels() {
        List<String> labels = new ArrayList<>();
        for (String state : DefectConstants.defectStateLabels()) {
            labels.add(state);
        }
        return labels;
    }private List<String> defectLevelLabels() {
        List<SysDictData> dictData = sysDictTypeService.selectDictDataByType("defect_level");
        if (dictData == null || dictData.isEmpty()) {
            return List.of();
        }
        return dictData.stream()
                .map(SysDictData::getDictLabel)
                .filter(label -> label != null && !label.isBlank())
                .collect(Collectors.toList());
    }private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException("未登录");
        }
        return loginUser;
    }
}
