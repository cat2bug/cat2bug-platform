package com.cat2bug.system.service.impl;

import java.util.Calendar;
import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.sign.Md5Utils;
import com.cat2bug.common.utils.uuid.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysDefectShardMapper;
import com.cat2bug.system.domain.SysDefectShard;
import com.cat2bug.system.service.ISysDefectShardService;

/**
 * 分享缺陷关联Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-04-10
 */
@Service
public class SysDefectShardServiceImpl implements ISysDefectShardService 
{
    @Autowired
    private SysDefectShardMapper sysDefectShardMapper;

    /**
     * 查询分享缺陷关联
     * 
     * @param defectShardId 分享缺陷关联主键
     * @return 分享缺陷关联
     */
    @Override
    public SysDefectShard selectSysDefectShardByDefectShardId(String defectShardId)
    {
        return sysDefectShardMapper.selectSysDefectShardByDefectShardId(defectShardId);
    }

    @Override
    public SysDefectShard selectSysDefectShardByDefectIdAndMemberId(Long defectId, Long memberId) {
        return sysDefectShardMapper.selectSysDefectShardByDefectIdAndMemberId(defectId,memberId);
    }

    /**
     * 查询分享缺陷关联列表
     * 
     * @param sysDefectShard 分享缺陷关联
     * @return 分享缺陷关联
     */
    @Override
    public List<SysDefectShard> selectSysDefectShardList(SysDefectShard sysDefectShard)
    {
        return sysDefectShardMapper.selectSysDefectShardList(sysDefectShard);
    }

    /**
     * 新增分享缺陷关联
     * 
     * @param sysDefectShard 分享缺陷关联
     * @return 结果
     */
    @Override
    public SysDefectShard insertSysDefectShard(SysDefectShard sysDefectShard)
    {
        sysDefectShard.setCreateById(SecurityUtils.getUserId());
        sysDefectShard.setUpdateTime(DateUtils.getNowDate());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DateUtils.getNowDate());
        calendar.add(Calendar.HOUR, sysDefectShard.getAgingHour());
        sysDefectShard.setAgingTime(calendar.getTime());

        int count;
        SysDefectShard shard = sysDefectShardMapper.selectSysDefectShardByDefectIdAndMemberId(sysDefectShard.getDefectId(),SecurityUtils.getUserId());
        if(shard==null) {
            sysDefectShard.setCreateTime(DateUtils.getNowDate());
            sysDefectShard.setDefectShardId(createShardId(sysDefectShard));
            count = sysDefectShardMapper.insertSysDefectShard(sysDefectShard);
        } else {
            sysDefectShard.setDefectShardId(shard.getDefectShardId());
            count = sysDefectShardMapper.updateSysDefectShard(sysDefectShard);
        }
        return count == 1 ? sysDefectShard : null;
    }

    private String createShardId(SysDefectShard sysDefectShard) {
        String id = sysDefectShard.getDefectId() + UUID.randomUUID().toString();
        return Md5Utils.hash(id);
    }

    /**
     * 修改分享缺陷关联
     * 
     * @param sysDefectShard 分享缺陷关联
     * @return 结果
     */
    @Override
    public int updateSysDefectShard(SysDefectShard sysDefectShard)
    {
        sysDefectShard.setUpdateTime(DateUtils.getNowDate());
        return sysDefectShardMapper.updateSysDefectShard(sysDefectShard);
    }

    /**
     * 批量删除分享缺陷关联
     * 
     * @param defectShardIds 需要删除的分享缺陷关联主键
     * @return 结果
     */
    @Override
    public int deleteSysDefectShardByDefectShardIds(String[] defectShardIds)
    {
        return sysDefectShardMapper.deleteSysDefectShardByDefectShardIds(defectShardIds);
    }

    /**
     * 删除分享缺陷关联信息
     * 
     * @param defectShardId 分享缺陷关联主键
     * @return 结果
     */
    @Override
    public int deleteSysDefectShardByDefectShardId(String defectShardId)
    {
        return sysDefectShardMapper.deleteSysDefectShardByDefectShardId(defectShardId);
    }
}
