package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.type.SysDefectLogStateEnum;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.IMUserConfig;
import com.cat2bug.im.service.IIMUserConfigService;
import com.cat2bug.im.service.IMService;
import com.cat2bug.system.domain.*;
import com.cat2bug.system.domain.vo.EnumVo;
import com.cat2bug.system.mapper.*;
import com.cat2bug.system.service.ISysDefectService;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 缺陷Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
@Service
public class SysDefectServiceImpl implements ISysDefectService 
{
    private final static Logger log = LogManager.getLogger(SysDefectServiceImpl.class);

    @Autowired
    private SysDefectMapper sysDefectMapper;
    @Autowired
    private SysDefectLogMapper sysDefectLogMapper;
    @Autowired
    private SysUserDefectMapper sysUserDefectMapper;
    @Autowired
    private IMService imService;
    @Autowired
    private IIMUserConfigService imUserConfigService;
    @Autowired
    private DefectMessageOfNoticeTemplateImpl defectMessageOfNoticeTemplate;
    @Autowired
    private SysModuleMapper sysModuleMapper;
    @Autowired
    private SysUserProjectMapper sysUserProjectMapper;
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

        // 发送通知
        SysDefect sysDefect = sysDefectMapper.selectSysDefectByDefectId(sysDefectLog.getDefectId(), SecurityUtils.getUserId(), DateUtils.getNowDate());
        this.sendDefectNotice(sysDefect.getProjectId(), sysDefectLog.getSrcHost(), sd.getDefectId());

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
        SysDefect sd = this.selectSysDefectByDefectId(sysDefectLog.getDefectId(), SecurityUtils.getUserId());
        Preconditions.checkNotNull(sd,MessageUtils.message("defect.not_found"));
        sd.setDefectState(SysDefectStateEnum.REJECTED);
        sd.setHandleBy(Arrays.asList(Long.valueOf(sd.getUpdateBy())));
        this.updateSysDefect(sd);

        // 发送通知
        SysDefect sysDefect = sysDefectMapper.selectSysDefectByDefectId(sysDefectLog.getDefectId(), SecurityUtils.getUserId(), DateUtils.getNowDate());
        this.sendDefectNotice(sysDefect.getProjectId(), sysDefectLog.getSrcHost(), sd.getDefectId());

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

        // 发送通知
        SysDefect sysDefect = sysDefectMapper.selectSysDefectByDefectId(sysDefectLog.getDefectId(), SecurityUtils.getUserId(), DateUtils.getNowDate());
        this.sendDefectNotice(sysDefect.getProjectId(), sysDefectLog.getSrcHost(), sd.getDefectId());

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
        sd.setDefectState(SysDefectStateEnum.CLOSED);
        this.updateSysDefect(sd);

        // 发送通知
        SysDefect sysDefect = sysDefectMapper.selectSysDefectByDefectId(sysDefectLog.getDefectId(),SecurityUtils.getUserId(), DateUtils.getNowDate());
        this.sendDefectNotice(sysDefect.getProjectId(), sysDefectLog.getSrcHost(), sd.getDefectId());

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

        // 发送通知
        SysDefect sysDefect = sysDefectMapper.selectSysDefectByDefectId(sysDefectLog.getDefectId(),SecurityUtils.getUserId(), DateUtils.getNowDate());
        this.sendDefectNotice(sysDefect.getProjectId(), sysDefectLog.getSrcHost(), sd.getDefectId());

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

        SysDefect sysDefect = sysDefectMapper.selectSysDefectByDefectId(sysDefectLog.getDefectId(),SecurityUtils.getUserId(), DateUtils.getNowDate());
        // 发送通知
        this.sendDefectNotice(sysDefect.getProjectId(), sysDefectLog.getSrcHost(), sd.getDefectId());

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
        return ret.stream().filter(s->!s.getValue().equals("RESOLVED")).collect(Collectors.toList());
    }

    /**
     * 查询缺陷
     * 
     * @param defectId 缺陷主键
     * @param memberId 成员主键
     * @return 缺陷
     */
    @Override
    public SysDefect selectSysDefectByDefectId(Long defectId,Long memberId)
    {
        SysDefect sysDefect = sysDefectMapper.selectSysDefectByDefectId(defectId, memberId, DateUtils.getNowDate());
        Preconditions.checkNotNull(sysDefect, MessageUtils.message("defect.not_found"));
        if(sysDefect.getHandleByList()!=null){
            sysDefect.setHandleByList(sysDefect.getHandleByList().stream().filter(h->h.getUserId()>0).collect(Collectors.toList()));
        }
        return sysDefect;
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
        return sysDefectMapper.selectSysDefectList(sysDefect, SecurityUtils.getUserId(), DateUtils.getNowDate());
    }

    /**
     * 新增缺陷
     *
     * @param sysDefect 缺陷
     * @return 结果
     */
    @Override
    @Transactional
    public SysDefect insertSysDefect(SysDefect sysDefect)
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

        // 发送通知
        this.sendDefectNotice(sysDefect.getProjectId(), sysDefect.getSrcHost(), sysDefect.getDefectId());

        // 新建日志
        this.inertLog(sysDefect.getDefectId(),sysDefect.getHandleBy(),null,SysDefectLogStateEnum.CREATE);
        return sysDefect;
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

    /** 批量添加日志 */
    private int batchInertLog(List<SysDefect> defectList, String describe,SysDefectLogStateEnum state){
        List<SysDefectLog> logList = new ArrayList<>();
        defectList.forEach(defect->{
            SysDefectLog sysDefectLog = new SysDefectLog();
            sysDefectLog.setDefectId(defect.getDefectId());
            sysDefectLog.setDefectLogDescribe(describe);
            sysDefectLog.setReceiveBy(defect.getHandleBy());
            sysDefectLog.setDefectLogType(state);
            sysDefectLog.setCreateBy(String.valueOf(SecurityUtils.getUserId()));
            sysDefectLog.setCreateTime(DateUtils.getNowDate());
            logList.add(sysDefectLog);
        });

        return this.sysDefectLogMapper.batchInsertSysDefectLog(logList);
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
        Long memberId = SecurityUtils.getUserId();
        sysDefect.setUpdateBy(String.valueOf(memberId));
        int ret = this.sysDefectMapper.updateSysDefect(sysDefect);
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

    /**
     * 获取项目中的历史版本
     * @param projectId 项目ID
     * @return  版本集合
     */
    @Override
    public List<SysVersion> selectVersionList(Long projectId) {
        return sysDefectMapper.selectVersionList(projectId);
    }

    /**
     * 获取默认缺陷通知配置
     * @return
     */
    @Override
    public Map<String, Object> getDefaultDefectNoticeOption() {
        Map<String, Object> moduleOption = new HashMap<>();

        Map<String, Object> defectOption = new HashMap<>();
        moduleOption.put("defect",defectOption);

        Map<String, Object> eventOption = new HashMap<>();
        defectOption.put("event",eventOption);
        eventOption.put("receiver", true);
        eventOption.put("collect", true);

        Map<String, Object> optionOption = new HashMap<>();
        defectOption.put("option",optionOption);
        Map<String, Object> realtimeOption = new HashMap<>();
        optionOption.put("realtime",realtimeOption);
        realtimeOption.put("switch",true);
        return moduleOption;
    }

    /**
     * 发送缺陷通知
     * @param defectId
     */
    private void sendDefectNotice(Long projectId, String srcHost, Long defectId) {
        SysDefect defect = this.sysDefectMapper.selectSysDefectByDefectId(defectId,SecurityUtils.getUserId(), DateUtils.getNowDate());
        defect.setSrcHost(srcHost);
        if(defect!=null) {
            // 获取关注缺陷的人ID集合
            SysUserDefect userDefect = new SysUserDefect();
            userDefect.setDefectId(defectId);
            userDefect.setCollect(1);
            List<Long> collectUserList = this.sysUserDefectMapper.selectSysUserDefectList(userDefect).stream()
                    .filter(u->{
                        try {
                            IMUserConfig userConfig = imUserConfigService.selectImUserConfigByProjectAndMember(projectId, u.getUserId(),this.getDefaultDefectNoticeOption());
                            Map<String, Object> defectConfig = (Map<String, Object>) userConfig.getConfig().getModules().get("defect");
                            Map<String, Object> eventConfig = (Map<String, Object>) defectConfig.get("event");
                            return (boolean) eventConfig.get("collect");
                        }catch (Exception e) {
                            log.error(e);
                            return false;
                        }
                    })
                    .map(u->u.getUserId()).collect(Collectors.toList());
            List<Long> handleUserList = defect.getHandleBy().stream()
                    .filter(id->{
                        try {
                            IMUserConfig userConfig = imUserConfigService.selectImUserConfigByProjectAndMember(projectId, id,this.getDefaultDefectNoticeOption());
                            Map<String, Object> defectConfig = (Map<String, Object>) userConfig.getConfig().getModules().get("defect");
                            Map<String, Object> eventConfig = (Map<String, Object>) defectConfig.get("event");
                            return (boolean) eventConfig.get("receiver");
                        }catch (Exception e) {
                            log.error(e);
                            return false;
                        }
                    }).collect(Collectors.toList());
            try {
                // 将缺陷处理人和关注人ID放到一个集合里
                Set<Long> receiverList = new HashSet<>();
                receiverList.addAll(collectUserList);       // 添加关注人集合
                receiverList.addAll(handleUserList);        // 添加处理人集合
                // 给处理人和关注此缺陷的人发送通知
                String title = String.format("[%s]%s:#%d %s",
                        MessageUtils.message(defect.getDefectState().name()),
                        MessageUtils.message("defect"),
                        defect.getProjectNum(),
                        defect.getDefectName());
                this.imService.sendMessage(
                        defect.getProjectId(),  // 项目ID
                        SysDefect.KEY,  // 通知组名称
                        SecurityUtils.getUserId(),  // 发送人ID
                        receiverList.stream().collect(Collectors.toList()), // 接收人集合
                        title,      // 通知标题
                        defect,     // 通知内容
                        String.format("%s/#/project/defect?defectId=%d",defect.getSrcHost(), defect.getDefectId()),
                        this.defectMessageOfNoticeTemplate,  // 通知内容格式模版
                        this.getDefaultDefectNoticeOption()
                );
            }catch (Exception e) {
                log.error(e);
            }
        }
    }

    @Transactional
    @Override
    public String importDefect(Long projectId, List<SysDefect> list) {
        List<String> sb = new ArrayList<>();
        long count = sysDefectMapper.getProjectDefectMaxNum(projectId, SecurityUtils.getUserId());

        Map<String, SysModule> moduleMap = sysModuleMapper.selectSysModulePathList(projectId).stream().collect(Collectors.toMap(SysModule::getModulePath, m->m));

        Map<String, SysUser> userMap = sysUserProjectMapper.selectSysUserListByProjectId(projectId,new SysUser()).stream().collect(Collectors.toMap(SysUser::getNickName, u->u));

        for(int i=0;i<list.size();i++){
            List<String> emptyCell = new ArrayList<>();     // 空数据列名集合
            List<String> invalidCell = new ArrayList<>();   // 无效的列名集合
            SysDefect d = list.get(i);
            int line = i+2;
            if(d==null) {
                sb.add(MessageUtils.message("line-data-format-exception", line));
                continue;
            }
            if(StringUtils.isBlank(d.getDefectName())) {
                emptyCell.add(MessageUtils.message("defect.name"));
            }
            if(StringUtils.isNotBlank(d.getDefectStateImportName())){
                switch (d.getDefectStateImportName()){
                    case "处理中":
                    case "Processing":
                        d.setDefectState(SysDefectStateEnum.PROCESSING);
                        break;
                    case "待审核":
                    case "Audit":
                        d.setDefectState(SysDefectStateEnum.AUDIT);
                        break;
                    case "已解决":
                        d.setDefectState(SysDefectStateEnum.RESOLVED);
                        break;
                    case "已驳回":
                    case "Rejected":
                        d.setDefectState(SysDefectStateEnum.REJECTED);
                        break;
                    case "已关闭":
                    case "Close":
                        d.setDefectState(SysDefectStateEnum.CLOSED);
                        break;
                }
            } else {
                emptyCell.add(MessageUtils.message("defect.state"));
            }
            if(StringUtils.isNotBlank(d.getDefectTypeImportName())) {
                switch (d.getDefectTypeImportName()){
                    case "BUG":
                    case "Bug":
                        d.setDefectType(SysDefectTypeEnum.BUG);
                        break;
                    case "Task":
                    case "任务":
                        d.setDefectType(SysDefectTypeEnum.TASK);
                        break;
                    case "Demand":
                    case "需求":
                        d.setDefectType(SysDefectTypeEnum.DEMAND);
                        break;
                }
            } else {
                emptyCell.add(MessageUtils.message("type"));
            }

            if(StringUtils.isNotBlank(d.getModuleName())){
                if(moduleMap.containsKey(d.getModuleName())) {
                    d.setModuleId(moduleMap.get(d.getModuleName()).getModuleId());
                } else {
                    invalidCell.add(MessageUtils.message("module"));
                }
            } else {
                emptyCell.add(MessageUtils.message("module"));
            }

            if(StringUtils.isNotBlank(d.getHandleByNames())){
                if(userMap.containsKey(d.getHandleByNames())) {
                    d.setHandleBy(Arrays.asList(userMap.get(d.getHandleByNames()).getUserId()));
                } else {
                    invalidCell.add(MessageUtils.message("handle-by"));
                }
            } else {
                emptyCell.add(MessageUtils.message("handle-by"));
            }

            if(emptyCell.size()>0){
                sb.add(MessageUtils.message("line-data-not-empty",line,emptyCell.stream().collect(Collectors.joining("、"))));
            }
            if(invalidCell.size()>0){
                sb.add(MessageUtils.message("line-data-not-empty",line,invalidCell.stream().collect(Collectors.joining("、"))));
            }
            d.setProjectNum(++count);
            d.setProjectId(projectId);
            d.setCreateTime(DateUtils.getNowDate());
            d.setUpdateTime(DateUtils.getNowDate());
            d.setCreateBy(SecurityUtils.getUsername());
            d.setUpdateBy(SecurityUtils.getUsername());
            d.setCreateById(SecurityUtils.getUserId());
            d.setUpdateById(SecurityUtils.getUserId());
            d.setImgUrls(d.getImgObjects());
        }
        if(sb.size()==0) {
            // 批量添加数据
            List<List<SysDefect>> batchList = Lists.partition(list, 50);
            for (int i = 0; i < batchList.size(); i++) {
                sysDefectMapper.batchInsertSysDefect(batchList.get(i));
                // 新建日志
                List<Long> ids = batchList.get(i).stream().map(d->d.getDefectId()).collect(Collectors.toList());
                this.batchInertLog(batchList.get(i),null,SysDefectLogStateEnum.IMPORT);
            }
        }
        return sb.stream().collect(Collectors.joining("<br/>"));
    }
}
