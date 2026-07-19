package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.entity.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(callSuper = false)
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

    /** 父评论ID（NULL=主评论，非空=扁平回复挂主评；defect_log 路径不使用，保持为空） */
    private Long parentId;

    /** 锚点类型：line=整行，selection=选中文字（仅报告批注使用） */
    private String anchorKind;

    /** 锚点原文片段快照（仅报告批注使用） */
    private String quote;

    /** 锚点前文片段快照，用于重复片段消歧（仅报告批注使用） */
    private String contextBefore;

    /** 锚点后文片段快照，用于重复片段消歧（仅报告批注使用） */
    private String contextAfter;

    /** 锚点辅助信息，如最近标题ID/块指纹，仅辅助匹配（仅报告批注使用） */
    private String anchorHint;

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
            .append("parentId", getParentId())
            .append("anchorKind", getAnchorKind())
            .toString();
    }
}
