package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysReportTemplate;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.system.mapper.SysReportTemplateMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SysReportTemplateService {

    @Inject
    SysReportTemplateMapper sysReportTemplateMapper;

    public TableDataInfo selectTemplatePage(SysReportTemplate query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysReportTemplateMapper.countSysReportTemplateList(query);
        List<SysReportTemplate> rows = sysReportTemplateMapper.selectSysReportTemplateList(
                query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysReportTemplate> selectTemplateListAll(SysReportTemplate query) {
        return sysReportTemplateMapper.selectSysReportTemplateList(query, 0, Integer.MAX_VALUE);
    }

    public SysReportTemplate selectSysReportTemplateByTemplateId(Long templateId) {
        SysReportTemplate template = sysReportTemplateMapper.selectSysReportTemplateByTemplateId(templateId);
        if (template == null) {
            throw new ApiException("报告模版不存在");
        }
        return template;
    }

    @Transactional
    public SysReportTemplate insertSysReportTemplate(SysReportTemplate sysReportTemplate) {
        sysReportTemplate.setTemplateKey(UUID.randomUUID().toString());
        sysReportTemplate.setUpdateTime(new Date());
        if (sysReportTemplateMapper.insertSysReportTemplate(sysReportTemplate) <= 0) {
            throw new ApiException("创建报告模版失败");
        }
        return sysReportTemplate;
    }

    @Transactional
    public SysReportTemplate updateSysReportTemplate(SysReportTemplate sysReportTemplate) {
        if (sysReportTemplate.getTemplateId() == null) {
            throw new ApiException("模版ID不能为空");
        }
        selectSysReportTemplateByTemplateId(sysReportTemplate.getTemplateId());
        sysReportTemplate.setUpdateTime(new Date());
        if (sysReportTemplateMapper.updateSysReportTemplate(sysReportTemplate) <= 0) {
            throw new ApiException("更新报告模版失败");
        }
        return sysReportTemplate;
    }

    @Transactional
    public int deleteSysReportTemplateByTemplateIds(Long[] templateIds) {
        if (templateIds == null || templateIds.length == 0) {
            throw new ApiException("模版ID不能为空");
        }
        return sysReportTemplateMapper.deleteSysReportTemplateByTemplateIds(templateIds);
    }
}
