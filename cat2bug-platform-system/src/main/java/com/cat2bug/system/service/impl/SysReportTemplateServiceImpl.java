package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.uuid.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysReportTemplateMapper;
import com.cat2bug.system.domain.SysReportTemplate;
import com.cat2bug.system.service.ISysReportTemplateService;

/**
 * 报告模版Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-04-29
 */
@Service
public class SysReportTemplateServiceImpl implements ISysReportTemplateService 
{
    @Autowired
    private SysReportTemplateMapper sysReportTemplateMapper;

    /**
     * 查询报告模版
     * 
     * @param templateId 报告模版主键
     * @return 报告模版
     */
    @Override
    public SysReportTemplate selectSysReportTemplateByTemplateId(Long templateId)
    {
        return sysReportTemplateMapper.selectSysReportTemplateByTemplateId(templateId);
    }

    /**
     * 查询报告模版列表
     * 
     * @param sysReportTemplate 报告模版
     * @return 报告模版
     */
    @Override
    public List<SysReportTemplate> selectSysReportTemplateList(SysReportTemplate sysReportTemplate)
    {
        return sysReportTemplateMapper.selectSysReportTemplateList(sysReportTemplate);
    }

    /**
     * 新增报告模版
     * 
     * @param sysReportTemplate 报告模版
     * @return 结果
     */
    @Override
    public int insertSysReportTemplate(SysReportTemplate sysReportTemplate)
    {
        sysReportTemplate.setTemplateKey(UUID.randomUUID().toString());
        sysReportTemplate.setUpdateTime(DateUtils.getNowDate());
        return sysReportTemplateMapper.insertSysReportTemplate(sysReportTemplate);
    }

    /**
     * 修改报告模版
     * 
     * @param sysReportTemplate 报告模版
     * @return 结果
     */
    @Override
    public int updateSysReportTemplate(SysReportTemplate sysReportTemplate)
    {
        sysReportTemplate.setUpdateTime(DateUtils.getNowDate());
        return sysReportTemplateMapper.updateSysReportTemplate(sysReportTemplate);
    }

    /**
     * 批量删除报告模版
     * 
     * @param templateIds 需要删除的报告模版主键
     * @return 结果
     */
    @Override
    public int deleteSysReportTemplateByTemplateIds(Long[] templateIds)
    {
        return sysReportTemplateMapper.deleteSysReportTemplateByTemplateIds(templateIds);
    }

    /**
     * 删除报告模版信息
     * 
     * @param templateId 报告模版主键
     * @return 结果
     */
    @Override
    public int deleteSysReportTemplateByTemplateId(Long templateId)
    {
        return sysReportTemplateMapper.deleteSysReportTemplateByTemplateId(templateId);
    }
}
