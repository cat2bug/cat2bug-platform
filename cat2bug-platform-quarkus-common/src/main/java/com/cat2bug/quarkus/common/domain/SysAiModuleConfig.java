package com.cat2bug.quarkus.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * AI 模型配置 sys_ai_module_config
 */
public class SysAiModuleConfig {

    private Long aiId;
    private Long projectId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private Long createById;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private Long updateById;

    public Long getAiId() {
        return aiId;
    }

    public void setAiId(Long aiId) {
        this.aiId = aiId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getCreateById() {
        return createById;
    }

    public void setCreateById(Long createById) {
        this.createById = createById;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Long getUpdateById() {
        return updateById;
    }

    public void setUpdateById(Long updateById) {
        this.updateById = updateById;
    }
}
