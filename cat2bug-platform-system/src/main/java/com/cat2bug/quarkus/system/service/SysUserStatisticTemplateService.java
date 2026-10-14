package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysUserStatisticTemplate;
import com.cat2bug.quarkus.system.mapper.SysUserStatisticTemplateMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class SysUserStatisticTemplateService {

    @Inject
    SysUserStatisticTemplateMapper sysUserStatisticTemplateMapper;

    public TableDataInfo selectTemplatePage(SysUserStatisticTemplate query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysUserStatisticTemplateMapper.countSysUserStatisticTemplateList(query);
        List<SysUserStatisticTemplate> rows = sysUserStatisticTemplateMapper.selectSysUserStatisticTemplateList(
                query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public SysUserStatisticTemplate selectById(Long statisticTemplateId) {
        return sysUserStatisticTemplateMapper.selectSysUserStatisticTemplateByStatisticTemplateId(statisticTemplateId);
    }

    public List<SysUserStatisticTemplate> selectTemplateListAll(SysUserStatisticTemplate query) {
        long total = sysUserStatisticTemplateMapper.countSysUserStatisticTemplateList(query);
        if (total <= 0) {
            return List.of();
        }
        int limit = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        List<SysUserStatisticTemplate> rows = sysUserStatisticTemplateMapper.selectSysUserStatisticTemplateList(
                query, 0, limit);
        return rows != null ? rows : List.of();
    }

    public int insertSysUserStatisticTemplate(SysUserStatisticTemplate template) {
        if (template.getModuleType() != null && template.getUserId() != null && template.getProjectId() != null) {
            SysUserStatisticTemplate probe = new SysUserStatisticTemplate();
            probe.setModuleType(template.getModuleType());
            probe.setUserId(template.getUserId());
            probe.setProjectId(template.getProjectId());
            List<SysUserStatisticTemplate> existing = sysUserStatisticTemplateMapper
                    .selectSysUserStatisticTemplateList(probe, 0, 1);
            if (existing != null && !existing.isEmpty()) {
                template.setStatisticTemplateId(existing.get(0).getStatisticTemplateId());
                return sysUserStatisticTemplateMapper.updateSysUserStatisticTemplate(template);
            }
        }
        return sysUserStatisticTemplateMapper.insertSysUserStatisticTemplate(template);
    }

    public int updateSysUserStatisticTemplate(SysUserStatisticTemplate template) {
        return sysUserStatisticTemplateMapper.updateSysUserStatisticTemplate(template);
    }

    public int deleteByIds(Long[] statisticTemplateIds) {
        return sysUserStatisticTemplateMapper.deleteSysUserStatisticTemplateByStatisticTemplateIds(statisticTemplateIds);
    }
}
