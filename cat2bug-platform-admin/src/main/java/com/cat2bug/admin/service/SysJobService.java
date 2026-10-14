package com.cat2bug.admin.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysJob;
import com.cat2bug.common.domain.SysJobLog;
import com.cat2bug.system.mapper.SysJobLogMapper;
import com.cat2bug.system.mapper.SysJobMapper;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 定时任务：sys_job 持久化 + quarkus-scheduler 内存调度（与 Spring 版 Quartz RAMJobStore 等价）。
 * 计划策略（misfire_policy）只做存取：内存调度器不补跑错过的触发，等同「放弃执行」。
 */
@ApplicationScoped
public class SysJobService {

    private static final Logger LOG = Logger.getLogger(SysJobService.class);

    public static final String STATUS_NORMAL = "0";
    public static final String STATUS_PAUSE = "1";
    private static final String TASK_ID_PREFIX = "TASK_CLASS_NAME";
    private static final CronParser CRON_PARSER =
            new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    @Inject
    Scheduler scheduler;

    @Inject
    SysJobMapper jobMapper;

    @Inject
    SysJobLogMapper jobLogMapper;

    void onStart(@Observes StartupEvent event) {
        reloadAll();
    }

    /** 启动或安装完成后，按数据库重建全部调度。 */
    public void reloadAll() {
        try {
            for (SysJob job : jobMapper.selectJobAll()) {
                schedule(job);
            }
        } catch (RuntimeException e) {
            LOG.warnf("定时任务未加载（数据库可能尚未初始化）: %s", e.getMessage());
        }
    }

    public TableDataInfo selectJobPage(SysJob query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = jobMapper.countJobList(query);
        List<SysJob> rows = jobMapper.selectJobList(query, page.getOffset(), page.getPageSize());
        rows.forEach(SysJobService::fillNextValidTime);
        return new TableDataInfo(rows, total);
    }

    public List<SysJob> selectJobListAll(SysJob query) {
        List<SysJob> list = jobMapper.selectJobListAll(query);
        return list != null ? list : List.of();
    }

    public SysJob selectJobById(Long jobId) {
        SysJob job = jobMapper.selectJobById(jobId);
        fillNextValidTime(job);
        return job;
    }

    public int insertJob(SysJob job) {
        job.setStatus(STATUS_PAUSE);
        int rows = jobMapper.insertJob(job);
        if (rows > 0) {
            schedule(job);
        }
        return rows;
    }

    public int updateJob(SysJob job) {
        SysJob previous = jobMapper.selectJobById(job.getJobId());
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            if (previous != null) {
                scheduler.unscheduleJob(taskId(previous));
            }
            SysJob saved = jobMapper.selectJobById(job.getJobId());
            if (saved != null) {
                schedule(saved);
            }
        }
        return rows;
    }

    public int changeStatus(Long jobId, String status) {
        SysJob job = jobMapper.selectJobById(jobId);
        if (job == null || !(STATUS_NORMAL.equals(status) || STATUS_PAUSE.equals(status))) {
            return 0;
        }
        job.setStatus(status);
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            if (STATUS_PAUSE.equals(status)) {
                scheduler.pause(taskId(job));
            } else {
                scheduler.resume(taskId(job));
            }
        }
        return rows;
    }

    /** 立即异步执行一次；任务不存在或 cron 已无下次触发时返回 false。 */
    public boolean run(Long jobId) {
        SysJob job = jobMapper.selectJobById(jobId);
        if (job == null || scheduler.getScheduledJob(taskId(job)) == null) {
            return false;
        }
        Infrastructure.getDefaultWorkerPool().execute(() -> execute(job));
        return true;
    }

    public void deleteJobByIds(Long[] jobIds) {
        for (Long jobId : jobIds) {
            SysJob job = jobMapper.selectJobById(jobId);
            if (job != null && jobMapper.deleteJobById(jobId) > 0) {
                scheduler.unscheduleJob(taskId(job));
            }
        }
    }

    public TableDataInfo selectJobLogPage(SysJobLog query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = jobLogMapper.countJobLogList(query);
        return new TableDataInfo(jobLogMapper.selectJobLogList(query, page.getOffset(), page.getPageSize()), total);
    }

    public List<SysJobLog> selectJobLogListAll(SysJobLog query) {
        List<SysJobLog> list = jobLogMapper.selectJobLogListAll(query);
        return list != null ? list : List.of();
    }

    public SysJobLog selectJobLogById(Long jobLogId) {
        return jobLogMapper.selectJobLogById(jobLogId);
    }

    public int deleteJobLogByIds(Long[] ids) {
        return jobLogMapper.deleteJobLogByIds(ids);
    }

    public void cleanJobLog() {
        jobLogMapper.cleanJobLog();
    }

    public static boolean isValidCron(String expression) {
        if (expression == null || expression.isBlank()) {
            return false;
        }
        try {
            CRON_PARSER.parse(expression).validate();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    static Optional<ZonedDateTime> nextExecution(String expression) {
        if (!isValidCron(expression)) {
            return Optional.empty();
        }
        return ExecutionTime.forCron(CRON_PARSER.parse(expression)).nextExecution(ZonedDateTime.now());
    }

    private static void fillNextValidTime(SysJob job) {
        if (job != null) {
            job.setNextValidTime(nextExecution(job.getCronExpression())
                    .map(t -> Date.from(t.toInstant())).orElse(null));
        }
    }

    private void schedule(SysJob job) {
        String id = taskId(job);
        if (scheduler.getScheduledJob(id) != null) {
            scheduler.unscheduleJob(id);
        }
        if (nextExecution(job.getCronExpression()).isEmpty()) {
            return;
        }
        SysJob snapshot = copy(job);
        scheduler.newJob(id)
                .setCron(job.getCronExpression())
                .setConcurrentExecution("0".equals(job.getConcurrent())
                        ? Scheduled.ConcurrentExecution.PROCEED
                        : Scheduled.ConcurrentExecution.SKIP)
                .setTask(execution -> execute(snapshot))
                .schedule();
        if (STATUS_PAUSE.equals(job.getStatus())) {
            scheduler.pause(id);
        }
    }

    void execute(SysJob job) {
        long start = System.currentTimeMillis();
        Throwable error = null;
        try {
            JobInvoker.invoke(job.getInvokeTarget());
        } catch (InvocationTargetException e) {
            error = e.getTargetException() != null ? e.getTargetException() : e;
        } catch (Exception | LinkageError e) {
            error = e;
        }
        if (error != null) {
            LOG.errorf(error, "任务执行异常 - %s", job.getJobName());
        }
        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setInvokeTarget(job.getInvokeTarget());
        jobLog.setStartTime(new Date(start));
        jobLog.setStopTime(new Date());
        jobLog.setJobMessage(job.getJobName() + " 总共耗时：" + (System.currentTimeMillis() - start) + "毫秒");
        jobLog.setStatus(error == null ? "0" : "1");
        if (error != null) {
            String trace = stackTrace(error);
            jobLog.setExceptionInfo(trace.length() > 2000 ? trace.substring(0, 2000) : trace);
        }
        try {
            jobLogMapper.insertJobLog(jobLog);
        } catch (RuntimeException e) {
            LOG.warnf("写入任务日志失败 %s: %s", job.getJobName(), e.getMessage());
        }
    }

    private static String taskId(SysJob job) {
        return TASK_ID_PREFIX + job.getJobId() + "." + job.getJobGroup();
    }

    private static String stackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }

    private static SysJob copy(SysJob job) {
        SysJob c = new SysJob();
        c.setJobId(job.getJobId());
        c.setJobName(job.getJobName());
        c.setJobGroup(job.getJobGroup());
        c.setInvokeTarget(job.getInvokeTarget());
        c.setCronExpression(job.getCronExpression());
        c.setConcurrent(job.getConcurrent());
        c.setStatus(job.getStatus());
        return c;
    }
}
