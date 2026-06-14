package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.web.excel.ExcelSheetReader;
import com.cat2bug.web.excel.ExcelSheetTable;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysDictData;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.poi.ExcelColumnExportSupport;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.service.ISysDefectService;
import com.cat2bug.system.service.ISysDictTypeService;
import com.cat2bug.system.service.ISysModuleService;
import com.cat2bug.system.service.ISysProjectDefectFieldService;
import com.cat2bug.system.service.ISysUserConfigService;
import com.cat2bug.system.util.DefectCustomFieldFastExcelSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;/**
 * 缺陷 Excel 导出/导入（B9：引擎无关读写 SPI）。
 */
@Service
public class DefectExcelService {@Autowired
    ISysUserConfigService sysUserConfigService;@Autowired
    ISysDefectService sysDefectService;@Autowired
    ISysModuleService sysModuleService;@Autowired
    ISysDictTypeService sysDictTypeService;@Autowired
    ISysProjectDefectFieldService sysProjectDefectFieldService;@Autowired
    DefectExcelExportWriter defectExcelExportWriter;@Autowired
    ExcelSheetReader excelSheetReader;public byte[] buildExportWorkbook(SysDefect query, Map<String, Object> params, Locale locale) {
        if (query == null || query.getProjectId() == null) {
            throw new ServiceException("项目不能为空");
        }
        if (params == null) {
            params = new HashMap<>();
        }
        params.putIfAbsent(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE, ExcelColumnExportSupport.SCOPE_DATA);List<SysDictData> levelDict = sysDictTypeService.selectDictDataByType("defect_level");
        Map<Long, String> modulePathById = sysModuleService.selectSysModulePathList(query.getProjectId()).stream()
                .filter(module -> module.getModuleId() != null && module.getModulePath() != null)
                .collect(Collectors.toMap(SysModule::getModuleId, SysModule::getModulePath, (left, right) -> left));List<SysDefect> defects = sysDefectService.selectSysDefectList(query);
        for (SysDefect defect : defects) {
            if (defect.getHandleByList() != null && !defect.getHandleByList().isEmpty()) {
                defect.setHandleByNames(defect.getHandleByList().stream()
                        .map(user -> user.getNickName())
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("/")));
            }
            if (defect.getModuleId() != null) {
                defect.setModulePath(modulePathById.get(defect.getModuleId()));
            }
        }List<SysProjectDefectField> customFieldDefs =
                sysProjectDefectFieldService.selectEnabledListByProjectId(query.getProjectId());
        List<DefectExcelColumnSupport.ColumnDef> columns =
                DefectExcelColumnSupport.resolveExportColumns(params, locale, customFieldDefs);return defectExcelExportWriter.writeWorkbook(defects, columns, levelDict);
    }

    public List<SysDefect> parseImportWorkbook(byte[] fileBytes, Long projectId, Map<String, Object> params,
                                               Locale locale) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new ServiceException("导入文件不能为空");
        }
        if (params == null) {
            params = new HashMap<>();
        }
        params.putIfAbsent(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE, ExcelColumnExportSupport.SCOPE_IMPORT_TEMPLATE);List<SysProjectDefectField> customFieldDefs =
                sysProjectDefectFieldService.selectEnabledListByProjectId(projectId);
        List<DefectExcelColumnSupport.ColumnDef> templateColumns =
                DefectExcelColumnSupport.resolveImportColumns(params, locale, customFieldDefs);ExcelSheetTable table;
        try {
            table = excelSheetReader.readFirstSheet(new ByteArrayInputStream(fileBytes));
        } catch (IOException e) {
            throw new ServiceException("解析导入文件失败");
        }
        if (table.rowCount() == 0) {
            throw new ServiceException("Excel 工作表不能为空");
        }
        int headerRowIndex = table.findHeaderRowIndex();
        List<String> headerCells = table.headerRow(headerRowIndex);
        if (headerCells.isEmpty()) {
            throw new ServiceException("Excel 表头不能为空");
        }Map<Integer, DefectExcelColumnSupport.ColumnDef> columnByIndex = new HashMap<>();
        for (int i = 0; i < headerCells.size(); i++) {
            String header = trimToNull(headerCells.get(i));
            DefectExcelColumnSupport.ColumnDef column =
                    DefectExcelColumnSupport.findByHeader(header, templateColumns);
            if (column != null) {
                columnByIndex.put(i, column);
            }
        }List<SysDefect> defects = new ArrayList<>();
        for (int rowIndex = headerRowIndex + 1; rowIndex < table.rowCount(); rowIndex++) {
            if (table.isRowEmpty(rowIndex)) {
                continue;
            }
            if (!hasMappedColumnValue(table, rowIndex, columnByIndex)) {
                continue;
            }
            SysDefect defect = new SysDefect();
            for (Map.Entry<Integer, DefectExcelColumnSupport.ColumnDef> entry : columnByIndex.entrySet()) {
                String value = trimToNull(table.cellText(rowIndex, entry.getKey()));
                DefectExcelColumnSupport.applyImportValue(defect, entry.getValue(), value);
            }
            defects.add(defect);
        }try {
            DefectCustomFieldFastExcelSupport.mergeCustomFieldsFromRows(table.rows(), defects, customFieldDefs);
        } catch (Exception e) {
            throw new ServiceException("解析导入文件失败");
        }
        return defects;
    }

    public Long resolveCurrentProjectId() {
        LoginUser loginUser = requireLoginUser();
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(loginUser.getUserId());
        Long projectId = userConfig != null ? userConfig.getCurrentProjectId() : null;
        if (projectId == null || projectId <= 0) {
            throw new ServiceException("当前项目不能为空");
        }
        return projectId;
    }

    private static boolean hasMappedColumnValue(ExcelSheetTable table, int rowIndex,
                                                Map<Integer, DefectExcelColumnSupport.ColumnDef> columnByIndex) {
        for (Integer columnIndex : columnByIndex.keySet()) {
            if (StringUtils.isNotEmpty(trimToNull(table.cellText(rowIndex, columnIndex)))) {
                return true;
            }
        }
        return false;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException("未登录");
        }
        return loginUser;
    }
}
