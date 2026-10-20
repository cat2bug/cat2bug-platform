package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDocument;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysDocumentMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * 按 id 删除；文件夹（docType=0）会递归收集子孙后一并删除，与前端「删除文件夹含其下全部」文案一致。
     */
    @Transactional
    public int deleteSysDocumentByDocIds(Long[] docIds) {
        if (docIds == null || docIds.length == 0) {
            throw new ApiException("文档ID不能为空");
        }
        Set<Long> cascadeIds = new LinkedHashSet<>();
        for (Long docId : docIds) {
            collectCascadeDeleteIds(docId, cascadeIds);
        }
        if (cascadeIds.isEmpty()) {
            throw new ApiException("文档ID不能为空");
        }
        return sysDocumentMapper.deleteSysDocumentByDocIds(cascadeIds.toArray(new Long[0]));
    }

    /** 收集待删 id：文件只含自身；文件夹含自身及全部子孙。 */
    void collectCascadeDeleteIds(Long docId, Set<Long> out) {
        if (docId == null || out.contains(docId)) {
            return;
        }
        SysDocument doc = sysDocumentMapper.selectSysDocumentByDocId(docId);
        if (doc == null) {
            out.add(docId);
            return;
        }
        out.add(docId);
        if (doc.getDocType() != null && doc.getDocType() == 0L) {
            SysDocument query = new SysDocument();
            query.setDocPid(docId);
            query.setProjectId(doc.getProjectId());
            for (SysDocument child : selectDocumentListAll(query)) {
                collectCascadeDeleteIds(child.getDocId(), out);
            }
        }
    }

    private boolean checkDuplication(SysDocument sysDocument) {
        // 仅改父夹等局部字段、未带名称时不做同名校验
        if (sysDocument.getDocName() == null || sysDocument.getDocName().isEmpty()) {
            return false;
        }
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
