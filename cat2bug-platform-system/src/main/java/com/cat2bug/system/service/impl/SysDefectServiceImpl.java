package com.cat2bug.system.service.impl;

import java.util.ArrayList;
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

    /**
     * 指派
     * @param sysDefectLog 缺陷日志
     * @return
     */
    @Override
    @Transactional
    public SysDefectLog assign(SysDefectLog sysDefectLog) {
        // 更新缺陷
        SysDefect sd = new SysDefect();
        sd.setDefectId(sysDefectLog.getDefectId());
        sd.setHandleBy(sysDefectLog.getReceiveBy());
        this.updateSysDefect(sd);

        // 插入日志
        sysDefectLog.setDefectLogType(SysDefectLogStateEnum.ASSIGN);
        this.inertLog(sysDefectLog);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(sysDefectLog.getDefectLogId());
    }

    /**
     * 驳回
     * @param sysDefectLog  缺陷日志
     * @return
     */
    @Override
    @Transactional
    public SysDefectLog reject(SysDefectLog sysDefectLog) {
        // 更新缺陷
        SysDefect sd = this.selectSysDefectByDefectId(sysDefectLog.getDefectId());
        Preconditions.checkNotNull(sd,MessageUtils.message("defect.not_found"));
        sd.setDefectState(SysDefectStateEnum.REJECTED);
        sd.setHandleBy(Arrays.asList(Long.valueOf(sd.getUpdateBy())));
        this.updateSysDefect(sd);

        // 插入日志
        sysDefectLog.setDefectLogType(SysDefectLogStateEnum.REJECTED);
        sysDefectLog.setReceiveBy(sd.getHandleBy());
        this.inertLog(sysDefectLog);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(sysDefectLog.getDefectLogId());
    }

    /**
     * 修复
     * @param sysDefectLog  缺陷日志
     * @return
     */
    @Override
    @Transactional
    public SysDefectLog repair(SysDefectLog sysDefectLog) {
        // 更新缺陷
        SysDefect sd = new SysDefect();
        sd.setDefectId(sysDefectLog.getDefectId());
        sd.setDefectState(SysDefectStateEnum.AUDIT);
        sd.setHandleBy(sysDefectLog.getReceiveBy());
        this.updateSysDefect(sd);

        // 插入日志
        sysDefectLog.setDefectLogType(SysDefectLogStateEnum.REPAIR);
        sysDefectLog.setReceiveBy(sd.getHandleBy());
        this.inertLog(sysDefectLog);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(sysDefectLog.getDefectLogId());
    }

    /**
     * 通过
     * @param sysDefectLog  缺陷日志
     * @return
     */
    @Override
    @Transactional
    public SysDefectLog pass(SysDefectLog sysDefectLog) {
        // 更新缺陷
        SysDefect sd = new SysDefect();
        sd.setDefectId(sysDefectLog.getDefectId());
        sd.setDefectState(SysDefectStateEnum.RESOLVED);
        this.updateSysDefect(sd);

        // 插入日志
        sysDefectLog.setDefectLogType(SysDefectLogStateEnum.PASS);
        this.inertLog(sysDefectLog);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(sysDefectLog.getDefectLogId());
    }

    @Override
    @Transactional
    public SysDefectLog close(SysDefectLog sysDefectLog) {
        // 更新缺陷
        SysDefect sd = new SysDefect();
        sd.setDefectId(sysDefectLog.getDefectId());
        sd.setDefectState(SysDefectStateEnum.CLOSED);
        this.updateSysDefect(sd);

        // 插入日志
        sysDefectLog.setDefectLogType(SysDefectLogStateEnum.CLOSED);
        this.inertLog(sysDefectLog);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(sysDefectLog.getDefectLogId());
    }

    @Override
    @Transactional
    public SysDefectLog open(SysDefectLog sysDefectLog) {
        // 更新缺陷
        SysDefect sd = new SysDefect();
        sd.setDefectId(sysDefectLog.getDefectId());
        sd.setDefectState(SysDefectStateEnum.PROCESSING);
        sd.setHandleBy(sysDefectLog.getReceiveBy());
        this.updateSysDefect(sd);

        // 插入日志
        sysDefectLog.setDefectLogType(SysDefectLogStateEnum.OPEN);
        this.inertLog(sysDefectLog);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(sysDefectLog.getDefectLogId());
    }

    @Override
    public List<EnumVo> getDefectTypeList() {
        List<EnumVo> ret = new ArrayList<>();
        for(int i = 0;i<SysDefectTypeEnum.values().length;i++){
            ret.add(new EnumVo(SysDefectTypeEnum.values()[i].ordinal(),SysDefectTypeEnum.values()[i].name()));
        }
        return ret;
    }

    @Override
    public List<EnumVo> getDefectStateList() {
        List<EnumVo> ret = new ArrayList<>();
        for(int i = 0;i<SysDefectStateEnum.values().length;i++){
            ret.add(new EnumVo(SysDefectStateEnum.values()[i].ordinal(),SysDefectStateEnum.values()[i].name()));
        }
        return ret;
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
        return sysDefectMapper.selectSysDefectByDefectId(defectId, SecurityUtils.getUserId());
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
        return sysDefectMapper.selectSysDefectList(sysDefect, SecurityUtils.getUserId());
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
        // 新建缺陷数据
        sysDefect.setCreateTime(DateUtils.getNowDate());
        sysDefect.setUpdateTime(DateUtils.getNowDate());
        sysDefect.setUpdateTime(DateUtils.getNowDate());
        sysDefect.setDefectState(SysDefectStateEnum.PROCESSING);
        sysDefect.setCreateBy(SecurityUtils.getUsername());
        sysDefect.setUpdateBy(SecurityUtils.getUsername());
        sysDefect.setCreateById(SecurityUtils.getUserId());
        sysDefect.setUpdateById(SecurityUtils.getUserId());
        long count = sysDefectMapper.getProjectDefectMaxNum(sysDefect.getProjectId(), SecurityUtils.getUserId());
        sysDefect.setProjectNum(count+1);
        Preconditions.checkState(sysDefectMapper.insertSysDefect(sysDefect)>0,MessageUtils.message("defect.insert_fail"));
        // 新建日志
        this.inertLog(sysDefect.getDefectId(),sysDefect.getHandleBy(),null,SysDefectLogStateEnum.CREATE);
        return 1;
    }

    /** 添加日志 */
    private SysDefectLog inertLog(Long defectId, List<Long> receives,String describe,SysDefectLogStateEnum state){
        SysDefectLog sysDefectLog = new SysDefectLog();
        sysDefectLog.setDefectId(defectId);
        sysDefectLog.setDefectLogDescribe(describe);
        sysDefectLog.setReceiveBy(receives);
        sysDefectLog.setDefectLogType(state);
        return this.inertLog(sysDefectLog);
    }

    /** 添加日志 */
    private SysDefectLog inertLog(SysDefectLog sysDefectLog){
        Preconditions.checkNotNull(sysDefectLog.getDefectId(),MessageUtils.message("defect.defect_id_cannot_empty"));
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
    @Transactional
    public int updateSysDefect(SysDefect sysDefect)
    {
        sysDefect.setUpdateTime(DateUtils.getNowDate());
        sysDefect.setUpdateBy(String.valueOf(SecurityUtils.getUserId()));
        int ret = sysDefectMapper.updateSysDefect(sysDefect);
        Preconditions.checkState(ret>0, MessageUtils.message("defect.update_fail"));
        return ret;
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
