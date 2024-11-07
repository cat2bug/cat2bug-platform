package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.handle.SysModuleComboHandlerAdapter;
import com.cat2bug.system.domain.handle.SysModuleIdHandlerAdapter;
import com.cat2bug.system.domain.handle.SysModuleNameHandlerAdapter;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.List;

/**
 * 测试用例对象 sys_case
 * 
 * @author yuzhantao
 * @date 2024-01-28
 */
@Data
public class SysCase extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 测试用例 */
    private Long caseId;

    /** 用例类型 */
    private Long caseType;

    /** 项目编号 */
    private Long projectId;

    /** 备注 */
    private String remark;

    /** 用例号码 */
    @Excel(name = "用例编号", i18nNameKey = "case.number", type = Excel.Type.EXPORT,headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private Long caseNum;

    /** 用例名称 */
    @Excel(name = "用例名称(必填)", i18nNameKey = "case.name_excel", align = HorizontalAlignment.LEFT, width = 50, headerColor = IndexedColors.RED,headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private String caseName;

    /** 模块id */
    @Excel(name = "交付物(必填)",
            i18nNameKey = "module.required",
            align = HorizontalAlignment.LEFT,
            width = 40,
            handler = SysModuleIdHandlerAdapter.class,
            comboHandler = SysModuleComboHandlerAdapter.class,
            headerColor = IndexedColors.RED,
            headerBackgroundColor=IndexedColors.GREY_25_PERCENT,
            type = Excel.Type.EXPORT
    )
    private Long moduleId;

    /** 模块名称 */
    @Excel(name = "交付物",
            i18nNameKey = "module.required",
            align = HorizontalAlignment.LEFT,
            width = 40,
            handler = SysModuleNameHandlerAdapter.class,
            comboHandler = SysModuleComboHandlerAdapter.class,
            headerColor = IndexedColors.RED,
            headerBackgroundColor=IndexedColors.GREY_25_PERCENT,
            type = Excel.Type.IMPORT
    )
    private String moduleName;

    /** 用例级别 */
    @Excel(name = "用例级别",i18nNameKey = "case.level", combo = "P0,P1,P2,P3,P4",
            headerBackgroundColor=IndexedColors.GREY_25_PERCENT,
            type = Excel.Type.IMPORT,
            handler = com.cat2bug.system.domain.excel.CaseLevelAdapter.class
    )
    private Long caseLevel;

    /** 用例级别 */
    @Excel(name = "用例级别",i18nNameKey = "case.level", combo = "P0,P1,P2,P3,P4", headerBackgroundColor=IndexedColors.GREY_25_PERCENT, type = Excel.Type.EXPORT)
    private String caseLevelName;

    /** 前置条件 */
    @Excel(name = "前置条件",i18nNameKey = "case.prerequisite", align = HorizontalAlignment.LEFT, width = 50, headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private String casePreconditions;

    /** 预期 */
    @Excel(name = "预期(必填)", i18nNameKey = "case.expected_excel", align = HorizontalAlignment.LEFT, width = 50, headerColor = IndexedColors.RED,headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private String caseExpect;

    @Excel(name = "数据", i18nNameKey = "case.data", width = 50, align = HorizontalAlignment.LEFT, headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private String caseData;

    /** 步骤 */
    @Excel(name = "步骤", i18nNameKey = "case.step", width = 50, align = HorizontalAlignment.LEFT, type = Excel.Type.EXPORT, headerBackgroundColor=IndexedColors.GREY_25_PERCENT, handler = com.cat2bug.system.domain.excel.CaseStepAdapter.class)
    private List<SysCaseStep> caseStep;

    @Excel(name = "步骤", i18nNameKey = "case.step", width = 50, align = HorizontalAlignment.LEFT, type = Excel.Type.IMPORT, headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private String caseStepScript;

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

    /** 焦点成员 */
    private List<SysUser> focusList;

    /** 关联通过的缺陷数量 */
    private int defectProcessingCount;
    /** 关联失败的缺陷数量 */
    private int defectAuditCount;
    /** 关联失败的缺陷数量 */
    private int defectCloseCount;
    /** 关联的测试计划子项 */
    private SysPlanItem planItem;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("caseId", getCaseId())
            .append("caseName", getCaseName())
            .append("moduleId", getModuleId())
            .append("caseType", getCaseType())
            .append("caseExpect", getCaseExpect())
            .append("caseStep", getCaseStep())
            .append("caseLevel", getCaseLevel())
            .append("casePreconditions", getCasePreconditions())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("caseNum", getCaseNum())
            .append("projectId", getProjectId())
            .toString();
    }
}
