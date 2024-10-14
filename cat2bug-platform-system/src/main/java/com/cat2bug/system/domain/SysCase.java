package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.handle.SysModuleComboHandlerAdapter;
import com.cat2bug.system.domain.handle.SysModuleHandlerAdapter;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;
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

    /** 模块id */
//    @Excel(name = "模块",
//            i18nNameKey = "module",
//            width = 40,
//            type = Excel.Type.IMPORT,
//            handler = SysModuleHandlerAdapter.class,
//            headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private Long moduleId;

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
    @Excel(name = "用例名称(必填)", i18nNameKey = "case.name_excel", width = 50, headerColor = IndexedColors.RED,headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private String caseName;

    /** 模块名称 */
    @Excel(name = "模块",
            i18nNameKey = "module",
            width = 40,
            handler = SysModuleHandlerAdapter.class,
            comboHandler = SysModuleComboHandlerAdapter.class,
            headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private String moduleName;

    /** 用例级别 */
    @Excel(name = "用例级别",i18nNameKey = "case.level", combo = "1,2,3,4,5",headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private Long caseLevel;

    /** 前置条件 */
    @Excel(name = "前置条件",i18nNameKey = "case.prerequisite", width = 50, headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private String casePreconditions;

    /** 预期 */
    @Excel(name = "预期(必填)", i18nNameKey = "case.expected_excel", width = 50, headerColor = IndexedColors.RED,headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private String caseExpect;

    /** 步骤 */
    private List<SysCaseStep> caseStep;

    @Excel(name = "步骤", i18nNameKey = "case.step", width = 50, headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private String caseStepScript;

    /** 图片集合 */
    private String imgUrls;

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
