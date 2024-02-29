package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.entity.SysUser;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 评论对象 sys_comment
 * 
 * @author yuzhantao
 * @date 2024-02-29
 */
@Data
public class SysComment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 评论ID */
    private Long commentId;

    /** 评论内容 */
    @Excel(name = "评论内容")
    private String commentContent;

    /** 创建人ID */
    @Excel(name = "创建人ID")
    private Long createById;

    /** 所属模块(defect_log:缺陷日志的评论) */
    @Excel(name = "所属模块(defect_log:缺陷日志的评论)")
    private String moduleType;

    /** 关联id */
    @Excel(name = "关联id")
    private Long correlationId;

    /** 创建人 */
    private SysUser createMember;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("commentId", getCommentId())
            .append("commentContent", getCommentContent())
            .append("createById", getCreateById())
            .append("createTime", getCreateTime())
            .append("moduleType", getModuleType())
            .append("correlationId", getCorrelationId())
            .toString();
    }
}
