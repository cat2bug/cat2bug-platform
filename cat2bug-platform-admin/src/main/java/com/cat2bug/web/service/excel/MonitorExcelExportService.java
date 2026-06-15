package com.cat2bug.web.service.excel;

import com.cat2bug.system.domain.SysLogininfor;
import com.cat2bug.system.domain.SysOperLog;
import com.cat2bug.system.domain.SysScreenSize;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 监控/系统类 POI Excel 导出 SPI（JVM 实现；Native 无 bean）。
 */
public interface MonitorExcelExportService
{
    void exportOperLogs(HttpServletResponse response, List<SysOperLog> list);

    void exportLogininfor(HttpServletResponse response, List<SysLogininfor> list);

    void exportScreenSizes(HttpServletResponse response, List<SysScreenSize> list);
}
