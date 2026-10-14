package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysDocument;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.framework.security.AuthContext;
import com.cat2bug.quarkus.system.mapper.SysDocumentMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;

@ApplicationScoped
public class SysDocumentService {

    @Inject
    SysDocumentMapper sysDocumentMapper;

    @Inject
    AuthContext authContext;

    public TableDataInfo selectDocumentPage(SysDocument query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysDocumentMapper.countSysDocumentList(query);
        List<SysDocument> rows = sysDocumentMapper.selectSysDocumentList(query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysDocument> selectDocumentListAll(SysDocument query) {
        long total = sysDocumentMapper.countSysDocumentList(query);
        if (total <= 0) {
            return List.of();
        }
        int limit = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        return sysDocumentMapper.selectSysDocumentList(query, 0, limit);
    }

    public SysDocument selectSysDocumentByDocId(Long docId) {
        return sysDocumentMapper.selectSysDocumentByDocId(docId);
    }

    @Transactional
    public int insertSysDocument(SysDocument sysDocument) {
        if (checkDuplication(sysDocument)) {
            throw new ApiException("文档名称已存在");
        }
        LoginUser loginUser = requireLoginUser();
        Date now = new Date();
        sysDocument.setCreateTime(now);
        sysDocument.setCreateById(loginUser.getUserId());
        sysDocument.setUpdateTime(now);
        sysDocument.setUpdateById(loginUser.getUserId());
        return sysDocumentMapper.insertSysDocument(sysDocument);
    }

    @Transactional
    public int updateSysDocument(SysDocument sysDocument) {
        if (checkDuplication(sysDocument)) {
            throw new ApiException("文档名称已存在");
        }
        LoginUser loginUser = requireLoginUser();
        sysDocument.setUpdateTime(new Date());
        sysDocument.setUpdateById(loginUser.getUserId());
        return sysDocumentMapper.updateSysDocument(sysDocument);
    }

    @Transactional
    public int deleteSysDocumentByDocIds(Long[] docIds) {
        if (docIds == null || docIds.length == 0) {
            throw new ApiException("文档ID不能为空");
        }
        return sysDocumentMapper.deleteSysDocumentByDocIds(docIds);
    }

    private boolean checkDuplication(SysDocument sysDocument) {
        SysDocument query = new SysDocument();
        query.setDocId(sysDocument.getDocId());
        query.setExactDocName(sysDocument.getDocName());
        query.setProjectId(sysDocument.getProjectId());
        query.setDocPid(sysDocument.getDocPid());
        return sysDocumentMapper.countSysDocumentList(query) > 0;
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
