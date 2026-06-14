package com.cat2bug.web.service.excel;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysDictData;
import java.util.List;/**
 * 缺陷 Excel 导出写入器（B9：JVM 用 POI，Native 用 FastExcel）。
 */
public interface DefectExcelExportWriter {byte[] writeWorkbook(List<SysDefect> defects, List<DefectExcelColumnSupport.ColumnDef> columns,
                         List<SysDictData> levelDict);
}
