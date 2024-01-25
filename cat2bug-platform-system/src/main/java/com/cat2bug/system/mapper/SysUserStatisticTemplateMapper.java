package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysUserStatisticTemplate;

/**
 * 用户统计模版Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-01-24
 */
public interface SysUserStatisticTemplateMapper 
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
     * 删除用户统计模版
     * 
     * @param statisticTemplateId 用户统计模版主键
     * @return 结果
     */
    public int deleteSysUserStatisticTemplateByStatisticTemplateId(Long statisticTemplateId);

    /**
     * 批量删除用户统计模版
     * 
     * @param statisticTemplateIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysUserStatisticTemplateByStatisticTemplateIds(Long[] statisticTemplateIds);

    /**
     * 批量删除用户统计模版
     *
     * @param sysUserStatisticTemplate 需要删除的数据
     * @return 结果
     */
    public int deleteSysUserStatisticTemplateByNameAndProjectAndMember(SysUserStatisticTemplate sysUserStatisticTemplate);
}
