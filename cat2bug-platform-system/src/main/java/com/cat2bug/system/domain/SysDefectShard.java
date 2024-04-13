package com.cat2bug.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 分享缺陷关联对象 sys_defect_shard
 * 
 * @author yuzhantao
 * @date 2024-04-10
 */
public class SysDefectShard extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 缺陷分享ID */
    private String defectShardId;
    /** 项目ID */
    private Long projectId;

    /** 缺陷ID */
    @Excel(name = "缺陷ID")
    private Long defectId;

    /** 分享密码 */
    @Excel(name = "分享密码")
    private String password;

    /** 创建人 */
    @Excel(name = "创建人")
    private Long createById;

    /** 时效时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "时效时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date agingTime;

    /** 默认语言 */
    private String defaultLang;

    /**
     * 时效小时数
     */
    private int agingHour;

    public void setDefectShardId(String defectShardId) 
    {
        this.defectShardId = defectShardId;
    }

    public String getDefectShardId() 
    {
        return defectShardId;
    }
    public void setDefectId(Long defectId) 
    {
        this.defectId = defectId;
    }

    public Long getDefectId() 
    {
        return defectId;
    }
    public void setPassword(String password) 
    {
        this.password = password;
    }

    public String getPassword() 
    {
        return password;
    }
    public void setCreateById(Long createById) 
    {
        this.createById = createById;
    }

    public Long getCreateById() 
    {
        return createById;
    }
    public void setAgingTime(Date agingTime) 
    {
        this.agingTime = agingTime;
    }

    public Date getAgingTime() 
    {
        return agingTime;
    }

    public int getAgingHour() {
        return agingHour;
    }

    public void setAgingHour(int agingHour) {
        this.agingHour = agingHour;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public void setDefaultLang(String defaultLang) {
        this.defaultLang = defaultLang;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("defectShardId", getDefectShardId())
            .append("defectId", getDefectId())
            .append("password", getPassword())
            .append("createTime", getCreateTime())
            .append("createById", getCreateById())
            .append("updateTime", getUpdateTime())
            .append("agingTime", getAgingTime())
            .toString();
    }
}
