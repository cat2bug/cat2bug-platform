package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.entity.SysDefect;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 项目缺陷页签配置对象 sys_project_defect_tabs
 * 
 * @author yuzhantao
 * @date 2024-04-06
 */
@Data
public class SysProjectDefectTabs extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 页签ID */
    private Long tabId;

    /** 页签名称 */
    @Excel(name = "页签名称")
    private String tabName;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 配置项 */
    @Excel(name = "配置项")
    private SysDefect config;

    /** 排序 */
    @Excel(name = "排序")
    private Long tabSort;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("tabId", getTabId())
            .append("tabName", getTabName())
            .append("projectId", getProjectId())
            .append("userId", getUserId())
            .append("config", getConfig())
            .append("tabSort", getTabSort())
            .append("createTime", getCreateTime())
            .toString();
    }
}
