package com.cat2bug.admin.service;

import com.cat2bug.common.domain.SysCloudPushJob;
import com.cat2bug.common.domain.SysCloudPushPart;
import com.cat2bug.system.mapper.SysCloudPushJobMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class CloudPushJobStore {

    @Inject
    SysCloudPushJobMapper mapper;

    @Transactional
    public void insertJobAndParts(SysCloudPushJob job, List<SysCloudPushPart> parts) {
        mapper.insertJob(job);
        if (parts != null) {
            for (SysCloudPushPart part : parts) {
                mapper.insertPart(part);
            }
        }
    }

    @Transactional
    public void updateJob(SysCloudPushJob job) {
        mapper.updateJob(job);
    }

    @Transactional
    public void updatePart(SysCloudPushPart part) {
        mapper.updatePart(part);
    }

    public SysCloudPushJob findJob(String jobId) {
        return mapper.selectJob(jobId);
    }

    public SysCloudPushJob findOpenByProject(Long projectId) {
        return mapper.selectOpenByProject(projectId);
    }

    public List<SysCloudPushJob> findRunning() {
        List<SysCloudPushJob> rows = mapper.selectRunning();
        return rows == null ? List.of() : rows;
    }

    public List<SysCloudPushPart> findParts(String jobId) {
        List<SysCloudPushPart> rows = mapper.selectParts(jobId);
        return rows == null ? List.of() : rows;
    }
}
