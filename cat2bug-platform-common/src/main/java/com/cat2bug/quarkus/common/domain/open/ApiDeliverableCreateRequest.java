package com.cat2bug.quarkus.common.domain.open;

/**
 * Open API 创建交付物请求体，对齐 Spring {@code com.cat2bug.api.domain.ApiDeliverableCreateRequest}。
 */
public class ApiDeliverableCreateRequest {

    /** 父级交付物全路径，多级用 {@code /} 分隔；不填表示挂在项目根下 */
    private String parentDeliverablePath;
    private String deliverableName;
    private String remark;

    public String getParentDeliverablePath() {
        return parentDeliverablePath;
    }

    public void setParentDeliverablePath(String parentDeliverablePath) {
        this.parentDeliverablePath = parentDeliverablePath;
    }

    public String getDeliverableName() {
        return deliverableName;
    }

    public void setDeliverableName(String deliverableName) {
        this.deliverableName = deliverableName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
