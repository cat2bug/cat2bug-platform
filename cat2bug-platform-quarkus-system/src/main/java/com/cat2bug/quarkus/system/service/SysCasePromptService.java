package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysCasePrompt;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.framework.security.AuthContext;
import com.cat2bug.quarkus.system.mapper.SysCasePromptMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SysCasePromptService {

    @Inject
    SysCasePromptMapper sysCasePromptMapper;

    @Inject
    AuthContext authContext;

    public TableDataInfo selectCasePromptPage(SysCasePrompt query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysCasePromptMapper.countSysCasePromptList(query);
        List<SysCasePrompt> rows = sysCasePromptMapper.selectSysCasePromptList(
                query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public SysCasePrompt selectSysCasePromptByCasePromptId(String casePromptId) {
        return sysCasePromptMapper.selectSysCasePromptByCasePromptId(casePromptId);
    }

    public List<SysCasePrompt> selectSysCasePromptList(SysCasePrompt query) {
        PageDomain page = PageDomain.of(1, Integer.MAX_VALUE);
        return sysCasePromptMapper.selectSysCasePromptList(query, page.getOffset(), page.getPageSize());
    }

    @Transactional
    public int insertSysCasePrompt(SysCasePrompt sysCasePrompt) {
        LoginUser loginUser = requireLoginUser();
        sysCasePrompt.setCasePromptId(UUID.randomUUID().toString());
        sysCasePrompt.setCreateById(loginUser.getUserId());
        sysCasePrompt.setCreateTime(new Date());
        return sysCasePromptMapper.insertSysCasePrompt(sysCasePrompt);
    }

    @Transactional
    public int updateSysCasePrompt(SysCasePrompt sysCasePrompt) {
        return sysCasePromptMapper.updateSysCasePrompt(sysCasePrompt);
    }

    @Transactional
    public int deleteSysCasePromptByCasePromptIds(String[] casePromptIds) {
        if (casePromptIds == null || casePromptIds.length == 0) {
            throw new ApiException("提示词ID不能为空");
        }
        return sysCasePromptMapper.deleteSysCasePromptByCasePromptIds(casePromptIds);
    }

    @Transactional
    public int deleteSysCasePromptByCasePromptId(String casePromptId) {
        return sysCasePromptMapper.deleteSysCasePromptByCasePromptId(casePromptId);
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
