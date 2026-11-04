package com.cat2bug.system.service;

import com.cat2bug.common.domain.ProjectCopyJob;
import com.cat2bug.system.mapper.ProjectCopyJobMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * 复制任务的短事务。异步线程通过本 Bean 提交，进度查询能立刻看到已完成的分项。
 */
@ApplicationScoped
public class ProjectCopyJobStore {

    @Inject
    ProjectCopyJobMapper projectCopyJobMapper;

    @Inject
    SysProjectMapper sysProjectMapper;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void insert(ProjectCopyJob job) {
        projectCopyJobMapper.insert(job);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void update(ProjectCopyJob job) {
        job.setUpdateTime(new java.util.Date());
        projectCopyJobMapper.update(job);
    }

    public ProjectCopyJob find(String jobId) {
        return projectCopyJobMapper.selectByJobId(jobId);
    }

    /** 每块复制单独提交，失败只回滚这一块。 */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void run(Runnable work) {
        work.run();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateUsedBytes(Long projectId, long usedBytes) {
        sysProjectMapper.updateUsedBytes(projectId, usedBytes);
    }
}
