package com.cat2bug.system.domain;

import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;
import com.cat2bug.system.domain.handle.SysModuleComboHandlerAdapter;
import com.cat2bug.system.domain.handle.SysModuleHandlerAdapter;
import lombok.Data;
import org.apache.poi.ss.usermodel.IndexedColors;

/**
 * 测试用例对象 sys_case
 * 
 * @author yuzhantao
 * @date 2024-01-28
 */
@Data
public class SysCaseExcelTepmplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

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
    @Excel(name = "步骤", i18nNameKey = "case.step", width = 50, headerBackgroundColor=IndexedColors.GREY_25_PERCENT)
    private String caseStep;

}
