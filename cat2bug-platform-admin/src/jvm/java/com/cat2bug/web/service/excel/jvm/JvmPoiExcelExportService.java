package com.cat2bug.web.service.excel.jvm;

import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.web.service.excel.MonitorExcelExportService;
import com.cat2bug.system.domain.SysLogininfor;
import com.cat2bug.system.domain.SysOperLog;
import com.cat2bug.system.domain.SysScreenSize;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JVM 专用 POI Excel 导出（Native profile 不加载）。
 */
@Profile("!native")
@Component
public class JvmPoiExcelExportService implements MonitorExcelExportService
{
    public void exportOperLogs(HttpServletResponse response, List<SysOperLog> list)
    {
        ExcelUtil<SysOperLog> util = new ExcelUtil<>(SysOperLog.class);
        util.exportExcel(response, list, "操作日志");
    }

    public void exportLogininfor(HttpServletResponse response, List<SysLogininfor> list)
    {
        ExcelUtil<SysLogininfor> util = new ExcelUtil<>(SysLogininfor.class);
        util.exportExcel(response, list, "登录日志");
    }

    public void exportScreenSizes(HttpServletResponse response, List<SysScreenSize> list)
    {
        ExcelUtil<SysScreenSize> util = new ExcelUtil<>(SysScreenSize.class);
        util.exportExcel(response, list, "屏幕尺寸数据");
    }
}
