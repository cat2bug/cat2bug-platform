package com.cat2bug.system.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.domain.type.SysDefectLogStateEnum;
import com.cat2bug.system.domain.type.SysDefectStateEnum;
import com.cat2bug.system.domain.type.SysDefectTypeEnum;
import com.cat2bug.system.domain.vo.EnumVo;
import com.cat2bug.system.mapper.SysDefectLogMapper;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysDefectMapper;
import com.cat2bug.system.domain.SysDefect;
import com.cat2bug.system.service.ISysDefectService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 缺陷Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
@Service
public class SysDefectServiceImpl implements ISysDefectService 
{
    @Autowired
    private SysDefectMapper sysDefectMapper;
    @Autowired
    private SysDefectLogMapper sysDefectLogMapper;

    @Override
    @Transactional
    public SysDefectLog assign(SysDefectLog sysDefectLog) {
        // 更新缺陷
        SysDefect sd = new SysDefect();
        sd.setDefectId(sysDefectLog.getDefectId());
        sd.setUpdateTime(DateUtils.getNowDate());
        sd.setUpdateBy(String.valueOf(SecurityUtils.getUserId()));
        sd.setDefectState(SysDefectStateEnum.AUDIT);
        sd.setHandleBy(sysDefectLog.getReceiveBy());
        Preconditions.checkState(sysDefectMapper.updateSysDefect(sd)>0, MessageUtils.message("defect.assign_fail"));

        // 插入日志
        sysDefectLog.setDefectLogType(SysDefectLogStateEnum.ASSIGN);
        this.inertLog(sysDefectLog);
        return sysDefectLog;
    }

    @Override
    public List<EnumVo> getDefectTypeList() {
        return Arrays.asList(
                new EnumVo(SysDefectTypeEnum.BUG.ordinal(),SysDefectTypeEnum.BUG.name()),
                new EnumVo(SysDefectTypeEnum.TASK.ordinal(),SysDefectTypeEnum.TASK.name()),
                new EnumVo(SysDefectTypeEnum.DEMAND.ordinal(),SysDefectTypeEnum.DEMAND.name())
        );
    }

    /**
     * 查询缺陷
     * 
     * @param defectId 缺陷主键
     * @return 缺陷
     */
    @Override
    public SysDefect selectSysDefectByDefectId(Long defectId)
    {
        return sysDefectMapper.selectSysDefectByDefectId(defectId);
    }

    /**
     * 查询缺陷列表
     * 
     * @param sysDefect 缺陷
     * @return 缺陷
     */
    @Override
    public List<SysDefect> selectSysDefectList(SysDefect sysDefect)
    {
        return sysDefectMapper.selectSysDefectList(sysDefect);
    }

    /**
     * 新增缺陷
     * 
     * @param sysDefect 缺陷
     * @return 结果
     */
    @Override
    @Transactional
    public int insertSysDefect(SysDefect sysDefect)
    {
        sysDefect.setCreateTime(DateUtils.getNowDate());
        sysDefect.setUpdateTime(DateUtils.getNowDate());
        long count = sysDefectMapper.getProjectDefectCount(sysDefect.getProjectId());
        sysDefect.setProjectNum(count+1);
        Preconditions.checkState(sysDefectMapper.insertSysDefect(sysDefect)>0,MessageUtils.message("defect.insert_fail"));
        this.inertLog(null,null,SysDefectLogStateEnum.CREATE);
        return 1;
    }

    /** 添加日志 */
    private SysDefectLog inertLog(List<Long> receives,String describe,SysDefectLogStateEnum state){
        SysDefectLog sysDefectLog = new SysDefectLog();
        sysDefectLog.setDefectLogDescribe(describe);
        sysDefectLog.setReceiveBy(receives);
        sysDefectLog.setDefectLogType(state);
        return this.inertLog(sysDefectLog);
    }

    /** 添加日志 */
    private SysDefectLog inertLog(SysDefectLog sysDefectLog){
        sysDefectLog.setCreateBy(String.valueOf(SecurityUtils.getUserId()));
        sysDefectLog.setCreateTime(DateUtils.getNowDate());
        return this.sysDefectLogMapper.insertSysDefectLog(sysDefectLog)>0?sysDefectLog:null;
    }

    /**
     * 修改缺陷
     * 
     * @param sysDefect 缺陷
     * @return 结果
     */
    @Override
    public int updateSysDefect(SysDefect sysDefect)
    {
        sysDefect.setUpdateTime(DateUtils.getNowDate());
        return sysDefectMapper.updateSysDefect(sysDefect);
    }

    /**
     * 批量删除缺陷
     * 
     * @param defectIds 需要删除的缺陷主键
     * @return 结果
     */
    @Override
    public int deleteSysDefectByDefectIds(Long[] defectIds)
    {
        return sysDefectMapper.deleteSysDefectByDefectIds(defectIds);
    }

    /**
     * 删除缺陷信息
     * 
     * @param defectId 缺陷主键
     * @return 结果
     */
    @Override
    public int deleteSysDefectByDefectId(Long defectId)
    {
        return sysDefectMapper.deleteSysDefectByDefectId(defectId);
    }
}
