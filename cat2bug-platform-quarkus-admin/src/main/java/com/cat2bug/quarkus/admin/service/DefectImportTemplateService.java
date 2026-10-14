package com.cat2bug.quarkus.admin.service;

import com.cat2bug.quarkus.common.constant.DefectConstants;
import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysDictData;
import com.cat2bug.quarkus.common.domain.SysModule;
import com.cat2bug.quarkus.common.domain.SysProjectDefectField;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.common.domain.SysUserConfig;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.excel.ExcelColumnExportSupport;
import com.cat2bug.quarkus.common.excel.ExcelHeaderCell;
import com.cat2bug.quarkus.common.excel.ExcelHeaderStyle;
import com.cat2bug.quarkus.common.excel.FastExcelSupport;
import com.cat2bug.quarkus.framework.security.AuthContext;
import com.cat2bug.quarkus.system.service.SysDictDataService;
import com.cat2bug.quarkus.system.service.SysModuleService;
import com.cat2bug.quarkus.system.service.SysProjectDefectFieldService;
import com.cat2bug.quarkus.system.service.SysUserConfigService;
import com.cat2bug.quarkus.system.service.SysUserProjectService;
import com.cat2bug.quarkus.system.util.DefectCustomFieldExcelSupport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.dhatim.fastexcel.DataValidationErrorStyle;
import org.dhatim.fastexcel.Range;
import org.dhatim.fastexcel.VisibilityState;
import org.dhatim.fastexcel.Worksheet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 缺陷 Excel 导入模版（Phase 3.4，FastExcel 直写，不依赖 Spring ExcelUtil）。
 */
@ApplicationScoped
public class DefectImportTemplateService {

    private static final String LIST_SHEET_NAME = "_lists";

    @Inject
    AuthContext authContext;

    @Inject
    SysUserConfigService sysUserConfigService;

    @Inject
    SysModuleService sysModuleService;

    @Inject
    SysUserProjectService sysUserProjectService;

    @Inject
    SysDictDataService sysDictDataService;

    @Inject
    SysProjectDefectFieldService sysProjectDefectFieldService;

    public byte[] buildTemplateWorkbook(Map<String, Object> params, Locale locale) {
        LoginUser loginUser = requireLoginUser();
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(loginUser.getUserId());
        Long projectId = userConfig != null ? userConfig.getCurrentProjectId() : null;
        if (projectId == null || projectId <= 0) {
            throw new ApiException("当前项目不能为空");
        }
        return buildTemplateWorkbook(projectId, params, locale);
    }

    public byte[] buildTemplateWorkbook(Long projectId, Map<String, Object> params, Locale locale) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.putIfAbsent(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE, ExcelColumnExportSupport.SCOPE_IMPORT_TEMPLATE);

        List<String> modulePaths = sysModuleService.selectSysModulePathList(projectId).stream()
                .map(SysModule::getModulePath)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<String> memberNames = sysUserProjectService.selectSysUserListByProjectId(projectId).stream()
                .map(SysUser::getNickName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());

        List<SysProjectDefectField> customFieldDefs =
                sysProjectDefectFieldService.selectEnabledListByProjectId(projectId);
        List<DefectExcelColumnSupport.ColumnDef> columns =
                DefectExcelColumnSupport.resolveTemplateColumns(params, locale, customFieldDefs);

        return FastExcelSupport.writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet(DefectExcelColumnSupport.SHEET_NAME);
            Worksheet listSheet = workbook.newWorksheet(LIST_SHEET_NAME);
            listSheet.setVisibilityState(VisibilityState.HIDDEN);

            FastExcelSupport.writeHeaders(sheet, headerCells(columns), 0);
            int listColumn = 0;
            listColumn = writeListColumn(listSheet, listColumn, "typeList", defectTypeLabels());
            listColumn = writeListColumn(listSheet, listColumn, "levelList", defectLevelLabels());
            listColumn = writeListColumn(listSheet, listColumn, "stateList", defectStateLabels());
            listColumn = writeListColumn(listSheet, listColumn, "moduleList", modulePaths);
            writeListColumn(listSheet, listColumn, "memberList", memberNames);

            applyListValidation(sheet, DefectExcelColumnSupport.columnIndex(columns, "defectTypeImportName"), "typeList");
            applyListValidation(sheet, DefectExcelColumnSupport.columnIndex(columns, "defectLevel"), "levelList");
            applyListValidation(sheet, DefectExcelColumnSupport.columnIndex(columns, "defectStateImportName"), "stateList");
            applyListValidation(sheet, DefectExcelColumnSupport.columnIndex(columns, "moduleName"), "moduleList");
            applyListValidation(sheet, DefectExcelColumnSupport.columnIndex(columns, "handleByNames"), "memberList");

            for (int i = 0; i < columns.size(); i++) {
                DefectExcelColumnSupport.ColumnDef column = columns.get(i);
                if (column.custom() && "enum".equals(column.customDef().getFieldType())) {
                    applyExplicitListValidation(sheet, i, DefectCustomFieldExcelSupport.resolveEnumComboLabels(column.customDef()));
                }
            }

            for (int i = 0; i < columns.size(); i++) {
                sheet.width(i, ("defectDescribe".equals(columns.get(i).fieldName()) ? 10000 : 5000) / 256d);
            }
        }, "生成缺陷导入模版失败");
    }

    /** 浅灰底加粗表头，必填列红字。 */
    private static List<ExcelHeaderCell> headerCells(List<DefectExcelColumnSupport.ColumnDef> columns) {
        return columns.stream()
                .map(column -> new ExcelHeaderCell(column.header(),
                        column.required() ? ExcelHeaderStyle.REQUIRED : ExcelHeaderStyle.NORMAL))
                .toList();
    }

    private int writeListColumn(Worksheet listSheet, int columnIndex, String rangeName, List<String> values) {
        List<String> safeValues = values == null || values.isEmpty() ? List.of("") : values;
        for (int rowIndex = 0; rowIndex < safeValues.size(); rowIndex++) {
            listSheet.value(rowIndex, columnIndex, safeValues.get(rowIndex));
        }
        // 工作簿级名称（不带 localSheetId），数据页的下拉才能引用到
        Range namedRange = listSheet.range(0, columnIndex, safeValues.size() - 1, columnIndex);
        namedRange.setFolderScope(true);
        listSheet.addNamedRange(namedRange, rangeName);
        return columnIndex + 1;
    }

    private void applyListValidation(Worksheet sheet, int columnIndex, String rangeName) {
        if (columnIndex < 0) {
            return;
        }
        addListValidation(sheet, columnIndex, rangeName);
    }

    private void applyExplicitListValidation(Worksheet sheet, int columnIndex, String[] values) {
        if (columnIndex < 0 || values == null || values.length == 0) {
            return;
        }
        addListValidation(sheet, columnIndex, "\"" + String.join(",", values) + "\"");
    }

    /** 数据区（第 1..DATA_ROWS 行）下拉校验：显示下拉箭头、允许空值、非法值以 stop 样式拦截。 */
    private static void addListValidation(Worksheet sheet, int columnIndex, String formula) {
        // FastExcel 原样写出公式与 errorTitle/error（null 会写成 "null"），需自行转义并显式置空
        sheet.range(1, columnIndex, DefectExcelColumnSupport.DATA_ROWS, columnIndex)
                .validateWithListByFormula(escapeXml(formula))
                .allowBlank(true)
                .showDropdown(true)
                .errorStyle(DataValidationErrorStyle.STOP)
                .showErrorMessage(true)
                .errorTitle("")
                .error("");
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private List<String> defectTypeLabels() {
        List<String> labels = new ArrayList<>();
        DefectConstants.getDefectTypeList().forEach(item -> labels.add(item.getValue()));
        return labels;
    }

    private List<String> defectStateLabels() {
        List<String> labels = new ArrayList<>();
        for (String state : DefectConstants.DEFECT_STATES) {
            labels.add(state);
        }
        return labels;
    }

    private List<String> defectLevelLabels() {
        List<SysDictData> dictData = sysDictDataService.selectDictDataByType("defect_level");
        if (dictData == null || dictData.isEmpty()) {
            return List.of();
        }
        return dictData.stream()
                .map(SysDictData::getDictLabel)
                .filter(label -> label != null && !label.isBlank())
                .collect(Collectors.toList());
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录");
        }
        return loginUser;
    }
}
