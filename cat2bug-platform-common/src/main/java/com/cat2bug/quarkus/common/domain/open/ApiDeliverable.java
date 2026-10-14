package com.cat2bug.quarkus.common.domain.open;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Open API 交付物视图，对齐 Spring {@code com.cat2bug.api.domain.ApiDeliverable}。
 */
public class ApiDeliverable {

    @JsonIgnore
    private Long deliverableId;
    @JsonIgnore
    private Long deliverablePid;
    private String deliverableName;
    private String deliverablePath;
    private int childrenCount;
    private String remark;

    public Long getDeliverableId() {
        return deliverableId;
    }

    public void setDeliverableId(Long deliverableId) {
        this.deliverableId = deliverableId;
    }

    public Long getDeliverablePid() {
        return deliverablePid;
    }

    public void setDeliverablePid(Long deliverablePid) {
        this.deliverablePid = deliverablePid;
    }

    public String getDeliverableName() {
        return deliverableName;
    }

    public void setDeliverableName(String deliverableName) {
        this.deliverableName = deliverableName;
    }

    public String getDeliverablePath() {
        return deliverablePath;
    }

    public void setDeliverablePath(String deliverablePath) {
        this.deliverablePath = deliverablePath;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
