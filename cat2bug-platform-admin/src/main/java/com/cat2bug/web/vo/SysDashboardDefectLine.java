package com.cat2bug.web.vo;

import com.cat2bug.common.annotation.Excel;
import lombok.Data;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.Date;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-04-10 12:34
 * @Version: 1.0.0
 */
@Data
public class SysDashboardDefectLine {
    /** 发生时间 */
    @Excel(name = "发生时间", i18nNameKey = "date", type = Excel.Type.EXPORT,headerBackgroundColor= IndexedColors.GREY_25_PERCENT)
    private Date time;
    /** 待处理数量 */
    @Excel(name = "待处理数量", i18nNameKey = "PROCESSING", type = Excel.Type.EXPORT,headerBackgroundColor= IndexedColors.GREY_25_PERCENT)
    private int processingCount;
    /** 待验证 */
    @Excel(name = "待验证", i18nNameKey = "AUDIT", type = Excel.Type.EXPORT,headerBackgroundColor= IndexedColors.GREY_25_PERCENT)
    private int auditCount;
    /** 已解决 */
    @Excel(name = "已解决", i18nNameKey = "RESOLVED", type = Excel.Type.EXPORT,headerBackgroundColor= IndexedColors.GREY_25_PERCENT)
    private int resolvedCount;
    /** 已驳回 */
    @Excel(name = "已驳回", i18nNameKey = "REJECTED", type = Excel.Type.EXPORT,headerBackgroundColor= IndexedColors.GREY_25_PERCENT)
    private int rejectedCount;
    /** 已关闭 */
    @Excel(name = "已关闭", i18nNameKey = "CLOSED", type = Excel.Type.EXPORT,headerBackgroundColor= IndexedColors.GREY_25_PERCENT)
    private int closedCount;
}
