package com.cat2bug.quarkus.admin.service;

import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.excel.ExcelSheetTable;
import com.cat2bug.quarkus.common.excel.FastExcelSupport;
import com.cat2bug.quarkus.common.util.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@ApplicationScoped
public class UserExcelImportService {

    private static final Pattern DECIMAL_NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");

    public List<SysUser> parseWorkbook(InputStream input) {
        List<UserExcelColumnSupport.ColumnDef> templateColumns = UserExcelColumnSupport.templateColumns();
        ExcelSheetTable table;
        try {
            table = FastExcelSupport.readFirstSheet(input);
        } catch (IOException e) {
            throw new ApiException("解析用户 Excel 失败");
        }
        if (table.rowCount() == 0) {
            throw new ApiException("导入用户数据不能为空！");
        }
        Map<Integer, UserExcelColumnSupport.ColumnDef> columnByIndex = new HashMap<>();
        for (int colIndex = 0; colIndex < table.columnCount(0); colIndex++) {
            UserExcelColumnSupport.ColumnDef column =
                    UserExcelColumnSupport.findByHeader(cellText(table, 0, colIndex), templateColumns);
            if (column != null) {
                columnByIndex.put(colIndex, column);
            }
        }
        if (columnByIndex.isEmpty()) {
            throw new ApiException("导入用户数据不能为空！");
        }
        List<SysUser> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < table.rowCount(); rowIndex++) {
            if (isBlankRow(table, rowIndex, columnByIndex)) {
                continue;
            }
            SysUser user = new SysUser();
            for (Map.Entry<Integer, UserExcelColumnSupport.ColumnDef> entry : columnByIndex.entrySet()) {
                UserExcelColumnSupport.applyImportValue(user, entry.getValue(),
                        cellText(table, rowIndex, entry.getKey()));
            }
            rows.add(user);
        }
        return rows;
    }

    private boolean isBlankRow(ExcelSheetTable table, int rowIndex,
                               Map<Integer, UserExcelColumnSupport.ColumnDef> columnByIndex) {
        for (Integer colIndex : columnByIndex.keySet()) {
            if (StringUtils.isNotBlank(cellText(table, rowIndex, colIndex))) {
                return false;
            }
        }
        return true;
    }

    private static String cellText(ExcelSheetTable table, int rowIndex, int colIndex) {
        return normalizeNumericText(table.cell(rowIndex, colIndex));
    }

    /**
     * FastExcel 返回数值单元格的原始文本（如 "1.0"、"1.3800138E10"），按 POI DataFormatter 的
     * 常规格式还原为 "1"、"13800138000"；非数值文本原样返回。
     */
    static String normalizeNumericText(String text) {
        if (text == null) {
            return "";
        }
        boolean decimalOrExponent = text.indexOf('.') >= 0 || text.indexOf('e') >= 0 || text.indexOf('E') >= 0;
        if (!decimalOrExponent || !DECIMAL_NUMBER.matcher(text).matches()) {
            return text;
        }
        BigDecimal number = new BigDecimal(text).stripTrailingZeros();
        boolean exponent = text.indexOf('e') >= 0 || text.indexOf('E') >= 0;
        if (number.scale() <= 0 || exponent) {
            return number.toPlainString();
        }
        return text;
    }
}
