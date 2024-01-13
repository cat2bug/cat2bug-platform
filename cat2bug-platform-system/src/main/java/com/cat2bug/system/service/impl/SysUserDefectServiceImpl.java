package com.cat2bug.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysUserDefectMapper;
import com.cat2bug.system.domain.SysUserDefect;
import com.cat2bug.system.service.ISysUserDefectService;

/**
 * 用户缺陷Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-01-10
 */
@Service
public class SysUserDefectServiceImpl implements ISysUserDefectService 
{
    @Autowired
    private SysUserDefectMapper sysUserDefectMapper;

    /**
     * 查询用户缺陷
     * 
     * @param userDefectId 用户缺陷主键
     * @return 用户缺陷
     */
    @Override
    public SysUserDefect selectSysUserDefectByUserDefectId(Long userDefectId)
    {
        return sysUserDefectMapper.selectSysUserDefectByUserDefectId(userDefectId);
    }

    /**
     * 查询用户缺陷列表
     * 
     * @param sysUserDefect 用户缺陷
     * @return 用户缺陷
     */
    @Override
    public List<SysUserDefect> selectSysUserDefectList(SysUserDefect sysUserDefect)
    {
        return sysUserDefectMapper.selectSysUserDefectList(sysUserDefect);
    }

    /**
     * 新增用户缺陷
     * 
     * @param sysUserDefect 用户缺陷
     * @return 结果
     */
    @Override
    public int insertSysUserDefect(SysUserDefect sysUserDefect)
    {
        return sysUserDefectMapper.insertSysUserDefect(sysUserDefect);
    }

    /**
     * 修改用户缺陷
     * 
     * @param sysUserDefect 用户缺陷
     * @return 结果
     */
    @Override
    public int updateSysUserDefect(SysUserDefect sysUserDefect)
    {
        return sysUserDefectMapper.updateSysUserDefect(sysUserDefect);
    }

    /**
     * 批量删除用户缺陷
     * 
     * @param userDefectIds 需要删除的用户缺陷主键
     * @return 结果
     */
    @Override
    public int deleteSysUserDefectByUserDefectIds(Long[] userDefectIds)
    {
        return sysUserDefectMapper.deleteSysUserDefectByUserDefectIds(userDefectIds);
    }

    /**
     * 删除用户缺陷信息
     * 
     * @param userDefectId 用户缺陷主键
     * @return 结果
     */
    @Override
    public int deleteSysUserDefectByUserDefectId(Long userDefectId)
    {
        return sysUserDefectMapper.deleteSysUserDefectByUserDefectId(userDefectId);
    }
}
