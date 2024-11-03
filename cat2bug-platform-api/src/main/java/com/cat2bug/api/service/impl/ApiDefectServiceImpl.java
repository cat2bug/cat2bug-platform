package com.cat2bug.api.service.impl;

import com.cat2bug.api.domain.*;
import com.cat2bug.api.domain.type.ApiDefectLogStateEnum;
import com.cat2bug.api.domain.type.ApiDefectStateEnum;
import com.cat2bug.api.mapper.ApiDefectLogMapper;
import com.cat2bug.api.mapper.ApiDefectMapper;
import com.cat2bug.api.mapper.ApiDeliverableMapper;
import com.cat2bug.api.mapper.ApiMemberMapper;
import com.cat2bug.api.service.ApiService;
import com.cat2bug.api.service.IApiDefectService;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.type.SysDefectLogStateEnum;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    @Resource
    private ApiService apiService;
    @Resource
    private ApiDefectMapper apiDefectMapper;
    @Resource
    private ApiDefectLogMapper apiDefectLogMapper;
    @Resource
    private ApiMemberMapper apiMemberMapper;
    @Resource
    private ApiDeliverableMapper apiDeliverableMapper;

    @Override
    public List<ApiDefect> selectApiDefectList(Long projectId, ApiDefect apiDefect) {
        // 如果等级存在，转换一下全小写
        if(StringUtils.isNotBlank(apiDefect.getDefectLevel())){
            apiDefect.setDefectLevel(apiDefect.getDefectLevel().toLowerCase());
        }
        return this.apiDefectMapper.selectApiDefectList(projectId, apiDefect).stream().map(d->{
            // 等级转换为大写输出
            if(StringUtils.isNotBlank(d.getDefectLevel())){
                d.setDefectLevel(d.getDefectLevel().toUpperCase());
            }
            return d;
        }).collect(Collectors.toList());
    }

    @Override
    public ApiDefect selectSysDefectByDefectNumber(Long number) {
        ApiDefect apiDefect = this.apiDefectMapper.selectSysDefectByDefectNumber(this.getProjectId(), number);
        if(StringUtils.isNotBlank(apiDefect.getDefectLevel())){
            apiDefect.setDefectLevel(apiDefect.getDefectLevel().toUpperCase());
        }
        return apiDefect;
    }

    @Override
    @Transactional
    public ApiDefect insertOrUpdateApiDefect(ApiDefectRequest apiDefect) {
        Preconditions.checkNotNull(apiDefect.getDefectName(),MessageUtils.message("defect.name_not_empty"));
//        Preconditions.checkNotNull(apiDefect.getDefectDescribe(),MessageUtils.message("defect.describe_not_empty"));
        // 自动裁剪缺陷名称长度
        apiDefect.setDefectName(apiDefect.getDefectName().substring(0,(int)Math.min(apiDefect.getDefectName().length(),DEFECT_NAME_MAX_LEN)));
        // 自动裁剪缺陷描述长度
        apiDefect.setDefectDescribe(apiDefect.getDefectDescribe().substring(0,(int)Math.min(apiDefect.getDefectDescribe().length(),DEFECT_DESCRIBE_MAX_LEN)));

        // 如果等级存在，转换一下全小写
        if(StringUtils.isNotBlank(apiDefect.getDefectLevel())){
            apiDefect.setDefectLevel(apiDefect.getDefectLevel().toLowerCase());
        }

        // 缺陷状态设置为进行中
        apiDefect.setDefectState(ApiDefectStateEnum.PROCESSING);
        apiDefect.setUpdateTime(DateUtils.getNowDate());
        apiDefect.setUpdateBy(SecurityUtils.getUsername());
        apiDefect.setUpdateById(SecurityUtils.getUserId());
        // 设置处理人
        if(apiDefect.getHandlerAccountList()!=null && apiDefect.getHandlerAccountList().size()>0) {
            List<ApiMember> members = this.apiMemberMapper.selectMemberByNames(
                    this.getProjectId(),
                    apiDefect.getHandlerAccountList());
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
            apiDefect.setDefectNum(count+1);
            Preconditions.checkState(apiDefectMapper.insertApiDefect(this.getProjectId(), apiDefect)>0, MessageUtils.message("defect.insert_fail"));
        }

        // 新建日志
        this.inertLog(apiDefect.getDefectId(), apiDefect.getHandleBy(),"",ApiDefectLogStateEnum.CREATE);
        return apiDefect;
    }

    /** 添加缺陷 */
    @Override
    public ApiDefect insertApiDefect(ApiDefectRequest apiDefect) {
        // 数据检测
        Preconditions.checkNotNull(apiDefect.getDefectName(),MessageUtils.message("defect.name_not_empty"));
        Preconditions.checkNotNull(apiDefect.getDefectType(),MessageUtils.message("defect.type_not_empty"));
        Preconditions.checkNotNull(apiDefect.getDefectLevel(),MessageUtils.message("defect.level_not_empty"));
        Preconditions.checkArgument(apiDefect.getDefectName().length()<=DEFECT_NAME_MAX_LEN,
                MessageUtils.message("defect.name_size_too_length",DEFECT_NAME_MAX_LEN ));
        Preconditions.checkArgument(apiDefect.getDefectDescribe().length()<=DEFECT_DESCRIBE_MAX_LEN,
                MessageUtils.message("defect.describe_size_too_length",DEFECT_DESCRIBE_MAX_LEN ));
        Preconditions.checkNotNull(apiDefect.getHandlerAccountList(),MessageUtils.message("defect.handler_not_empty"));
        Preconditions.checkArgument(apiDefect.getHandlerAccountList().size()>0,
                MessageUtils.message("defect.handler_not_empty"));

        // 如果等级存在，转换一下全小写
        if(StringUtils.isNotBlank(apiDefect.getDefectLevel())){
            apiDefect.setDefectLevel(apiDefect.getDefectLevel().toLowerCase());
        }
        // 缺陷状态设置为进行中
        apiDefect.setDefectState(ApiDefectStateEnum.PROCESSING);
        apiDefect.setUpdateTime(DateUtils.getNowDate());
        apiDefect.setUpdateBy(SecurityUtils.getUsername());
        apiDefect.setUpdateById(SecurityUtils.getUserId());
        // 设置处理人
        if(apiDefect.getHandlerAccountList()!=null && apiDefect.getHandlerAccountList().size()>0) {
            List<ApiMember> members = this.apiMemberMapper.selectMemberByNames(
                    this.getProjectId(),
                    apiDefect.getHandlerAccountList());
            if(members.size() != apiDefect.getHandlerAccountList().size()) {
                throw new RuntimeException(MessageUtils.message("api.insert-handler-non-existent"));
            }
            apiDefect.setHandleBy(members.stream().map(m->m.getMemberId()).collect(Collectors.toList()));
        }

        apiDefect.setCreateTime(DateUtils.getNowDate());
        apiDefect.setCreateBy(SecurityUtils.getUsername());
        apiDefect.setCreateById(SecurityUtils.getUserId());
        // 设置缺陷编号
        Long projectId = this.getProjectId();
        long count = apiDefectMapper.getProjectDefectMaxNum(projectId);
        apiDefect.setDefectNum(++count);
        Preconditions.checkState(apiDefectMapper.insertApiDefect(projectId, apiDefect)>0, MessageUtils.message("defect.insert_fail"));

        // 新建日志
        this.inertLog(apiDefect.getDefectId(), apiDefect.getHandleBy(),"",ApiDefectLogStateEnum.CREATE);
        // 查找新建的缺陷并返回
        ApiDefect retDefect = this.apiDefectMapper.selectSysDefectByDefectNumber(projectId, count);
        if(StringUtils.isNotBlank(retDefect.getDefectLevel())){
            retDefect.setDefectLevel(retDefect.getDefectLevel().toUpperCase());
        }
        return retDefect;
    }

    /**
     * 指派
     * @param apiDefectHandle 缺陷处理
     * @return  缺陷
     */
    @Transactional
    @Override
    public ApiDefect assign(ApiDefectHandle apiDefectHandle) {
        Preconditions.checkArgument(
                apiDefectHandle.getHandlerAccountList()!=null &&
                apiDefectHandle.getHandlerAccountList().size()>0,
                MessageUtils.message("defect.handler_not_empty"));
        return this.updateDefectHandle(apiDefectHandle,
                null,
                ApiDefectLogStateEnum.ASSIGN,
                "defect.assign_fail");
    }

    /**
     * 驳回
     * @param apiDefectHandle  缺陷处理
     * @return 缺陷
     */
    @Override
    public ApiDefect reject(ApiDefectHandle apiDefectHandle) {
        Preconditions.checkNotNull(apiDefectHandle.getDescribe(),MessageUtils.message("defect.describe_not_empty"));

        return this.updateDefectHandle(apiDefectHandle,
                ApiDefectStateEnum.REJECTED,
                ApiDefectLogStateEnum.REJECTED,
                "defect.reject_fail");
    }

    /**
     * 修复
     * @param apiDefectHandle  缺陷处理
     * @return 缺陷
     */
    @Override
    public ApiDefect repair(ApiDefectHandle apiDefectHandle) {
        Preconditions.checkArgument(
                apiDefectHandle.getHandlerAccountList()!=null &&
                        apiDefectHandle.getHandlerAccountList().size()>0,
                MessageUtils.message("defect.handler_not_empty"));
        return this.updateDefectHandle(apiDefectHandle,
                ApiDefectStateEnum.AUDIT,
                ApiDefectLogStateEnum.REPAIR,
                "defect.repair_fail");
    }

    /**
     * 通过
     * @param apiDefectHandle  缺陷处理
     * @return
     */
    @Override
    public ApiDefect pass(ApiDefectHandle apiDefectHandle) {
        return this.updateDefectHandle(apiDefectHandle,
                ApiDefectStateEnum.CLOSED,
                ApiDefectLogStateEnum.PASS,
                "defect.pass_fail");
    }

    @Override
    public ApiDefect close(ApiDefectHandle apiDefectHandle) {
        Preconditions.checkNotNull(apiDefectHandle.getDescribe(),MessageUtils.message("defect.describe_not_empty"));
        return this.updateDefectHandle(apiDefectHandle,
                ApiDefectStateEnum.CLOSED,
                ApiDefectLogStateEnum.CLOSED,
                "defect.close_fail");
    }

    @Override
    public ApiDefect open(ApiDefectHandle apiDefectHandle) {
        Preconditions.checkNotNull(apiDefectHandle.getDescribe(),MessageUtils.message("defect.describe_not_empty"));
        return this.updateDefectHandle(apiDefectHandle,
                ApiDefectStateEnum.PROCESSING,
                ApiDefectLogStateEnum.OPEN,
                "defect.open_fail");
    }

    /**
     * 根据接口传回的api-key获取对应项目ID
     * @return 项目ID
     */
    private Long getProjectId() {
        return this.apiService.getProjectId();
    }

    /**
     * 更新缺陷操作
     * @param apiDefectHandle   更新参数
     * @param apiDefectLogStateEnum 更新状态
     * @param exceptionMessageKey   如果插入失败的异常信息i18n关键字
     * @return 更新后的缺陷信息
     */
    private ApiDefect updateDefectHandle(ApiDefectHandle apiDefectHandle, ApiDefectStateEnum apiDefectStateEnum, ApiDefectLogStateEnum apiDefectLogStateEnum, String exceptionMessageKey) {
        Long projectId = this.getProjectId();
        ApiDefect apiDefect = this.apiDefectMapper.selectSysDefectByDefectNumber(
                projectId, apiDefectHandle.getDefectNum()
        );
        Preconditions.checkNotNull(apiDefect, MessageUtils.message("defect.not_found"));
        // 更新缺陷
        ApiDefectRequest sd = new ApiDefectRequest();
        sd.setDefectId(apiDefect.getDefectId());
        if(apiDefectStateEnum!=null) {
            sd.setDefectState(apiDefectStateEnum);
        }
        if(StringUtils.isNotBlank(apiDefectHandle.getDescribe())){
            sd.setDefectDescribe(apiDefectHandle.getDescribe());
        }
        // 设置处理人
        if(apiDefectHandle.getHandlerAccountList()!=null && apiDefectHandle.getHandlerAccountList().size()>0) {
            List<ApiMember> members = this.apiMemberMapper.selectMemberByNames(
                    this.getProjectId(),
                    apiDefectHandle.getHandlerAccountList());
            if(members.size() != apiDefectHandle.getHandlerAccountList().size()) {
                throw new RuntimeException(MessageUtils.message("api.insert-handler-non-existent"));
            }
            sd.setHandleBy(members.stream().map(m->m.getMemberId()).collect(Collectors.toList()));
        }
        int count = this.apiDefectMapper.updateApiDefect(sd);
        Preconditions.checkArgument(count>0, MessageUtils.message(exceptionMessageKey));

        // 插入日志
        this.inertLog(apiDefect.getDefectId(), sd.getHandleBy(), apiDefectHandle.getDescribe(), ApiDefectLogStateEnum.ASSIGN);

        // 查找新建的缺陷并返回
        ApiDefect retDefect = this.apiDefectMapper.selectSysDefectByDefectNumber(
                projectId, apiDefectHandle.getDefectNum()
        );
        if(StringUtils.isNotBlank(retDefect.getDefectLevel())){
            retDefect.setDefectLevel(retDefect.getDefectLevel().toUpperCase());
        }
        return retDefect;
    }


    /** 添加日志 */
    private ApiDefectLog inertLog(Long defectId, List<Long> receives, String describe, ApiDefectLogStateEnum state){
        ApiDefectLog sysDefectLog = new ApiDefectLog();
        sysDefectLog.setDefectId(defectId);
        sysDefectLog.setDefectLogDescribe(describe);
//        sysDefectLog.setReceiveBy(receives);
        sysDefectLog.setDefectLogState(state);
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
