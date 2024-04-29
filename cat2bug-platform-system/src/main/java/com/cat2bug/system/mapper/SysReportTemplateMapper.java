package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysReportTemplate;

/**
 * 报告模版Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-04-29
 */
public interface SysReportTemplateMapper 
{
    /**
     * 查询报告模版
     * 
     * @param templateId 报告模版主键
     * @return 报告模版
     */
    public SysReportTemplate selectSysReportTemplateByTemplateId(Long templateId);

    /**
     * 查询报告模版列表
     * 
     * @param sysReportTemplate 报告模版
     * @return 报告模版集合
     */
    public List<SysReportTemplate> selectSysReportTemplateList(SysReportTemplate sysReportTemplate);

    /**
     * 新增报告模版
     * 
     * @param sysReportTemplate 报告模版
     * @return 结果
     */
    public int insertSysReportTemplate(SysReportTemplate sysReportTemplate);

    /**
     * 修改报告模版
     * 
     * @param sysReportTemplate 报告模版
     * @return 结果
     */
    public int updateSysReportTemplate(SysReportTemplate sysReportTemplate);

    /**
     * 删除报告模版
     * 
     * @param templateId 报告模版主键
     * @return 结果
     */
    public int deleteSysReportTemplateByTemplateId(Long templateId);

    /**
     * 批量删除报告模版
     * 
     * @param templateIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysReportTemplateByTemplateIds(Long[] templateIds);
}
