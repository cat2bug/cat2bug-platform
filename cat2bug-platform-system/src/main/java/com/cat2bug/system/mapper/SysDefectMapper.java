package com.cat2bug.system.mapper;

import java.util.Date;
import java.util.List;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.system.domain.SysVersion;
import org.apache.ibatis.annotations.Param;

/**
 * 缺陷Mapper接口
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
public interface SysDefectMapper 
{
    /**
     * 查询缺陷
     * 
     * @param defectId 缺陷主键
     * @return 缺陷
     */
    public SysDefect selectSysDefectByDefectId(@Param("defectId") Long defectId, @Param("currentUserId") Long currentUserId, @Param("currentTime") Date currentTime);

    /**
     * 查询项目中缺陷数量
     * @param projectId
     * @return
     */
    public Long getProjectDefectMaxNum(@Param("projectId") Long projectId, @Param("currentUserId") Long currentUserId);

    /**
     * 查询缺陷列表
     * 
     * @param sysDefect 缺陷
     * @return 缺陷集合
     */
    public List<SysDefect> selectSysDefectList(@Param("defect") SysDefect sysDefect, @Param("currentUserId") Long currentUserId, @Param("currentTime") Date currentTime);

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
     * 删除缺陷
     * 
     * @param defectId 缺陷主键
     * @return 结果
     */
    public int deleteSysDefectByDefectId(Long defectId);

    /**
     * 批量删除缺陷
     * 
     * @param defectIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysDefectByDefectIds(Long[] defectIds);

    /**
     * 获取项目中的历史版本
     * @param projectId 项目ID
     * @return  版本集合
     */
    public List<SysVersion> selectVersionList(@Param("projectId")Long projectId);
}
