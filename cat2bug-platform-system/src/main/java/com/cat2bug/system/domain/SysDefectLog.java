package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.type.SysDefectLogStateEnum;
import com.cat2bug.system.domain.type.SysDefectStateEnum;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 缺陷日志对象 sys_defect_log
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
@Data
public class SysDefectLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 缺陷日志id */
    private Long defectLogId;

    /** 缺陷日志的描述 */
    @Excel(name = "缺陷日志的描述")
    private String defectLogDescribe;

    /** 处理类型(转发\评论\关闭) */
    private SysDefectLogStateEnum defectLogType;

    @Excel(name = "处理类型")
    private String defectLogTypeName;

    /** 缺陷接收人 */
    @Excel(name = "缺陷接收人")
    private List<Long> receiveBy;

    private List<SysUser> receiveByList;

    /** 附件集合 */
    @Excel(name = "附件集合")
    private String annexUrls;

    /** 缺陷id */
    @Excel(name = "缺陷id")
    private Long defectId;

    private String createByName;

    private String createByAvatar;

    /** 评论 */
    private List<SysComment> commentList;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("defectLogId", getDefectLogId())
            .append("defectLogDescribe", getDefectLogDescribe())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("defectLogType", getDefectLogType())
            .append("receiveBy", getReceiveBy())
            .append("annexUrls", getAnnexUrls())
            .append("defectId", getDefectId())
            .toString();
    }
}
