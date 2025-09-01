package com.cat2bug.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysUserStatisticTemplateMapper;
import com.cat2bug.system.domain.SysUserStatisticTemplate;
import com.cat2bug.system.service.ISysUserStatisticTemplateService;

/**
 * 用户统计模版Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-01-24
 */
@Service
public class SysUserStatisticTemplateServiceImpl implements ISysUserStatisticTemplateService 
{
    @Autowired
    private SysUserStatisticTemplateMapper sysUserStatisticTemplateMapper;

    /**
     * 查询用户统计模版
     * 
     * @param statisticTemplateId 用户统计模版主键
     * @return 用户统计模版
     */
    @Override
    public SysUserStatisticTemplate selectSysUserStatisticTemplateByStatisticTemplateId(Long statisticTemplateId)
    {
        return sysUserStatisticTemplateMapper.selectSysUserStatisticTemplateByStatisticTemplateId(statisticTemplateId);
    }

    /**
     * 查询用户统计模版列表
     * 
     * @param sysUserStatisticTemplate 用户统计模版
     * @return 用户统计模版
     */
    @Override
    public List<SysUserStatisticTemplate> selectSysUserStatisticTemplateList(SysUserStatisticTemplate sysUserStatisticTemplate)
    {
        return sysUserStatisticTemplateMapper.selectSysUserStatisticTemplateList(sysUserStatisticTemplate);
    }

    /**
     * 新增用户统计模版
     * 
     * @param sysUserStatisticTemplate 用户统计模版
     * @return 结果
     */
    @Override
    public int insertSysUserStatisticTemplate(SysUserStatisticTemplate sysUserStatisticTemplate)
    {
        if(sysUserStatisticTemplate.getModuleType()!=null &&
        sysUserStatisticTemplate.getUserId()!=null &&
        sysUserStatisticTemplate.getProjectId()!=null) {
            List<SysUserStatisticTemplate> list = sysUserStatisticTemplateMapper.selectSysUserStatisticTemplateList(sysUserStatisticTemplate);
            if(list!=null && list.size()>0) {
                // 更新
                sysUserStatisticTemplate.setStatisticTemplateId(list.get(0).getStatisticTemplateId());
                return sysUserStatisticTemplateMapper.updateSysUserStatisticTemplate(sysUserStatisticTemplate);
            }
        }

        return sysUserStatisticTemplateMapper.insertSysUserStatisticTemplate(sysUserStatisticTemplate);
    }

    /**
     * 修改用户统计模版
     * 
     * @param sysUserStatisticTemplate 用户统计模版
     * @return 结果
     */
    @Override
    public int updateSysUserStatisticTemplate(SysUserStatisticTemplate sysUserStatisticTemplate)
    {
        return sysUserStatisticTemplateMapper.updateSysUserStatisticTemplate(sysUserStatisticTemplate);
    }

    /**
     * 批量删除用户统计模版
     * 
     * @param statisticTemplateIds 需要删除的用户统计模版主键
     * @return 结果
     */
    @Override
    public int deleteSysUserStatisticTemplateByStatisticTemplateIds(Long[] statisticTemplateIds)
    {
        return sysUserStatisticTemplateMapper.deleteSysUserStatisticTemplateByStatisticTemplateIds(statisticTemplateIds);
    }

    /**
     * 删除用户统计模版信息
     * 
     * @param statisticTemplateId 用户统计模版主键
     * @return 结果
     */
    @Override
    public int deleteSysUserStatisticTemplateByStatisticTemplateId(Long statisticTemplateId)
    {
        return sysUserStatisticTemplateMapper.deleteSysUserStatisticTemplateByStatisticTemplateId(statisticTemplateId);
    }
}
