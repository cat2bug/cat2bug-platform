package com.cat2bug.quarkus.common.domain.vo;

import java.util.List;

public class SendNotice {

    private String title;
    private String content;
    private Long projectId;
    private String groupName;
    private List<Long> receiveIds;
    private String src;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<Long> getReceiveIds() {
        return receiveIds;
    }

    public void setReceiveIds(List<Long> receiveIds) {
        this.receiveIds = receiveIds;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }
}
