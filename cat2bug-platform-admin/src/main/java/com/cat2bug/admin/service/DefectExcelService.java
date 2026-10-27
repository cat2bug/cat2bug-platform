package com.cat2bug.admin.service;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysDictData;
import com.cat2bug.common.domain.SysModule;
import com.cat2bug.common.domain.SysProjectDefectField;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.excel.ExcelColumnExportSupport;
import com.cat2bug.common.excel.ExcelDrawingImageReader;
import com.cat2bug.common.excel.ExcelHeaderCell;
import com.cat2bug.common.excel.ExcelHeaderStyle;
import com.cat2bug.common.excel.ExcelImageEmbedSupport;
import com.cat2bug.common.excel.FastExcelSupport;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.storage.FileUploadService;
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
import java.util.LinkedHashMap;
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

    @Inject
    FileUploadService fileUploadService;

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

        String assetHost = params.get("host") != null ? String.valueOf(params.get("host")).trim() : null;
        return FastExcelSupport.writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet(DefectExcelColumnSupport.SHEET_NAME);
            FastExcelSupport.writeHeaders(sheet, headerCells(columns), 0);
            for (int rowIndex = 0; rowIndex < defects.size(); rowIndex++) {
                SysDefect defect = defects.get(rowIndex);
                int excelRow = rowIndex + 1;
                for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                    DefectExcelColumnSupport.ColumnDef column = columns.get(colIndex);
                    String value = DefectExcelColumnSupport.formatExportValue(defect, column, levelDict);
                    if (value == null || value.isEmpty()) {
                        continue;
                    }
                    if (DefectExcelColumnSupport.isImageColumn(column)
                            && ExcelImageEmbedSupport.embedInCell(sheet, excelRow, colIndex, value, assetHost)) {
                        continue;
                    }
                    sheet.value(excelRow, colIndex, value);
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
            List<Integer> defectSheetRows = new ArrayList<>();
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
                defectSheetRows.add(rowIndex);
            }

            DefectCustomFieldExcelSupport.mergeCustomFieldsFromImport(rows, defects, customFieldDefs);
            applyEmbeddedImages(fileBytes, defects, defectSheetRows, columnByIndex);
            return defects;
        } catch (IOException e) {
            throw new ApiException("解析导入文件失败");
        } catch (Exception e) {
            throw new ApiException("解析导入文件失败");
        }
    }

    /**
     * 将 xlsx 悬浮图落到 profile，并写入系统 imgUrls / 自定义 image 字段（单元格文本为空时也能导入）。
     */
    private void applyEmbeddedImages(byte[] fileBytes, List<SysDefect> defects, List<Integer> defectSheetRows,
                                     Map<Integer, DefectExcelColumnSupport.ColumnDef> columnByIndex) {
        if (defects == null || defects.isEmpty() || defectSheetRows == null || defectSheetRows.isEmpty()) {
            return;
        }
        Map<String, List<ExcelDrawingImageReader.EmbeddedImage>> byCell =
                ExcelDrawingImageReader.readByCell(fileBytes);
        if (byCell.isEmpty()) {
            return;
        }
        List<Map.Entry<Integer, DefectExcelColumnSupport.ColumnDef>> imageColumns = columnByIndex.entrySet().stream()
                .filter(e -> DefectExcelColumnSupport.isImageColumn(e.getValue())
                        || "imgObjects".equals(e.getValue().fieldName()))
                .toList();
        if (imageColumns.isEmpty()) {
            return;
        }
        int n = Math.min(defects.size(), defectSheetRows.size());
        for (int i = 0; i < n; i++) {
            SysDefect defect = defects.get(i);
            int sheetRow = defectSheetRows.get(i);
            for (Map.Entry<Integer, DefectExcelColumnSupport.ColumnDef> entry : imageColumns) {
                int col = entry.getKey();
                List<ExcelDrawingImageReader.EmbeddedImage> images = byCell.get(sheetRow + "_" + col);
                if (images == null || images.isEmpty()) {
                    continue;
                }
                List<String> urls = uploadEmbeddedImages(images);
                if (urls.isEmpty()) {
                    continue;
                }
                DefectExcelColumnSupport.ColumnDef column = entry.getValue();
                if (column.custom()) {
                    Map<String, Object> custom = defect.getCustomFields() != null
                            ? new LinkedHashMap<>(defect.getCustomFields()) : new LinkedHashMap<>();
                    Object existing = custom.get(column.customDef().getFieldKey());
                    custom.put(column.customDef().getFieldKey(), mergeImageUrls(existing, urls));
                    defect.setCustomFields(custom);
                } else {
                    defect.setImgUrls(mergeImageUrlString(defect.getImgUrls(), urls));
                }
            }
        }
    }

    private List<String> uploadEmbeddedImages(List<ExcelDrawingImageReader.EmbeddedImage> images) {
        List<String> urls = new ArrayList<>();
        for (ExcelDrawingImageReader.EmbeddedImage image : images) {
            if (image == null || image.data() == null || image.data().length == 0) {
                continue;
            }
            String fileName = StringUtils.isNotEmpty(image.fileName()) ? image.fileName() : "import.png";
            try (InputStream in = new ByteArrayInputStream(image.data())) {
                var result = fileUploadService.upload(in, image.data().length, fileName, null);
                if (result != null && StringUtils.isNotEmpty(result.getUrl())) {
                    urls.add(result.getUrl());
                }
            } catch (Exception ignored) {
                // 单张失败不阻断整行导入
            }
        }
        return urls;
    }

    private static List<String> mergeImageUrls(Object existing, List<String> uploaded) {
        List<String> merged = new ArrayList<>();
        if (existing instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && StringUtils.isNotEmpty(String.valueOf(item))) {
                    merged.add(String.valueOf(item).trim());
                }
            }
        } else if (existing != null && StringUtils.isNotEmpty(String.valueOf(existing))) {
            for (String part : String.valueOf(existing).split("[,，]")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    merged.add(t);
                }
            }
        }
        for (String url : uploaded) {
            if (StringUtils.isNotEmpty(url) && !merged.contains(url)) {
                merged.add(url);
            }
        }
        return merged;
    }

    private static String mergeImageUrlString(String existing, List<String> uploaded) {
        List<String> merged = mergeImageUrls(existing, uploaded);
        return String.join(",", merged);
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

    /** 与原 POI 列宽一致：图片列 50、描述列 10000、其余 5000（单位 1/256 字符）。 */
    private static double columnWidthChars(DefectExcelColumnSupport.ColumnDef column) {
        if (DefectExcelColumnSupport.isImageColumn(column)) {
            return ExcelImageEmbedSupport.IMAGE_COLUMN_WIDTH_CHARS;
        }
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
