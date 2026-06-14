package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.web.excel.ExcelSheetReader;
import com.cat2bug.web.excel.ExcelSheetTable;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.service.ISysModuleService;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;/**
 * 用例 Excel 导入解析。
 */
@Service
public class CaseExcelImportService {@Autowired
    ISysModuleService sysModuleService;@Autowired
    ExcelSheetReader excelSheetReader;public List<SysCase> parseWorkbook(InputStream input, Long projectId, Map<String, Object> params, Locale locale) {
        if (params == null) {
            params = Map.of();
        }
        List<CaseExcelColumnSupport.ColumnDef> templateColumns =
                CaseExcelColumnSupport.resolveTemplateColumns(params, locale);
        Map<String, Long> modulePathToId = sysModuleService.selectSysModulePathList(projectId).stream()
                .collect(Collectors.toMap(SysModule::getModulePath, SysModule::getModuleId, (a, b) -> a));
        ExcelSheetTable table;
        try {
            table = excelSheetReader.readFirstSheet(input);
        } catch (IOException e) {
            throw new ServiceException("解析用例 Excel 失败");
        }
        if (table.rowCount() == 0) {
            throw new ServiceException("导入用例数据为空或格式不合法");
        }
        int headerRowIndex = table.findHeaderRowIndex();
        List<String> headerCells = table.headerRow(headerRowIndex);
        Map<Integer, CaseExcelColumnSupport.ColumnDef> columnByIndex = new HashMap<>();
        for (int colIndex = 0; colIndex < headerCells.size(); colIndex++) {
            CaseExcelColumnSupport.ColumnDef column =
                    CaseExcelColumnSupport.findByHeader(headerCells.get(colIndex), templateColumns);
            if (column != null) {
                columnByIndex.put(colIndex, column);
            }
        }
        if (columnByIndex.isEmpty()) {
            throw new ServiceException("导入用例数据为空或格式不合法");
        }
        List<SysCase> rows = new ArrayList<>();
        for (int rowIndex = headerRowIndex + 1; rowIndex < table.rowCount(); rowIndex++) {
            if (table.isRowEmpty(rowIndex)) {
                continue;
            }
            SysCase item = new SysCase();
            for (Map.Entry<Integer, CaseExcelColumnSupport.ColumnDef> entry : columnByIndex.entrySet()) {
                String value = table.cellText(rowIndex, entry.getKey());
                CaseExcelColumnSupport.applyImportValue(item, entry.getValue(), value, modulePathToId);
            }
            rows.add(item);
        }
        return rows;
    }
}
