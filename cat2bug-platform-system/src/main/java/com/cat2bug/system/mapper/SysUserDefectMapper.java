package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysUserDefect;

/**
 * 用户缺陷Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-01-10
 */
public interface SysUserDefectMapper 
{
    /**
     * 查询用户缺陷
     * 
     * @param userDefectId 用户缺陷主键
     * @return 用户缺陷
     */
    public SysUserDefect selectSysUserDefectByUserDefectId(Long userDefectId);

    /**
     * 查询用户缺陷列表
     * 
     * @param sysUserDefect 用户缺陷
     * @return 用户缺陷集合
     */
    public List<SysUserDefect> selectSysUserDefectList(SysUserDefect sysUserDefect);

    /**
     * 新增用户缺陷
     * 
     * @param sysUserDefect 用户缺陷
     * @return 结果
     */
    public int insertSysUserDefect(SysUserDefect sysUserDefect);

    /**
     * 修改用户缺陷
     * 
     * @param sysUserDefect 用户缺陷
     * @return 结果
     */
    public int updateSysUserDefect(SysUserDefect sysUserDefect);

    /**
     * 删除用户缺陷
     * 
     * @param userDefectId 用户缺陷主键
     * @return 结果
     */
    public int deleteSysUserDefectByUserDefectId(Long userDefectId);

    /**
     * 批量删除用户缺陷
     * 
     * @param userDefectIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysUserDefectByUserDefectIds(Long[] userDefectIds);
}
