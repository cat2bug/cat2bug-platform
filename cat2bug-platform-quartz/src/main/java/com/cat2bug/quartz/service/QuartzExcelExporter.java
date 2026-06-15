package com.cat2bug.quartz.service;

import com.cat2bug.quartz.domain.SysJob;
import com.cat2bug.quartz.domain.SysJobLog;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * Quartz 模块 Excel 导出 SPI（JVM 实现由 admin 模块提供）。
 */
public interface QuartzExcelExporter
{
    void exportJobs(HttpServletResponse response, List<SysJob> jobs);

    void exportJobLogs(HttpServletResponse response, List<SysJobLog> jobLogs);
}
