package com.cat2bug.common.core.domain.entity;

import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.List;

/**
 * 缺陷对象 sys_defect
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
@Data
public class SysDefect extends BaseEntity
{
    public static final String KEY = "defect";

    private static final long serialVersionUID = 1L;

    /** 缺陷id */
    private Long defectId;

    /** 项目编号 */
    @Excel(name = "编号", i18nNameKey = "id", type=Excel.Type.EXPORT, width = 30)
    private Long projectNum;

    private Long defectNumber;

    /** 缺陷类型 */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Excel(name = "类型", i18nNameKey = "type", type=Excel.Type.EXPORT,handler = com.cat2bug.common.core.domain.excel.DefectTypeHandler.class, comboHandler = com.cat2bug.common.core.domain.excel.DefectTypeComboHandlerAdapter.class)
    private SysDefectTypeEnum defectType;

    @Excel(name = "类型", i18nNameKey = "type", type=Excel.Type.IMPORT, comboHandler = com.cat2bug.common.core.domain.excel.DefectTypeComboHandlerAdapter.class)
    private String defectTypeImportName;

    private SysDefectTypeEnum defectTypeName;

    /** 缺陷标题 */
    @Excel(name = "缺陷名称", i18nNameKey = "defect.name", width = 100)
    private String defectName;

    /** 缺陷描述 */
    @Excel(name = "描述", i18nNameKey = "describe", width = 100)
    private String defectDescribe;

    /** 缺陷等级 */
    @Excel(name = "缺陷等级", i18nNameKey = "defect.level", handler = com.cat2bug.common.core.domain.excel.DefectLevelHandler.class, comboHandler = com.cat2bug.common.core.domain.excel.DefectLevelComboHandlerAdapter.class)
    private String defectLevel;

    /** 缺陷等级 */
    private String defectLevelName;

    /** 缺陷状态 */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @Excel(name = "缺陷状态", i18nNameKey = "defect.state", type=Excel.Type.EXPORT,handler = com.cat2bug.common.core.domain.excel.DefectStateHandler.class, comboHandler = com.cat2bug.common.core.domain.excel.DefectStateComboHandlerAdapter.class)
    private SysDefectStateEnum defectState;

    @Excel(name = "缺陷状态", i18nNameKey = "defect.state", type=Excel.Type.IMPORT, comboHandler = com.cat2bug.common.core.domain.excel.DefectStateComboHandlerAdapter.class)
    private String defectStateImportName;

    /** 缺陷状态 */
    private SysDefectStateEnum defectStateName;

    /** 测试模块id */
    private Long moduleId;

    /** 测试模块名称 */
    @Excel(name = "交付物", i18nNameKey = "module", width = 50, comboHandler = com.cat2bug.common.core.domain.excel.ModuleComboHandlerAdapter.class)
    private String moduleName;

    /** 版本 */
    @Excel(name = "版本", i18nNameKey = "version")
    private String moduleVersion;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:MM:ss")
    @Excel(name = "创建时间", i18nNameKey = "create-time", width = 30, dateFormat = "yyyy-MM-dd HH:MM:ss", type = Excel.Type.EXPORT)
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:MM:ss")
    @Excel(name = "更新时间", i18nNameKey = "update-time", width = 30, dateFormat = "yyyy-MM-dd HH:MM:ss", type = Excel.Type.EXPORT)
    private Date updateTime;

    /** 计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "计划开始时间", i18nNameKey = "plan-start-time", width = 30, dateFormat = "yyyy-MM-dd HH:MM:ss", type = Excel.Type.EXPORT)
    private Date planStartTime;
    /** 计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "计划结束时间", i18nNameKey = "plan-end-time", width = 30, dateFormat = "yyyy-MM-dd HH:MM:ss", type = Excel.Type.EXPORT)
    private Date planEndTime;

    /** 处理人id */
    private List<Long> handleBy;

    /** 处理人名称 */
    private List<SysUser> handleByList;

    /** 处理人名称 */
    @Excel(name = "处理人", i18nNameKey = "handle-by", comboHandler = com.cat2bug.common.core.domain.excel.MemberComboHandlerAdapter.class)
    private String handleByNames;

    /** 图片 */
    @Excel(name = "图片", i18nNameKey = "image", cellType = Excel.ColumnType.IMAGE_LIST, type = Excel.Type.EXPORT, width = 50,height = 50)
    private String imgUrls;
    private String imgList;
    @Excel(name = "图片", i18nNameKey = "image", cellType = Excel.ColumnType.IMAGE, type = Excel.Type.IMPORT, width = 50,height = 50)
    private String imgObjects;

    /** 附件 */
    @Excel(name = "附件", i18nNameKey = "annex", width = 100,handler = com.cat2bug.common.core.domain.excel.UrlListHandler.class, type = Excel.Type.EXPORT)
    private String annexUrls;
    private String annexList;

    /** 项目id */
    private Long projectId;

    /** 项目 */
//    @Excel(name = "项目")
    private String projectName;


    /** 测试计划id */
//    @Excel(name = "测试计划id")
    private Long testPlanId;

    /** 测试用例id */
//    @Excel(name = "测试用例id")
    private Long caseId;

    /** 数据来源 */
//    @Excel(name = "数据来源")
    private Integer dataSources;

    /** 数据来源相关参数 */
//    @Excel(name = "数据来源相关参数")
    private String dataSourcesParams;

    /** 用例步骤id */
//    @Excel(name = "用例步骤id")
    private Long caseStepId;

    /** 焦点成员 */
    private List<SysUser> focusList;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
//    @Excel(name = "处理时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date handleTime;

    /** 是否收藏 */
    private boolean collect;

    /**
     * 缺陷组标识
     */
    private String defectGroupKey;
    /**
     * 缺陷唯一标识
     */
    private String defectKey;

    /** 驳回次数 */
    private int rejectCount;
    /** 处理的通过时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handlePassTime;
    /** 处理的开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handleStartTime;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("defectId", getDefectId())
            .append("defectType", getDefectType())
            .append("defectName", getDefectName())
            .append("defectDescribe", getDefectDescribe())
            .append("annexUrls", getAnnexUrls())
            .append("projectId", getProjectId())
            .append("testPlanId", getTestPlanId())
            .append("caseId", getCaseId())
            .append("dataSources", getDataSources())
            .append("dataSourcesParams", getDataSourcesParams())
            .append("moduleId", getModuleId())
            .append("moduleVersion", getModuleVersion())
            .append("createBy", getCreateBy())
            .append("updateTime", getUpdateTime())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("defectState", getDefectState())
            .append("caseStepId", getCaseStepId())
            .append("handleBy", getHandleBy())
            .append("handleTime", getHandleTime())
            .append("defectLevel", getDefectLevel())
            .toString();
    }
}
