package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.web.excel.ExcelHeaderCell;
import com.cat2bug.web.excel.ExcelHeaderStyle;
import com.cat2bug.web.excel.ExcelTableModel;
import com.cat2bug.web.excel.ExcelTableWriter;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.service.ISysUserService;
import java.util.List;@Service
public class UserExcelExportService {@Autowired
    ISysUserService sysUserService;@Autowired
    ExcelTableWriter excelTableWriter;public byte[] buildExportWorkbook(SysUser query) {
        List<SysUser> users = sysUserService.selectUserList(query);
        List<UserExcelColumnSupport.ColumnDef> columns = UserExcelColumnSupport.exportColumns();
        List<ExcelHeaderCell> headers = columns.stream()
                .map(column -> new ExcelHeaderCell(column.header(), ExcelHeaderStyle.DARK_BAR))
                .toList();
        List<List<String>> rows = users.stream()
                .map(user -> columns.stream()
                        .map(column -> UserExcelColumnSupport.formatExportValue(user, column))
                        .toList())
                .toList();
        return excelTableWriter.write(
                ExcelTableModel.withHeaders(UserExcelColumnSupport.EXPORT_SHEET_NAME, headers, rows, 20),
                "导出用户数据失败");
    }
}
