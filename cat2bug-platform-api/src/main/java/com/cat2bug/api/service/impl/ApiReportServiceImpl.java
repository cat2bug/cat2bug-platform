package com.cat2bug.api.service.impl;

import com.cat2bug.api.domain.ApiMember;
import com.cat2bug.api.mapper.ApiDefectMapper;
import com.cat2bug.api.mapper.ApiMemberMapper;
import com.cat2bug.api.mapper.ApiReportMapper;
import com.cat2bug.api.service.ApiService;
import com.cat2bug.api.service.IApiReportService;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysReport;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.DictUtils;
import com.cat2bug.common.utils.SecurityUtils;
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
    private ApiReportMapper apiReportMapper;

    private ApiDefectMapper apiDefectMapper;

    private ApiService apiService;

    private ApiMemberMapper apiMemberMapper;

    @Autowired
    public ApiReportServiceImpl(
            ApiService apiService,
            ApiReportMapper apiReportMapper,
            ApiDefectMapper apiDefectMapper,
            ApiMemberMapper apiMemberMapper) {
        this.apiService = apiService;
        this.apiReportMapper = apiReportMapper;
        this.apiDefectMapper = apiDefectMapper;
        this.apiMemberMapper = apiMemberMapper;
    }

    @Transactional
    @Override
    public int pushDefectReport(SysReport<List<SysDefect>> apiReport) {
        apiReport.setCreateById(SecurityUtils.getUserId());
        apiReport.setCreateTime(DateUtils.getNowDate());
        apiReport.setProjectId(this.apiService.getProjectId());
        int ret = apiReportMapper.insertSysReport(apiReport);
        List<SysDefect> list = apiReport.getReportData();
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
            List<List<SysDefect>> partition = ListUtils.partition(list,50);
            AtomicLong count = new AtomicLong(apiDefectMapper.getProjectDefectMaxNum(this.apiService.getProjectId()));
            for(List<SysDefect> l : partition) {
                l.forEach(ll-> {
                    ll.setCreateById(SecurityUtils.getUserId());
                    ll.setCreateTime(DateUtils.getNowDate());
                    ll.setProjectNum(count.incrementAndGet());
                    ll.setUpdateTime(DateUtils.getNowDate());
                    ll.setHandleBy(handlerIds);
                    if(Strings.isBlank(ll.getDefectLevel())){
                        ll.setDefectLevel("middle");
                    }
                });
                apiDefectMapper.batchInsertApiDefect(this.apiService.getProjectId(), l);
            }
        }
        return ret;
    }
}
