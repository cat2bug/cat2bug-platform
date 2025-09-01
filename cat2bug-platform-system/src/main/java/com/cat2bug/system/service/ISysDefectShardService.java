package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysDefectShard;

/**
 * 分享缺陷关联Service接口
 * 
 * @author yuzhantao
 * @date 2024-04-10
 */
public interface ISysDefectShardService 
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
     * @param defectId 缺陷ID
     * @param memberId 成员ID
     * @return  分享
     */
    public SysDefectShard selectSysDefectShardByDefectIdAndMemberId(Long defectId,Long memberId);

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
    public SysDefectShard insertSysDefectShard(SysDefectShard sysDefectShard);

    /**
     * 修改分享缺陷关联
     * 
     * @param sysDefectShard 分享缺陷关联
     * @return 结果
     */
    public int updateSysDefectShard(SysDefectShard sysDefectShard);

    /**
     * 批量删除分享缺陷关联
     * 
     * @param defectShardIds 需要删除的分享缺陷关联主键集合
     * @return 结果
     */
    public int deleteSysDefectShardByDefectShardIds(String[] defectShardIds);

    /**
     * 删除分享缺陷关联信息
     * 
     * @param defectShardId 分享缺陷关联主键
     * @return 结果
     */
    public int deleteSysDefectShardByDefectShardId(String defectShardId);
}
