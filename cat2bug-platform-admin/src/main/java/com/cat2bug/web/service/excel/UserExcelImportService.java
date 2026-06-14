package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.web.excel.ExcelSheetReader;
import com.cat2bug.web.excel.ExcelSheetTable;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;@Service
public class UserExcelImportService {@Autowired
    ExcelSheetReader excelSheetReader;public List<SysUser> parseWorkbook(InputStream input) {
        List<UserExcelColumnSupport.ColumnDef> templateColumns = UserExcelColumnSupport.templateColumns();
        ExcelSheetTable table;
        try {
            table = excelSheetReader.readFirstSheet(input);
        } catch (IOException e) {
            throw new ServiceException("解析用户 Excel 失败");
        }
        if (table.rowCount() == 0) {
            throw new ServiceException("导入用户数据不能为空！");
        }
        int headerRowIndex = table.findHeaderRowIndex();
        List<String> headerCells = table.headerRow(headerRowIndex);
        Map<Integer, UserExcelColumnSupport.ColumnDef> columnByIndex = new HashMap<>();
        for (int colIndex = 0; colIndex < headerCells.size(); colIndex++) {
            UserExcelColumnSupport.ColumnDef column =
                    UserExcelColumnSupport.findByHeader(headerCells.get(colIndex), templateColumns);
            if (column != null) {
                columnByIndex.put(colIndex, column);
            }
        }
        if (columnByIndex.isEmpty()) {
            throw new ServiceException("导入用户数据不能为空！");
        }
        List<SysUser> rows = new ArrayList<>();
        for (int rowIndex = headerRowIndex + 1; rowIndex < table.rowCount(); rowIndex++) {
            if (table.isRowEmpty(rowIndex)) {
                continue;
            }
            SysUser user = new SysUser();
            for (Map.Entry<Integer, UserExcelColumnSupport.ColumnDef> entry : columnByIndex.entrySet()) {
                String value = table.cellText(rowIndex, entry.getKey());
                UserExcelColumnSupport.applyImportValue(user, entry.getValue(), value);
            }
            rows.add(user);
        }
        return rows;
    }
}
