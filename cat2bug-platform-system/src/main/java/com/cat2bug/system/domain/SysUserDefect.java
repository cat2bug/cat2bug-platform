package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 用户缺陷对象 sys_user_defect
 * 
 * @author yuzhantao
 * @date 2024-01-10
 */
public class SysUserDefect extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户缺陷id */
    private Long userDefectId;

    /** 缺陷id */
    @Excel(name = "缺陷id")
    private Long defectId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 是否收藏 */
    @Excel(name = "是否收藏")
    private Integer collect;

    public void setUserDefectId(Long userDefectId) 
    {
        this.userDefectId = userDefectId;
    }

    public Long getUserDefectId() 
    {
        return userDefectId;
    }
    public void setDefectId(Long defectId) 
    {
        this.defectId = defectId;
    }

    public Long getDefectId() 
    {
        return defectId;
    }
    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }
    public void setCollect(Integer collect) 
    {
        this.collect = collect;
    }

    public Integer getCollect() 
    {
        return collect;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userDefectId", getUserDefectId())
            .append("defectId", getDefectId())
            .append("userId", getUserId())
            .append("collect", getCollect())
            .toString();
    }
}
