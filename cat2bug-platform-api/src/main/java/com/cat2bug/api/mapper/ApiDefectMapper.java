package com.cat2bug.api.mapper;

import com.cat2bug.api.domain.ApiDefect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 缺陷Mapper接口
 * 
 * @author yuzhantao
 */
@Mapper
public interface ApiDefectMapper 
{
    /**
     * 查询缺陷
     * 
     * @param defectId 缺陷主键
     * @return 缺陷
     */
//    public ApiDefect selectApiDefectByDefectId(@Param("defectId") Long defectId, @Param("currentUserId") Long currentUserId);

    /**
     * 查询项目中缺陷数量
     * @param projectId
     * @return
     */
    public Long getProjectDefectMaxNum(@Param("projectId") Long projectId);


    /**
     * 查询缺陷列表
     * 
     * @param apiDefect 缺陷
     * @return 缺陷集合
     */
    public List<ApiDefect> selectApiDefectList(@Param("projectId") Long projectId, @Param("defect") ApiDefect apiDefect);

    /**
     * 新增缺陷
     * 
     * @param apiDefect 缺陷
     * @return 结果
     */
    public int insertApiDefect(@Param("projectId") Long projectId, @Param("defect")  ApiDefect apiDefect);

    /**
     * 修改缺陷
     * 
     * @param apiDefect 缺陷
     * @return 结果
     */
//    public int updateApiDefect(ApiDefect apiDefect);

    /**
     * 删除缺陷
     * 
     * @param defectNumber 缺陷主键
     * @return 结果
     */
//    public int deleteApiDefectByDefectId(Long defectNumber);

    /**
     * 批量删除缺陷
     * 
     * @param defectNumbers 需要删除的数据主键集合
     * @return 结果
     */
//    public int deleteApiDefectByDefectIds(Long[] defectNumbers);
}
