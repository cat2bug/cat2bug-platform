package com.cat2bug.admin.service;

import com.cat2bug.common.excel.ExcelTableModel;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class UserImportTemplateService {

    public byte[] buildTemplateWorkbook() {
        List<UserExcelColumnSupport.ColumnDef> columns = UserExcelColumnSupport.templateColumns();
        List<String> headers = columns.stream().map(UserExcelColumnSupport.ColumnDef::header).toList();
        ExcelTableModel model = ExcelTableModel.simple(UserExcelColumnSupport.TEMPLATE_SHEET_NAME, headers, List.of());
        return SimpleExcelExportSupport.write(model, col -> SimpleExcelExportSupport.DEFAULT_COLUMN_WIDTH,
                "生成用户导入模版失败");
    }
}
