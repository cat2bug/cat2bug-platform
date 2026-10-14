package com.cat2bug.quarkus.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 定时任务调度表 sys_job
 */
public class SysJob extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long jobId;
    private String jobName;
    private String jobGroup;
    /** 调用目标字符串，如 {@code ryTask.ryParams('a')} 或全限定类名 + 方法。 */
    private String invokeTarget;
    private String cronExpression;
    /** 0 默认 1 立即触发执行 2 触发一次执行 3 不触发立即执行 */
    private String misfirePolicy = "0";
    /** 0 允许并发 1 禁止并发 */
    private String concurrent;
    /** 0 正常 1 暂停 */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextValidTime;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getInvokeTarget() {
        return invokeTarget;
    }

    public void setInvokeTarget(String invokeTarget) {
        this.invokeTarget = invokeTarget;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getMisfirePolicy() {
        return misfirePolicy;
    }

    public void setMisfirePolicy(String misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }

    public String getConcurrent() {
        return concurrent;
    }

    public void setConcurrent(String concurrent) {
        this.concurrent = concurrent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getNextValidTime() {
        return nextValidTime;
    }

    public void setNextValidTime(Date nextValidTime) {
        this.nextValidTime = nextValidTime;
    }
}
