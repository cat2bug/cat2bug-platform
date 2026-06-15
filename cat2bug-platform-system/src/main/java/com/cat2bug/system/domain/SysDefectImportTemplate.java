package com.cat2bug.system.domain;

import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.cat2bug.common.enums.ExcelIndexedColor;

/**
 * 缺陷 Excel 导入模板（仅用于生成导入模版，与 {@link com.cat2bug.common.core.domain.entity.SysDefect} 导入逻辑字段一致）。
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class SysDefectImportTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "类型", i18nNameKey = "type", comboHandler = com.cat2bug.common.core.domain.excel.DefectTypeComboHandlerAdapter.class, headerBackgroundColor = ExcelIndexedColor.GREY_25_PERCENT)
    private String defectTypeImportName;

    @Excel(name = "缺陷名称", i18nNameKey = "defect.name", width = 100, headerColor = ExcelIndexedColor.RED, headerBackgroundColor = ExcelIndexedColor.GREY_25_PERCENT)
    private String defectName;

    @Excel(name = "描述", i18nNameKey = "describe", width = 100, headerBackgroundColor = ExcelIndexedColor.GREY_25_PERCENT)
    private String defectDescribe;

    @Excel(name = "缺陷等级", i18nNameKey = "defect.level", comboHandler = com.cat2bug.common.core.domain.excel.DefectLevelComboHandlerAdapter.class, headerBackgroundColor = ExcelIndexedColor.GREY_25_PERCENT)
    private String defectLevel;

    @Excel(name = "缺陷状态", i18nNameKey = "defect.state", comboHandler = com.cat2bug.common.core.domain.excel.DefectStateComboHandlerAdapter.class, headerBackgroundColor = ExcelIndexedColor.GREY_25_PERCENT)
    private String defectStateImportName;

    @Excel(name = "交付物", i18nNameKey = "module", width = 50, comboHandler = com.cat2bug.common.core.domain.excel.ModuleComboHandlerAdapter.class, headerBackgroundColor = ExcelIndexedColor.GREY_25_PERCENT)
    private String moduleName;

    @Excel(name = "版本", i18nNameKey = "version", headerBackgroundColor = ExcelIndexedColor.GREY_25_PERCENT)
    private String moduleVersion;

    @Excel(name = "图片", i18nNameKey = "image", cellType = Excel.ColumnType.IMAGE_LIST, width = 50, height = 50, headerBackgroundColor = ExcelIndexedColor.GREY_25_PERCENT)
    private String imgObjects;

    @Excel(name = "处理人", i18nNameKey = "handle-by", comboHandler = com.cat2bug.common.core.domain.excel.MemberComboHandlerAdapter.class, headerColor = ExcelIndexedColor.RED, headerBackgroundColor = ExcelIndexedColor.GREY_25_PERCENT)
    private String handleByNames;
}
