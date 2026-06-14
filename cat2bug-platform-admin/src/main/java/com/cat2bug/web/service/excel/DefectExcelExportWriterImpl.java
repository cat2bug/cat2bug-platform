package com.cat2bug.web.service.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import com.cat2bug.web.excel.ExcelHeaderCell;
import com.cat2bug.web.excel.ExcelHeaderStyle;
import com.cat2bug.web.excel.ExcelTableModel;
import com.cat2bug.web.excel.ExcelTableWriter;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysDictData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;@Service
public class DefectExcelExportWriterImpl implements DefectExcelExportWriter {@Autowired
    ExcelTableWriter excelTableWriter;@Override
    public byte[] writeWorkbook(List<SysDefect> defects, List<DefectExcelColumnSupport.ColumnDef> columns,
                                List<SysDictData> levelDict) {
        List<ExcelHeaderCell> headers = columns.stream()
                .map(column -> new ExcelHeaderCell(column.header(),
                        column.required() ? ExcelHeaderStyle.REQUIRED : ExcelHeaderStyle.NORMAL))
                .toList();
        List<List<String>> rows = defects.stream()
                .map(defect -> columns.stream()
                        .map(column -> DefectExcelColumnSupport.formatExportValue(defect, column, levelDict))
                        .toList())
                .toList();
        Map<Integer, Integer> columnWidths = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            columnWidths.put(i, "defectDescribe".equals(columns.get(i).fieldName()) ? 40 : 20);
        }
        ExcelTableModel model = new ExcelTableModel(
                DefectExcelColumnSupport.SHEET_NAME, headers, rows, 20, columnWidths, java.util.Set.of());
        return excelTableWriter.write(model, "导出缺陷数据失败");
    }
}
