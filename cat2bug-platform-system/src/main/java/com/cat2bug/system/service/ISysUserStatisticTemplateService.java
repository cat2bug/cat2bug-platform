package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysUserStatisticTemplate;

/**
 * 用户统计模版Service接口
 * 
 * @author yuzhantao
 * @date 2024-01-24
 */
public interface ISysUserStatisticTemplateService 
{
    /**
     * 查询用户统计模版
     * 
     * @param statisticTemplateId 用户统计模版主键
     * @return 用户统计模版
     */
    public SysUserStatisticTemplate selectSysUserStatisticTemplateByStatisticTemplateId(Long statisticTemplateId);

    /**
     * 查询用户统计模版列表
     * 
     * @param sysUserStatisticTemplate 用户统计模版
     * @return 用户统计模版集合
     */
    public List<SysUserStatisticTemplate> selectSysUserStatisticTemplateList(SysUserStatisticTemplate sysUserStatisticTemplate);

    /**
     * 新增用户统计模版
     * 
     * @param sysUserStatisticTemplate 用户统计模版
     * @return 结果
     */
    public int insertSysUserStatisticTemplate(SysUserStatisticTemplate sysUserStatisticTemplate);

    /**
     * 修改用户统计模版
     * 
     * @param sysUserStatisticTemplate 用户统计模版
     * @return 结果
     */
    public int updateSysUserStatisticTemplate(SysUserStatisticTemplate sysUserStatisticTemplate);

    /**
     * 批量删除用户统计模版
     * 
     * @param statisticTemplateIds 需要删除的用户统计模版主键集合
     * @return 结果
     */
    public int deleteSysUserStatisticTemplateByStatisticTemplateIds(Long[] statisticTemplateIds);

    /**
     * 删除用户统计模版信息
     * 
     * @param statisticTemplateId 用户统计模版主键
     * @return 结果
     */
    public int deleteSysUserStatisticTemplateByStatisticTemplateId(Long statisticTemplateId);
}
