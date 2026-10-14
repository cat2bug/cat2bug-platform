package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.JobExcelExportService;
import com.cat2bug.quarkus.admin.service.JobInvoker;
import com.cat2bug.quarkus.admin.service.SysJobService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.SysJob;
import com.cat2bug.quarkus.common.domain.SysJobLog;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.framework.security.SecurityUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.util.Arrays;

/**
 * 定时任务（/monitor/job）与调度日志（/monitor/jobLog）。
 */
@Path("/monitor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysJobService sysJobService;

    @Inject
    JobExcelExportService jobExcelExportService;

    @GET
    @Path("/job/list")
    @RequiresPermissions("monitor:job:list")
    public TableDataInfo list(@QueryParam("jobName") String jobName,
                              @QueryParam("jobGroup") String jobGroup,
                              @QueryParam("status") String status,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        return sysJobService.selectJobPage(jobQuery(jobName, jobGroup, status), pageNum, pageSize);
    }

    @POST
    @Path("/job/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("monitor:job:export")
    public Response exportForm(@RestForm("jobName") String jobName,
                               @RestForm("jobGroup") String jobGroup,
                               @RestForm("status") String status) {
        return spreadsheet(jobExcelExportService.exportJobs(jobQuery(jobName, jobGroup, status)));
    }

    @POST
    @Path("/job/export")
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("monitor:job:export")
    public Response exportJson(SysJob query) {
        return spreadsheet(jobExcelExportService.exportJobs(query != null ? query : new SysJob()));
    }

    @GET
    @Path("/job/{jobId}")
    @RequiresPermissions("monitor:job:query")
    public AjaxResult getInfo(@PathParam("jobId") Long jobId) {
        return AjaxResult.success(sysJobService.selectJobById(jobId));
    }

    @POST
    @Path("/job")
    @RequiresPermissions("monitor:job:add")
    public AjaxResult add(SysJob job) {
        String error = validate(job, "新增");
        if (error != null) {
            return AjaxResult.error(error);
        }
        job.setCreateBy(SecurityUtils.getUsername());
        return toAjax(sysJobService.insertJob(job));
    }

    @PUT
    @Path("/job")
    @RequiresPermissions("monitor:job:edit")
    public AjaxResult edit(SysJob job) {
        String error = validate(job, "修改");
        if (error != null) {
            return AjaxResult.error(error);
        }
        job.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(sysJobService.updateJob(job));
    }

    @PUT
    @Path("/job/changeStatus")
    @RequiresPermissions("monitor:job:changeStatus")
    public AjaxResult changeStatus(SysJob job) {
        return toAjax(sysJobService.changeStatus(job.getJobId(), job.getStatus()));
    }

    @PUT
    @Path("/job/run")
    @RequiresPermissions("monitor:job:changeStatus")
    public AjaxResult run(SysJob job) {
        return sysJobService.run(job.getJobId()) ? AjaxResult.success() : AjaxResult.error("任务不存在或已过期！");
    }

    @DELETE
    @Path("/job/{jobIds}")
    @RequiresPermissions("monitor:job:remove")
    public AjaxResult remove(@PathParam("jobIds") String jobIds) {
        sysJobService.deleteJobByIds(parseIds(jobIds));
        return AjaxResult.success();
    }

    @GET
    @Path("/jobLog/list")
    @RequiresPermissions("monitor:job:list")
    public TableDataInfo logList(@QueryParam("jobName") String jobName,
                                 @QueryParam("jobGroup") String jobGroup,
                                 @QueryParam("status") String status,
                                 @QueryParam("params[beginTime]") String beginTime,
                                 @QueryParam("params[endTime]") String endTime,
                                 @QueryParam("pageNum") Integer pageNum,
                                 @QueryParam("pageSize") Integer pageSize) {
        return sysJobService.selectJobLogPage(logQuery(jobName, jobGroup, status, beginTime, endTime),
                pageNum, pageSize);
    }

    @POST
    @Path("/jobLog/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("monitor:job:export")
    public Response exportLogForm(@RestForm("jobName") String jobName,
                                  @RestForm("jobGroup") String jobGroup,
                                  @RestForm("status") String status,
                                  @RestForm("params[beginTime]") String beginTime,
                                  @RestForm("params[endTime]") String endTime) {
        return spreadsheet(jobExcelExportService.exportJobLogs(
                logQuery(jobName, jobGroup, status, beginTime, endTime)));
    }

    @POST
    @Path("/jobLog/export")
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("monitor:job:export")
    public Response exportLogJson(SysJobLog query) {
        return spreadsheet(jobExcelExportService.exportJobLogs(query != null ? query : new SysJobLog()));
    }

    @GET
    @Path("/jobLog/{jobLogId}")
    @RequiresPermissions("monitor:job:query")
    public AjaxResult logInfo(@PathParam("jobLogId") Long jobLogId) {
        return AjaxResult.success(sysJobService.selectJobLogById(jobLogId));
    }

    @DELETE
    @Path("/jobLog/clean")
    @RequiresPermissions("monitor:job:remove")
    public AjaxResult cleanLog() {
        sysJobService.cleanJobLog();
        return AjaxResult.success();
    }

    @DELETE
    @Path("/jobLog/{jobLogIds}")
    @RequiresPermissions("monitor:job:remove")
    public AjaxResult removeLog(@PathParam("jobLogIds") String jobLogIds) {
        return toAjax(sysJobService.deleteJobLogByIds(parseIds(jobLogIds)));
    }

    private static String validate(SysJob job, String action) {
        String prefix = action + "任务'" + job.getJobName() + "'失败，";
        String target = job.getInvokeTarget();
        if (!SysJobService.isValidCron(job.getCronExpression())) {
            return prefix + "Cron表达式不正确";
        }
        if (target == null || target.isBlank()) {
            return prefix + "目标字符串不在白名单内";
        }
        if (target.toLowerCase().contains("rmi:")) {
            return prefix + "目标字符串不允许'rmi'调用";
        }
        if (target.toLowerCase().contains("ldap:") || target.toLowerCase().contains("ldaps:")) {
            return prefix + "目标字符串不允许'ldap(s)'调用";
        }
        if (target.toLowerCase().contains("http://") || target.toLowerCase().contains("https://")) {
            return prefix + "目标字符串不允许'http(s)'调用";
        }
        if (JobInvoker.containsErrorString(target)) {
            return prefix + "目标字符串存在违规";
        }
        if (!JobInvoker.whiteList(target)) {
            return prefix + "目标字符串不在白名单内";
        }
        return null;
    }

    private static SysJob jobQuery(String jobName, String jobGroup, String status) {
        SysJob query = new SysJob();
        query.setJobName(jobName);
        query.setJobGroup(jobGroup);
        query.setStatus(status);
        return query;
    }

    private static SysJobLog logQuery(String jobName, String jobGroup, String status,
                                      String beginTime, String endTime) {
        SysJobLog query = new SysJobLog();
        query.setJobName(jobName);
        query.setJobGroup(jobGroup);
        query.setStatus(status);
        if (beginTime != null) {
            query.getParams().put("beginTime", beginTime);
        }
        if (endTime != null) {
            query.getParams().put("endTime", endTime);
        }
        return query;
    }

    private static Long[] parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toArray(Long[]::new);
    }

    private static Response spreadsheet(byte[] workbook) {
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    private static AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
