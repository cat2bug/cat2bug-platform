package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysDefectShard;
import org.apache.ibatis.annotations.Param;

/**
 * 分享缺陷关联Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-04-10
 */
public interface SysDefectShardMapper 
{
    /**
     * 查询分享缺陷关联
     * 
     * @param defectShardId 分享缺陷关联主键
     * @return 分享缺陷关联
     */
    public SysDefectShard selectSysDefectShardByDefectShardId(String defectShardId);

    /**
     * 查询分享
     * @param defectId  缺陷id
     * @param memberId  成员id
     * @return          分享
     */
    public SysDefectShard selectSysDefectShardByDefectIdAndMemberId(@Param("defectId") Long defectId, @Param("memberId") Long memberId);

    /**
     * 查询分享缺陷关联列表
     * 
     * @param sysDefectShard 分享缺陷关联
     * @return 分享缺陷关联集合
     */
    public List<SysDefectShard> selectSysDefectShardList(SysDefectShard sysDefectShard);

    /**
     * 新增分享缺陷关联
     * 
     * @param sysDefectShard 分享缺陷关联
     * @return 结果
     */
    public int insertSysDefectShard(SysDefectShard sysDefectShard);

    /**
     * 修改分享缺陷关联
     * 
     * @param sysDefectShard 分享缺陷关联
     * @return 结果
     */
    public int updateSysDefectShard(SysDefectShard sysDefectShard);

    /**
     * 删除分享缺陷关联
     * 
     * @param defectShardId 分享缺陷关联主键
     * @return 结果
     */
    public int deleteSysDefectShardByDefectShardId(String defectShardId);

    /**
     * 批量删除分享缺陷关联
     * 
     * @param defectShardIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysDefectShardByDefectShardIds(String[] defectShardIds);
}
