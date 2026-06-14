package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.web.excel.ExcelHeaderCell;
import com.cat2bug.web.excel.ExcelHeaderStyle;
import com.cat2bug.web.excel.ExcelTableModel;
import com.cat2bug.web.excel.ExcelTableWriter;
import java.util.List;@Service
public class UserImportTemplateService {@Autowired
    ExcelTableWriter excelTableWriter;public byte[] buildTemplateWorkbook() {
        List<UserExcelColumnSupport.ColumnDef> columns = UserExcelColumnSupport.templateColumns();
        List<ExcelHeaderCell> headers = columns.stream()
                .map(column -> new ExcelHeaderCell(column.header(), ExcelHeaderStyle.DARK_BAR))
                .toList();
        return excelTableWriter.write(
                ExcelTableModel.emptyHeaders(UserExcelColumnSupport.TEMPLATE_SHEET_NAME, headers, 20),
                "生成用户导入模版失败");
    }
}
