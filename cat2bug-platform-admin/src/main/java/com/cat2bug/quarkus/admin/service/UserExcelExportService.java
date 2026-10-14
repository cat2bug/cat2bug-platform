package com.cat2bug.quarkus.admin.service;

import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.common.excel.ExcelTableModel;
import com.cat2bug.quarkus.system.service.SysUserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class UserExcelExportService {

    @Inject
    SysUserService sysUserService;

    public byte[] buildExportWorkbook(SysUser query) {
        List<SysUser> users = sysUserService.selectUserListAll(query);
        List<UserExcelColumnSupport.ColumnDef> columns = UserExcelColumnSupport.exportColumns();
        List<String> headers = columns.stream().map(UserExcelColumnSupport.ColumnDef::header).toList();
        List<List<String>> rows = new ArrayList<>(users.size());
        for (SysUser user : users) {
            List<String> line = new ArrayList<>(columns.size());
            for (UserExcelColumnSupport.ColumnDef column : columns) {
                line.add(UserExcelColumnSupport.formatExportValue(user, column));
            }
            rows.add(line);
        }
        ExcelTableModel model = ExcelTableModel.simple(UserExcelColumnSupport.EXPORT_SHEET_NAME, headers, rows);
        return SimpleExcelExportSupport.write(model, col -> SimpleExcelExportSupport.DEFAULT_COLUMN_WIDTH,
                "导出用户数据失败");
    }
}
