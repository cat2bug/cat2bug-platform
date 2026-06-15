package com.cat2bug.web.service.excel.jvm;

import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.quartz.domain.SysJob;
import com.cat2bug.quartz.domain.SysJobLog;
import com.cat2bug.quartz.service.QuartzExcelExporter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("!native")
@Component
public class JvmQuartzExcelExporter implements QuartzExcelExporter
{
    @Override
    public void exportJobs(HttpServletResponse response, List<SysJob> jobs)
    {
        ExcelUtil<SysJob> util = new ExcelUtil<>(SysJob.class);
        util.exportExcel(response, jobs, "定时任务");
    }

    @Override
    public void exportJobLogs(HttpServletResponse response, List<SysJobLog> jobLogs)
    {
        ExcelUtil<SysJobLog> util = new ExcelUtil<>(SysJobLog.class);
        util.exportExcel(response, jobLogs, "调度日志");
    }
}
