package com.cat2bug.api.service.impl;

import com.cat2bug.api.domain.ApiDefect;
import com.cat2bug.api.domain.ApiDefectLog;
import com.cat2bug.api.domain.type.ApiDefectLogStateEnum;
import com.cat2bug.api.domain.type.ApiDefectStateEnum;
import com.cat2bug.api.mapper.ApiDefectLogMapper;
import com.cat2bug.api.mapper.ApiDefectMapper;
import com.cat2bug.api.service.ApiService;
import com.cat2bug.api.service.IApiDefectService;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-10 23:34
 * @Version: 1.0.0
 */
@Service
public class ApiDefectServiceImpl implements IApiDefectService {
    private ApiService apiService;
    private ApiDefectMapper apiDefectMapper;
    private ApiDefectLogMapper apiDefectLogMapper;
    @Autowired
    public ApiDefectServiceImpl(ApiService apiService, ApiDefectMapper apiDefectMapper,ApiDefectLogMapper apiDefectLogMapper) {
        this.apiService = apiService;
        this.apiDefectMapper = apiDefectMapper;
        this.apiDefectLogMapper = apiDefectLogMapper;
    }
    @Override
    public List<ApiDefect> selectApiDefectList(ApiDefect apiDefect) {
        return this.apiDefectMapper.selectApiDefectList(this.getProjectId(),apiDefect);
    }

    @Override
    public ApiDefect selectSysDefectByDefectNumber(Long number) {
        return this.apiDefectMapper.selectSysDefectByDefectNumber(this.getProjectId(), number);
    }

    @Override
    public ApiDefect insertApiDefect(ApiDefect apiDefect) {
        apiDefect.setCreateTime(DateUtils.getNowDate());
        apiDefect.setUpdateTime(DateUtils.getNowDate());
        apiDefect.setDefectState(ApiDefectStateEnum.PROCESSING);
        apiDefect.setCreateBy(SecurityUtils.getUsername());
        apiDefect.setUpdateBy(SecurityUtils.getUsername());
        apiDefect.setCreateById(SecurityUtils.getUserId());
        apiDefect.setUpdateById(SecurityUtils.getUserId());
        long count = apiDefectMapper.getProjectDefectMaxNum(this.getProjectId());
        apiDefect.setDefectNumber(count+1);
        Preconditions.checkState(apiDefectMapper.insertApiDefect(this.getProjectId(), apiDefect)>0, MessageUtils.message("defect.insert_fail"));
        // 新建日志
        this.inertLog(apiDefect.getDefectId(), new ArrayList<>(),"",ApiDefectLogStateEnum.CREATE);
        return apiDefect;
    }

    private Long getProjectId() {
        return this.apiService.getProjectId();
    }

    /** 添加日志 */
    private ApiDefectLog inertLog(Long defectId, List<Long> receives, String describe, ApiDefectLogStateEnum state){
        ApiDefectLog sysDefectLog = new ApiDefectLog();
        sysDefectLog.setDefectId(defectId);
        sysDefectLog.setDefectLogDescribe(describe);
        sysDefectLog.setReceiveBy(receives);
        sysDefectLog.setDefectLogType(state);
        return this.inertLog(sysDefectLog);
    }

    /** 添加日志 */
    private ApiDefectLog inertLog(ApiDefectLog sysDefectLog){
        Preconditions.checkNotNull(sysDefectLog.getDefectId(),MessageUtils.message("defect.defect_id_cannot_empty"));
        sysDefectLog.setCreateById(SecurityUtils.getUserId());
        sysDefectLog.setCreateTime(DateUtils.getNowDate());
        return this.apiDefectLogMapper.insertApiDefectLog(sysDefectLog)>0?sysDefectLog:null;
    }
}
