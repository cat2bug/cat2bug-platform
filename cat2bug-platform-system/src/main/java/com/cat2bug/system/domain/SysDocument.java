package com.cat2bug.system.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 文档对象 sys_document
 * 
 * @author yuzhantao
 * @date 2024-06-17
 */
@Data
public class SysDocument extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文档ID */
    private Long docId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 文档名称 */
    @Excel(name = "文档名称")
    private String docName;

    /** 文档类型(0=文件夹；1=文件) */
    @Excel(name = "文档类型(0=文件夹；1=文件)")
    private Long docType;

    /** 文件类型 */
    @Excel(name = "文件类型")
    private String fileExtension;

    /** 创建人ID */
    @Excel(name = "创建人ID")
    private Long createById;

    /** 更新人ID */
    @Excel(name = "更新人")
    private Long updateById;

    /** 更新人图标 */
    private String updateByAvatar;

    /** 文档版本 */
    @Excel(name = "文档版本")
    private Long fileVersion;

    /** 文件夹ID */
    @Excel(name = "文件夹ID")
    private Long docPid;

    /** 备注 */
    @Excel(name = "备注")
    private String docRemakr;

    /** 文件 */
    @Excel(name = "文件")
    private String fileUrl;

    /** 是否是叶子节点 */
    private boolean leaf;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("docId", getDocId())
            .append("projectId", getProjectId())
            .append("docName", getDocName())
            .append("docType", getDocType())
            .append("fileExtension", getFileExtension())
            .append("createTime", getCreateTime())
            .append("createById", getCreateById())
            .append("updateTime", getUpdateTime())
            .append("updateById", getUpdateById())
            .append("fileVersion", getFileVersion())
            .append("docPid", getDocPid())
            .append("docRemakr", getDocRemakr())
            .append("fileUrl", getFileUrl())
            .toString();
    }
}
