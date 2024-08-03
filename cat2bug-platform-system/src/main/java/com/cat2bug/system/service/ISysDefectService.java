package com.cat2bug.system.service;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.domain.SysVersion;
import com.cat2bug.system.domain.vo.EnumVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

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
     * @param memberId 成员主键
     * @return 缺陷
     */
    public SysDefect selectSysDefectByDefectId(Long defectId, Long memberId);

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

    /**
     * 获取项目中的历史版本
     * @param projectId 项目ID
     * @return  版本集合
     */
    public List<SysVersion> selectVersionList(@Param("projectId")Long projectId);

    /**
     * 获取默认缺陷通知配置
     * @return
     */
    public Map<String, Object> getDefaultDefectNoticeOption();

    /**
     * 导入缺陷
     * @param projectId 项目ID
     * @param list      缺陷列表
     * @return
     */
    public String importDefect(Long projectId, List<SysDefect> list);
}
