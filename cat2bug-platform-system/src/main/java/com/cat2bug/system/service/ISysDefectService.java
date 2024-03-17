package com.cat2bug.system.service;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.domain.vo.EnumVo;

import java.util.List;

/**
 * 缺陷Service接口
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
public interface ISysDefectService 
{
    /**
     * 指派
     * @param sysDefectLog 缺陷日志
     * @return  缺陷日志
     */
    public SysDefectLog assign(SysDefectLog sysDefectLog);

    /**
     * 驳回
     * @param sysDefectLog  缺陷日志
     * @return  缺陷日志
     */
    public SysDefectLog reject(SysDefectLog sysDefectLog);

    /**
     * 修复
     * @param sysDefectLog  缺陷日志
     * @return  缺陷日志
     */
    public SysDefectLog repair(SysDefectLog sysDefectLog);

    /**
     * 通过
     * @param sysDefectLog  缺陷日志
     * @return  缺陷日志
     */
    public SysDefectLog pass(SysDefectLog sysDefectLog);

    /**
     * 关闭
     * @param sysDefectLog  缺陷日志
     * @return  缺陷日志
     */
    public SysDefectLog close(SysDefectLog sysDefectLog);

    /**
     * 启动
     * @param sysDefectLog  缺陷日志
     * @return  缺陷日志
     */
    public SysDefectLog open(SysDefectLog sysDefectLog);

    /**
     * 获取缺陷类型列表
     * @return
     */
    public List<EnumVo> getDefectTypeList();

    public List<EnumVo> getDefectStateList();

    /**
     * 查询缺陷
     * 
     * @param defectId 缺陷主键
     * @return 缺陷
     */
    public SysDefect selectSysDefectByDefectId(Long defectId);

    /**
     * 查询缺陷列表
     * 
     * @param sysDefect 缺陷
     * @return 缺陷集合
     */
    public List<SysDefect> selectSysDefectList(SysDefect sysDefect);

    /**
     * 新增缺陷
     * 
     * @param sysDefect 缺陷
     * @return 结果
     */
    public int insertSysDefect(SysDefect sysDefect);

    /**
     * 修改缺陷
     * 
     * @param sysDefect 缺陷
     * @return 结果
     */
    public int updateSysDefect(SysDefect sysDefect);

    /**
     * 批量删除缺陷
     * 
     * @param defectIds 需要删除的缺陷主键集合
     * @return 结果
     */
    public int deleteSysDefectByDefectIds(Long[] defectIds);

    /**
     * 删除缺陷信息
     * 
     * @param defectId 缺陷主键
     * @return 结果
     */
    public int deleteSysDefectByDefectId(Long defectId);
}
