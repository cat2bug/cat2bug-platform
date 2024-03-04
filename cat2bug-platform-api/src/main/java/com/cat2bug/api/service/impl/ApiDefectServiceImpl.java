package com.cat2bug.api.service.impl;

import com.cat2bug.api.domain.ApiDefect;
import com.cat2bug.api.domain.ApiDefectLog;
import com.cat2bug.api.domain.ApiMember;
import com.cat2bug.api.domain.type.ApiDefectLogStateEnum;
import com.cat2bug.api.domain.type.ApiDefectStateEnum;
import com.cat2bug.api.mapper.ApiDefectLogMapper;
import com.cat2bug.api.mapper.ApiDefectMapper;
import com.cat2bug.api.mapper.ApiMemberMapper;
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
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-10 23:34
 * @Version: 1.0.0
 */
@Service
public class ApiDefectServiceImpl implements IApiDefectService {
    /** 缺陷描述最大长度 */
    private static final long DEFECT_DESCRIBE_MAX_LEN = 65536;
    /** 缺陷名称最大长度 */
    private static final long DEFECT_NAME_MAX_LEN = 128;
    private ApiService apiService;
    private ApiDefectMapper apiDefectMapper;
    private ApiDefectLogMapper apiDefectLogMapper;
    private ApiMemberMapper apiMemberMapper;

    @Autowired
    public ApiDefectServiceImpl(ApiService apiService, ApiDefectMapper apiDefectMapper,ApiDefectLogMapper apiDefectLogMapper,ApiMemberMapper apiMemberMapper) {
        this.apiService = apiService;
        this.apiDefectMapper = apiDefectMapper;
        this.apiDefectLogMapper = apiDefectLogMapper;
        this.apiMemberMapper = apiMemberMapper;
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
        Preconditions.checkNotNull(apiDefect.getDefectName(),MessageUtils.message("defect.name_not_empty"));
        Preconditions.checkNotNull(apiDefect.getDefectDescribe(),MessageUtils.message("defect.describe_not_empty"));
        // 自动裁剪缺陷名称长度
        apiDefect.setDefectName(apiDefect.getDefectName().substring(0,(int)Math.min(apiDefect.getDefectName().length(),DEFECT_NAME_MAX_LEN)));
        // 自动裁剪缺陷描述长度
        apiDefect.setDefectDescribe(apiDefect.getDefectDescribe().substring(0,(int)Math.min(apiDefect.getDefectDescribe().length(),DEFECT_DESCRIBE_MAX_LEN)));
        // 缺陷状态设置为进行中
        apiDefect.setDefectState(ApiDefectStateEnum.PROCESSING);
        apiDefect.setUpdateTime(DateUtils.getNowDate());
        apiDefect.setUpdateBy(SecurityUtils.getUsername());
        apiDefect.setUpdateById(SecurityUtils.getUserId());
        // 设置处理人
        if(apiDefect.getHandleByList()!=null && apiDefect.getHandleByList().size()>0) {
            List<ApiMember> members = this.apiMemberMapper.selectMemberByNames(this.getProjectId(), apiDefect.getHandleByList());
            apiDefect.setHandleBy(members.stream().map(m->m.getMemberId()).collect(Collectors.toList()));
        }
        // 根据名称查询缺陷是否存在，存在就更新，不存在就插入
        List<ApiDefect> list = apiDefectMapper.selectApiDefectListByDefectName(this.getProjectId(),apiDefect.getDefectName());
        if(list!=null && list.size()>0) {
            ApiDefect oldApiDefect = list.get(0); // 获取第一条，只更新最新的第一条数据
            apiDefect.setDefectId(oldApiDefect.getDefectId());
            Preconditions.checkState(apiDefectMapper.updateApiDefect(apiDefect)>0, MessageUtils.message("defect.insert_fail"));
        } else {
            apiDefect.setCreateTime(DateUtils.getNowDate());
            apiDefect.setCreateBy(SecurityUtils.getUsername());
            apiDefect.setCreateById(SecurityUtils.getUserId());
            long count = apiDefectMapper.getProjectDefectMaxNum(this.getProjectId());
            apiDefect.setDefectNumber(count+1);
            Preconditions.checkState(apiDefectMapper.insertApiDefect(this.getProjectId(), apiDefect)>0, MessageUtils.message("defect.insert_fail"));
        }

        // 新建日志
        this.inertLog(apiDefect.getDefectId(), apiDefect.getHandleBy(),"",ApiDefectLogStateEnum.CREATE);
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
        sysDefectLog.setCreateBy(SecurityUtils.getUserId().toString());
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
