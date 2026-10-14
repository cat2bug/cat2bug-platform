package com.cat2bug.admin.service;

import com.cat2bug.common.domain.SysJob;
import com.cat2bug.common.domain.SysJobLog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

/**
 * 定时任务 / 调度日志导出，列与 Spring {@code @Excel} 注解一致。
 */
@ApplicationScoped
public class JobExcelExportService {

    private static final Map<String, String> MISFIRE = Map.of(
            "0", "默认", "1", "立即触发执行", "2", "触发一次执行", "3", "不触发立即执行");
    private static final Map<String, String> CONCURRENT = Map.of("0", "允许", "1", "禁止");
    private static final Map<String, String> JOB_STATUS = Map.of("0", "正常", "1", "暂停");
    private static final Map<String, String> LOG_STATUS = Map.of("0", "正常", "1", "失败");

    private static final List<SimpleExcelExportSupport.ColumnDef<SysJob>> JOB_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("任务序号", j -> SimpleExcelExportSupport.text(j.getJobId())),
            new SimpleExcelExportSupport.ColumnDef<>("任务名称", SysJob::getJobName),
            new SimpleExcelExportSupport.ColumnDef<>("任务组名", SysJob::getJobGroup),
            new SimpleExcelExportSupport.ColumnDef<>("调用目标字符串", SysJob::getInvokeTarget),
            new SimpleExcelExportSupport.ColumnDef<>("执行表达式 ", SysJob::getCronExpression),
            new SimpleExcelExportSupport.ColumnDef<>("计划策略 ", j -> label(MISFIRE, j.getMisfirePolicy())),
            new SimpleExcelExportSupport.ColumnDef<>("并发执行", j -> label(CONCURRENT, j.getConcurrent())),
            new SimpleExcelExportSupport.ColumnDef<>("任务状态", j -> label(JOB_STATUS, j.getStatus()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysJobLog>> JOB_LOG_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("日志序号", l -> SimpleExcelExportSupport.text(l.getJobLogId())),
            new SimpleExcelExportSupport.ColumnDef<>("任务名称", SysJobLog::getJobName),
            new SimpleExcelExportSupport.ColumnDef<>("任务组名", SysJobLog::getJobGroup),
            new SimpleExcelExportSupport.ColumnDef<>("调用目标字符串", SysJobLog::getInvokeTarget),
            new SimpleExcelExportSupport.ColumnDef<>("日志信息", SysJobLog::getJobMessage),
            new SimpleExcelExportSupport.ColumnDef<>("执行状态", l -> label(LOG_STATUS, l.getStatus())),
            new SimpleExcelExportSupport.ColumnDef<>("异常信息", SysJobLog::getExceptionInfo)
    );

    @Inject
    SysJobService sysJobService;

    public byte[] exportJobs(SysJob query) {
        return SimpleExcelExportSupport.export("定时任务", JOB_COLUMNS,
                sysJobService.selectJobListAll(query), "导出定时任务失败");
    }

    public byte[] exportJobLogs(SysJobLog query) {
        return SimpleExcelExportSupport.export("调度日志", JOB_LOG_COLUMNS,
                sysJobService.selectJobLogListAll(query), "导出调度日志失败");
    }

    private static String label(Map<String, String> labels, String value) {
        return value == null ? "" : labels.getOrDefault(value, value);
    }
}
