package com.cat2bug.admin.service;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysDictData;
import com.cat2bug.common.domain.SysModule;
import com.cat2bug.common.domain.SysProjectDefectField;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.excel.ExcelColumnExportSupport;
import com.cat2bug.common.excel.ExcelHeaderCell;
import com.cat2bug.common.excel.ExcelHeaderStyle;
import com.cat2bug.common.excel.FastExcelSupport;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.service.SysDefectService;
import com.cat2bug.system.service.SysDictDataService;
import com.cat2bug.system.service.SysModuleService;
import com.cat2bug.system.service.SysProjectDefectFieldService;
import com.cat2bug.system.service.SysUserConfigService;
import com.cat2bug.system.util.DefectCustomFieldExcelSupport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 缺陷 Excel 导出/导入（Phase 3.4，FastExcel 直写，不依赖 Spring ExcelUtil）。
 */
@ApplicationScoped
public class DefectExcelService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Inject
    AuthContext authContext;

    @Inject
    SysUserConfigService sysUserConfigService;

    @Inject
    SysDefectService sysDefectService;

    @Inject
    SysModuleService sysModuleService;

    @Inject
    SysDictDataService sysDictDataService;

    @Inject
    SysProjectDefectFieldService sysProjectDefectFieldService;

    public byte[] buildExportWorkbook(SysDefect query, Map<String, Object> params, Locale locale) {
        if (query == null || query.getProjectId() == null) {
            throw new ApiException("项目不能为空");
        }
        if (params == null) {
            params = new HashMap<>();
        }
        params.putIfAbsent(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE, ExcelColumnExportSupport.SCOPE_DATA);

        List<SysDictData> levelDict = sysDictDataService.selectDictDataByType("defect_level");
        Map<Long, String> modulePathById = sysModuleService.selectSysModulePathList(query.getProjectId()).stream()
                .filter(module -> module.getModuleId() != null && module.getModulePath() != null)
                .collect(Collectors.toMap(SysModule::getModuleId, SysModule::getModulePath, (left, right) -> left));

        List<SysDefect> defects = sysDefectService.selectAllSysDefectList(query);
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
        }

        List<SysProjectDefectField> customFieldDefs =
                sysProjectDefectFieldService.selectEnabledListByProjectId(query.getProjectId());
        List<DefectExcelColumnSupport.ColumnDef> columns =
                DefectExcelColumnSupport.resolveExportColumns(params, locale, customFieldDefs);

        return FastExcelSupport.writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet(DefectExcelColumnSupport.SHEET_NAME);
            FastExcelSupport.writeHeaders(sheet, headerCells(columns), 0);
            for (int rowIndex = 0; rowIndex < defects.size(); rowIndex++) {
                SysDefect defect = defects.get(rowIndex);
                for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                    DefectExcelColumnSupport.ColumnDef column = columns.get(colIndex);
                    String value = DefectExcelColumnSupport.formatExportValue(defect, column, levelDict);
                    if (value != null && !value.isEmpty()) {
                        sheet.value(rowIndex + 1, colIndex, value);
                    }
                }
            }
            for (int i = 0; i < columns.size(); i++) {
                sheet.width(i, columnWidthChars(columns.get(i)));
            }
        }, "导出缺陷数据失败");
    }

    public List<SysDefect> parseImportWorkbook(byte[] fileBytes, Long projectId, Map<String, Object> params,
                                               Locale locale) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new ApiException("导入文件不能为空");
        }
        if (params == null) {
            params = new HashMap<>();
        }
        params.putIfAbsent(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE, ExcelColumnExportSupport.SCOPE_IMPORT_TEMPLATE);

        List<SysProjectDefectField> customFieldDefs =
                sysProjectDefectFieldService.selectEnabledListByProjectId(projectId);
        List<DefectExcelColumnSupport.ColumnDef> templateColumns =
                DefectExcelColumnSupport.resolveImportColumns(params, locale, customFieldDefs);

        try (InputStream input = new ByteArrayInputStream(fileBytes)) {
            List<List<Cell>> rows = DefectCustomFieldExcelSupport.readFirstSheetCells(input);
            List<Cell> headerRow = rows.isEmpty() ? null : rows.get(0);
            if (headerRow == null || headerRow.isEmpty()) {
                throw new ApiException("Excel 表头不能为空");
            }

            Map<Integer, DefectExcelColumnSupport.ColumnDef> columnByIndex = new HashMap<>();
            for (int i = 0; i < headerRow.size(); i++) {
                String header = readString(headerRow, i);
                DefectExcelColumnSupport.ColumnDef column =
                        DefectExcelColumnSupport.findByHeader(header, templateColumns);
                if (column != null) {
                    columnByIndex.put(i, column);
                }
            }

            List<SysDefect> defects = new ArrayList<>();
            for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
                List<Cell> row = rows.get(rowIndex);
                if (row.isEmpty() || isRowEmpty(row, columnByIndex)) {
                    continue;
                }
                SysDefect defect = new SysDefect();
                for (Map.Entry<Integer, DefectExcelColumnSupport.ColumnDef> entry : columnByIndex.entrySet()) {
                    String value = readString(row, entry.getKey());
                    DefectExcelColumnSupport.applyImportValue(defect, entry.getValue(), value);
                }
                defects.add(defect);
            }

            DefectCustomFieldExcelSupport.mergeCustomFieldsFromImport(rows, defects, customFieldDefs);
            return defects;
        } catch (IOException e) {
            throw new ApiException("解析导入文件失败");
        } catch (Exception e) {
            throw new ApiException("解析导入文件失败");
        }
    }

    public Long resolveCurrentProjectId() {
        LoginUser loginUser = requireLoginUser();
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(loginUser.getUserId());
        Long projectId = userConfig != null ? userConfig.getCurrentProjectId() : null;
        if (projectId == null || projectId <= 0) {
            throw new ApiException("当前项目不能为空");
        }
        return projectId;
    }

    /** 浅灰底加粗表头，必填列红字。 */
    private static List<ExcelHeaderCell> headerCells(List<DefectExcelColumnSupport.ColumnDef> columns) {
        return columns.stream()
                .map(column -> new ExcelHeaderCell(column.header(),
                        column.required() ? ExcelHeaderStyle.REQUIRED : ExcelHeaderStyle.NORMAL))
                .toList();
    }

    /** 与原 POI 列宽一致：描述列 10000、其余 5000（单位 1/256 字符）。 */
    private static double columnWidthChars(DefectExcelColumnSupport.ColumnDef column) {
        return ("defectDescribe".equals(column.fieldName()) ? 10000 : 5000) / 256d;
    }

    private static boolean isRowEmpty(List<Cell> row, Map<Integer, DefectExcelColumnSupport.ColumnDef> columnByIndex) {
        for (Integer columnIndex : columnByIndex.keySet()) {
            if (StringUtils.isNotEmpty(readString(row, columnIndex))) {
                return false;
            }
        }
        return true;
    }

    private static String readString(List<Cell> row, int columnIndex) {
        Cell cell = DefectCustomFieldExcelSupport.cellAt(row, columnIndex);
        if (cell == null || cell.getType() == null) {
            return null;
        }
        CellType cellType = cell.getType();
        if (cellType == CellType.STRING) {
            return trimToNull(cell.getText());
        }
        if (cellType == CellType.NUMBER) {
            if (DefectCustomFieldExcelSupport.isDateFormatted(cell)) {
                LocalDateTime date = cell.asDate();
                return DATE_TIME_FORMATTER.format(date);
            }
            double numeric = cell.asNumber().doubleValue();
            if (numeric == Math.rint(numeric)) {
                return String.valueOf((long) numeric);
            }
            return String.valueOf(numeric);
        }
        if (cellType == CellType.BOOLEAN) {
            return String.valueOf(cell.asBoolean());
        }
        if (cellType == CellType.FORMULA) {
            // 仅接受字符串结果的公式；数值/布尔结果的公式视为整份导入解析失败
            if (!(cell.getValue() instanceof String text)) {
                throw new IllegalStateException("Cannot get a STRING value from a non-string formula cell");
            }
            return trimToNull(text);
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录");
        }
        return loginUser;
    }
}
