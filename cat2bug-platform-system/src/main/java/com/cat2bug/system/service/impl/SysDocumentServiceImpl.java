package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysDocumentMapper;
import com.cat2bug.system.domain.SysDocument;
import com.cat2bug.system.service.ISysDocumentService;

/**
 * 文档Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-06-17
 */
@Service
public class SysDocumentServiceImpl implements ISysDocumentService 
{
    @Autowired
    private SysDocumentMapper sysDocumentMapper;

    /**
     * 查询文档
     * 
     * @param docId 文档主键
     * @return 文档
     */
    @Override
    public SysDocument selectSysDocumentByDocId(Long docId)
    {
        return sysDocumentMapper.selectSysDocumentByDocId(docId);
    }

    /**
     * 查询文档列表
     * 
     * @param sysDocument 文档
     * @return 文档
     */
    @Override
    public List<SysDocument> selectSysDocumentList(SysDocument sysDocument)
    {
        return sysDocumentMapper.selectSysDocumentList(sysDocument);
    }

    /**
     * 新增文档
     * 
     * @param sysDocument 文档
     * @return 结果
     */
    @Override
    public int insertSysDocument(SysDocument sysDocument)
    {
        sysDocument.setCreateTime(DateUtils.getNowDate());
        sysDocument.setCreateById(SecurityUtils.getUserId());
        sysDocument.setUpdateTime(DateUtils.getNowDate());
        sysDocument.setUpdateById(SecurityUtils.getUserId());
        return sysDocumentMapper.insertSysDocument(sysDocument);
    }

    /**
     * 修改文档
     * 
     * @param sysDocument 文档
     * @return 结果
     */
    @Override
    public int updateSysDocument(SysDocument sysDocument)
    {
        sysDocument.setUpdateTime(DateUtils.getNowDate());
        sysDocument.setUpdateById(SecurityUtils.getUserId());
        return sysDocumentMapper.updateSysDocument(sysDocument);
    }

    /**
     * 批量删除文档
     * 
     * @param docIds 需要删除的文档主键
     * @return 结果
     */
    @Override
    public int deleteSysDocumentByDocIds(Long[] docIds)
    {
        return sysDocumentMapper.deleteSysDocumentByDocIds(docIds);
    }

    /**
     * 删除文档信息
     * 
     * @param docId 文档主键
     * @return 结果
     */
    @Override
    public int deleteSysDocumentByDocId(Long docId)
    {
        return sysDocumentMapper.deleteSysDocumentByDocId(docId);
    }
}
