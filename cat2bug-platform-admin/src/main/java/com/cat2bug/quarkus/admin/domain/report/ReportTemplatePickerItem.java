package com.cat2bug.quarkus.admin.domain.report;

import com.cat2bug.quarkus.common.domain.SysReportTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Picker row: custom templates expose DB fields; presets expose key/title/preview metadata.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportTemplatePickerItem {

    private Long templateId;
    private String moduleType;
    private String templateContent;
    private String templateTitle;
    private Long updateById;
    private Long projectId;
    private String templateIconUrl;
    private Long majorVersion;
    private Long minorVersion;
    private String templateKey;
    private String source;
    private String groupCode;
    private Boolean editable;
    private Boolean deletable;

    public static ReportTemplatePickerItem fromCustom(SysReportTemplate template) {
        ReportTemplatePickerItem item = new ReportTemplatePickerItem();
        item.setTemplateId(template.getTemplateId());
        item.setModuleType(template.getModuleType());
        item.setTemplateContent(template.getTemplateContent());
        item.setTemplateTitle(template.getTemplateTitle());
        item.setUpdateById(template.getUpdateById());
        item.setProjectId(template.getProjectId());
        item.setTemplateIconUrl(template.getTemplateIconUrl());
        item.setMajorVersion(template.getMajorVersion());
        item.setMinorVersion(template.getMinorVersion());
        item.setTemplateKey(template.getTemplateKey());
        item.setSource("custom");
        item.setGroupCode("custom");
        item.setEditable(true);
        item.setDeletable(true);
        return item;
    }

    public static ReportTemplatePickerItem fromPreset(
            String groupCode, String templateKey, String title, String previewUrl) {
        ReportTemplatePickerItem item = new ReportTemplatePickerItem();
        item.setTemplateKey(templateKey);
        item.setTemplateTitle(title);
        item.setTemplateIconUrl(previewUrl);
        item.setSource("preset");
        item.setGroupCode(groupCode);
        item.setEditable(false);
        item.setDeletable(false);
        return item;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public String getTemplateTitle() {
        return templateTitle;
    }

    public void setTemplateTitle(String templateTitle) {
        this.templateTitle = templateTitle;
    }

    public Long getUpdateById() {
        return updateById;
    }

    public void setUpdateById(Long updateById) {
        this.updateById = updateById;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTemplateIconUrl() {
        return templateIconUrl;
    }

    public void setTemplateIconUrl(String templateIconUrl) {
        this.templateIconUrl = templateIconUrl;
    }

    public Long getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(Long majorVersion) {
        this.majorVersion = majorVersion;
    }

    public Long getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(Long minorVersion) {
        this.minorVersion = minorVersion;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public Boolean getDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }
}
