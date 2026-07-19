package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.BaseEntity;
import com.cat2bug.common.core.domain.entity.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报告编辑历史 sys_report_history
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class SysReportHistory extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 历史ID */
    private Long historyId;

    /** 报告ID */
    private Long reportId;

    /** 保存时的正文快照 */
    private String reportDescription;

    /** 修改人ID */
    private Long createById;

    /** 修改人 */
    private SysUser createMember;

    /** 该历史条目下的评论数（列表展示用，非持久化） */
    private Integer commentCount;
}
