package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.web.excel.ExcelHeaderCell;
import com.cat2bug.web.excel.ExcelHeaderStyle;
import com.cat2bug.web.excel.FastExcelTemplateSupport;
import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.core.domain.entity.SysDictData;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.poi.ExcelColumnExportSupport;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.service.ISysDictTypeService;
import com.cat2bug.system.service.ISysModuleService;
import com.cat2bug.system.service.ISysProjectDefectFieldService;
import com.cat2bug.system.service.ISysUserConfigService;
import com.cat2bug.system.service.ISysUserProjectService;
import com.cat2bug.system.util.DefectCustomFieldFastExcelSupport;
import org.dhatim.fastexcel.Range;
import org.dhatim.fastexcel.Worksheet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;/**
 * Native：缺陷导入模版（表头 + 隐藏列表 + 下拉校验）。
 */
@Profile("native")
@Service
public class FastExcelDefectImportTemplateService implements DefectImportTemplateService {private static final String LIST_SHEET_NAME = "_lists";@Autowired
    ISysUserConfigService sysUserConfigService;@Autowired
    ISysModuleService sysModuleService;@Autowired
    ISysUserProjectService sysUserProjectService;@Autowired
    ISysDictTypeService sysDictTypeService;@Autowired
    ISysProjectDefectFieldService sysProjectDefectFieldService;@Override
    public byte[] buildTemplateWorkbook(Map<String, Object> params, Locale locale) {
        LoginUser loginUser = requireLoginUser();
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(loginUser.getUserId());
        Long projectId = userConfig != null ? userConfig.getCurrentProjectId() : null;
        if (projectId == null || projectId <= 0) {
            throw new ServiceException("当前项目不能为空");
        }
        return buildTemplateWorkbook(projectId, params, locale);
    }@Override
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
                DefectExcelColumnSupport.resolveTemplateColumns(params, locale, customFieldDefs);List<ExcelHeaderCell> headers = columns.stream()
                .map(column -> new ExcelHeaderCell(column.header(),
                        column.required() ? ExcelHeaderStyle.REQUIRED : ExcelHeaderStyle.NORMAL))
                .toList();
        Map<Integer, Integer> widths = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            widths.put(i, "defectDescribe".equals(columns.get(i).fieldName()) ? 40 : 20);
        }try {
            return FastExcelTemplateSupport.writeWorkbook("Cat2Bug", DefectExcelColumnSupport.SHEET_NAME, ctx -> {
                Worksheet sheet = ctx.dataSheet();
                FastExcelTemplateSupport.writeHeaders(sheet, headers);
                Worksheet listSheet = FastExcelTemplateSupport.newHiddenListSheet(ctx.workbook(), LIST_SHEET_NAME);int listColumn = 0;
                listColumn = writeListAndValidate(sheet, listSheet, listColumn,
                        DefectExcelColumnSupport.columnIndex(columns, "defectTypeImportName"), defectTypeLabels());
                listColumn = writeListAndValidate(sheet, listSheet, listColumn,
                        DefectExcelColumnSupport.columnIndex(columns, "defectLevel"), defectLevelLabels());
                listColumn = writeListAndValidate(sheet, listSheet, listColumn,
                        DefectExcelColumnSupport.columnIndex(columns, "defectStateImportName"), defectStateLabels());
                listColumn = writeListAndValidate(sheet, listSheet, listColumn,
                        DefectExcelColumnSupport.columnIndex(columns, "moduleName"), modulePaths);
                listColumn = writeListAndValidate(sheet, listSheet, listColumn,
                        DefectExcelColumnSupport.columnIndex(columns, "handleByNames"), memberNames);for (int i = 0; i < columns.size(); i++) {
                    DefectExcelColumnSupport.ColumnDef column = columns.get(i);
                    if (column.custom() && "enum".equals(column.customDef().getFieldType())) {
                        String[] labels = DefectCustomFieldFastExcelSupport.resolveEnumComboLabels(column.customDef());
                        if (labels != null && labels.length > 0) {
                            Range listRange = FastExcelTemplateSupport.writeListColumn(listSheet, listColumn,
                                    List.of(labels));
                            listColumn++;
                            FastExcelTemplateSupport.applyListValidation(sheet, i, listRange, 1,
                                    DefectExcelColumnSupport.DATA_ROWS);
                        }
                    }
                }FastExcelTemplateSupport.applyColumnWidths(sheet, widths, 20);
            });
        } catch (IOException e) {
            throw new ServiceException("生成缺陷导入模版失败");
        }
    }

    private static int writeListAndValidate(Worksheet dataSheet, Worksheet listSheet, int listColumn,
                                            int dataColumn, List<String> values) {
        if (dataColumn < 0) {
            return listColumn;
        }
        Range listRange = FastExcelTemplateSupport.writeListColumn(listSheet, listColumn, values);
        FastExcelTemplateSupport.applyListValidation(dataSheet, dataColumn, listRange, 1,
                DefectExcelColumnSupport.DATA_ROWS);
        return listColumn + 1;
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
