package com.cat2bug.api.service.impl;

import com.cat2bug.api.domain.ApiDefectLog;
import com.cat2bug.api.domain.ApiDefectRequest;
import com.cat2bug.api.domain.ApiMember;
import com.cat2bug.api.domain.type.ApiDefectLogStateEnum;
import com.cat2bug.api.domain.type.ApiDefectStateEnum;
import com.cat2bug.api.domain.type.ApiDefectTypeEnum;
import com.cat2bug.api.mapper.ApiDefectLogMapper;
import com.cat2bug.api.mapper.ApiDefectMapper;
import com.cat2bug.api.mapper.ApiMemberMapper;
import com.cat2bug.api.mapper.ApiReportMapper;
import com.cat2bug.api.service.ApiService;
import com.cat2bug.api.service.IApiReportService;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysReport;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.google.common.base.Preconditions;
import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-13 14:05
 * @Version: 1.0.0
 */
@Service
public class ApiReportServiceImpl implements IApiReportService {
    /** 缺陷描述最大长度 */
    private static final long DEFECT_DESCRIBE_MAX_LEN = 65536;
    /** 缺陷名称最大长度 */
    private static final long DEFECT_NAME_MAX_LEN = 128;

    private ApiReportMapper apiReportMapper;

    private ApiDefectMapper apiDefectMapper;

    private ApiService apiService;

    private ApiMemberMapper apiMemberMapper;

    private ApiDefectLogMapper apiDefectLogMapper;

    @Autowired
    public ApiReportServiceImpl(
            ApiService apiService,
            ApiReportMapper apiReportMapper,
            ApiDefectMapper apiDefectMapper,
            ApiMemberMapper apiMemberMapper,
            ApiDefectLogMapper apiDefectLogMapper) {
        this.apiService = apiService;
        this.apiReportMapper = apiReportMapper;
        this.apiDefectMapper = apiDefectMapper;
        this.apiMemberMapper = apiMemberMapper;
        this.apiDefectLogMapper = apiDefectLogMapper;
    }

    @Transactional
    @Override
    public int pushDefectReport(SysReport<List<ApiDefectRequest>> apiReport) {
        apiReport.setCreateById(SecurityUtils.getUserId());
        apiReport.setCreateTime(DateUtils.getNowDate());
        apiReport.setProjectId(this.apiService.getProjectId());

        int ret;
        if(Strings.isNotBlank(apiReport.getReportKey())) {
            SysReport oldReport = apiReportMapper.findSysReportByReportKey(apiReport.getReportKey());
            if(oldReport!=null) {
                ret = apiReportMapper.updateSysReport(apiReport);
            } else {
                ret = apiReportMapper.insertSysReport(apiReport);
            }
        } else {
            ret = apiReportMapper.insertSysReport(apiReport);
        }

        List<ApiDefectRequest> list = apiReport.getReportData();
        if(ret>0 && list!=null) {
            List<Long> handlerIds = new ArrayList<>();
            if(Strings.isNotBlank(apiReport.getHandler())){
                List<ApiMember> members = this.apiMemberMapper.selectMemberByNames(
                        this.apiService.getProjectId(),
                        Arrays.asList(apiReport.getHandler()));
                Optional<ApiMember> member = members.stream().findFirst();
                if(member.isPresent())
                    handlerIds.add(member.get().getMemberId());
            }
            List<ApiDefectRequest> insertList = list.stream().filter(l->Strings.isBlank(l.getDefectGroupKey()) && Strings.isBlank(l.getDefectKey())).collect(Collectors.toList());
            if(insertList.size()>0) {
                this.insertDefects(handlerIds, insertList);
            }

            List<ApiDefectRequest> updateList = list.stream().filter(l->Strings.isNotBlank(l.getDefectGroupKey()) || Strings.isNotBlank(l.getDefectKey())).collect(Collectors.toList());
            if(updateList.size()>0) {
                this.updateDefects(handlerIds, updateList);
            }
        }
        return ret;
    }

    /**
     * 插入缺陷
     * @param handlerIds    处理人集合
     * @param list          缺陷列表
     */
    void insertDefects(List<Long> handlerIds,List<ApiDefectRequest> list) {
        List<List<ApiDefectRequest>> partition = ListUtils.partition(list,50);
        AtomicLong count = new AtomicLong(apiDefectMapper.getProjectDefectMaxNum(this.apiService.getProjectId()));
        for(List<ApiDefectRequest> l : partition) {
            l.forEach(ll-> {
                ll = setInsertSysDefect(ll,count.incrementAndGet(),handlerIds);
            });
            apiDefectMapper.batchInsertSysDefect(this.apiService.getProjectId(), l);
            batchInertLogs(l);
        }
    }

    ApiDefectRequest setInsertSysDefect(ApiDefectRequest sysDefect, long projectNumber, List<Long> handlerIds) {
        // 自动裁剪缺陷名称长度
        sysDefect.setDefectName(sysDefect.getDefectName().substring(0,(int)Math.min(sysDefect.getDefectName().length(),DEFECT_NAME_MAX_LEN)));
        // 自动裁剪缺陷描述长度
        sysDefect.setDefectDescribe(sysDefect.getDefectDescribe().substring(0,(int)Math.min(sysDefect.getDefectDescribe().length(),DEFECT_DESCRIBE_MAX_LEN)));

        sysDefect.setCreateById(SecurityUtils.getUserId());
        sysDefect.setCreateTime(DateUtils.getNowDate());
//        sysDefect.setDefectNumber(projectNumber);
//        sysDefect.setProjectNum(projectNumber);
        sysDefect.setUpdateTime(DateUtils.getNowDate());
        sysDefect.setUpdateById(SecurityUtils.getUserId());
        sysDefect.setHandleBy(handlerIds);
        if(sysDefect.getDefectType()==null){
            sysDefect.setDefectType(ApiDefectTypeEnum.BUG);
        }
        if (sysDefect.getDefectState() == null) {
            sysDefect.setDefectState(ApiDefectStateEnum.PROCESSING);
        }
        if(Strings.isBlank(sysDefect.getDefectLevel())){
            sysDefect.setDefectLevel("middle");
        } else {
            sysDefect.setDefectLevel(sysDefect.getDefectLevel().toLowerCase());
        }
        return sysDefect;
    }

    void updateDefects(List<Long> handlerIds,List<ApiDefectRequest> list) {
        for(ApiDefectRequest s : list) {
            List<Long> ids = apiDefectMapper.selectDefectIdsByKey(this.apiService.getProjectId(),s.getDefectGroupKey(),s.getDefectKey());
            if(ids.size()==0){
                if(Strings.isBlank(s.getDefectName()) || Strings.isBlank(s.getDefectDescribe())){
                    continue;
                }
                long count = apiDefectMapper.getProjectDefectMaxNum(this.apiService.getProjectId());
                s = setInsertSysDefect(s,count+1,handlerIds);
                apiDefectMapper.insertApiDefect(this.apiService.getProjectId(),s);
                inertLog(s,ApiDefectLogStateEnum.CREATE);
            } else {
                for (Long id : ids) {
                    s.setDefectId(id);
                    s.setUpdateTime(DateUtils.getNowDate());
                    s.setHandleBy(handlerIds);
                    apiDefectMapper.updateApiDefect(s);
                    inertLog(s,ApiDefectLogStateEnum.UPDATE);
                }
            }
        }
    }

    /** 转换日志对象 */
    private ApiDefectLog toLog(ApiDefectRequest sysDefect,ApiDefectLogStateEnum logStateEnum) {
        ApiDefectLog sysDefectLog = new ApiDefectLog();
        sysDefectLog.setDefectId(sysDefect.getDefectId());
        sysDefectLog.setDefectLogDescribe("");
//        sysDefectLog.setReceiveBy(sysDefect.getHandleBy());
        sysDefectLog.setDefectLogState(logStateEnum);
        sysDefectLog.setCreateBy(SecurityUtils.getUserId().toString());
        return sysDefectLog;
    }
    /** 批量添加日志 */
    private void batchInertLogs(List<ApiDefectRequest> list) {
        List<ApiDefectLog> logList = list.stream().map(l->toLog(l,ApiDefectLogStateEnum.CREATE)).collect(Collectors.toList());
        apiDefectLogMapper.batchInsertApiDefectLog(logList);
    }

    /** 添加日志 */
    private ApiDefectLog inertLog(ApiDefectRequest sysDefect,ApiDefectLogStateEnum logStateEnum){
        ApiDefectLog sysDefectLog = toLog(sysDefect,logStateEnum);
        Preconditions.checkNotNull(sysDefectLog.getDefectId(), MessageUtils.message("defect.defect_id_cannot_empty"));
        sysDefectLog.setCreateById(SecurityUtils.getUserId());
        sysDefectLog.setCreateTime(DateUtils.getNowDate());
        return this.apiDefectLogMapper.insertApiDefectLog(sysDefectLog)>0?sysDefectLog:null;
    }
}
