package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysDocument;

/**
 * 文档Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-06-17
 */
public interface SysDocumentMapper 
{
    /**
     * 查询文档
     * 
     * @param docId 文档主键
     * @return 文档
     */
    public SysDocument selectSysDocumentByDocId(Long docId);

    /**
     * 查询文档列表
     * 
     * @param sysDocument 文档
     * @return 文档集合
     */
    public List<SysDocument> selectSysDocumentList(SysDocument sysDocument);

    /**
     * 新增文档
     * 
     * @param sysDocument 文档
     * @return 结果
     */
    public int insertSysDocument(SysDocument sysDocument);

    /**
     * 修改文档
     * 
     * @param sysDocument 文档
     * @return 结果
     */
    public int updateSysDocument(SysDocument sysDocument);

    /**
     * 删除文档
     * 
     * @param docId 文档主键
     * @return 结果
     */
    public int deleteSysDocumentByDocId(Long docId);

    /**
     * 批量删除文档
     * 
     * @param docIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysDocumentByDocIds(Long[] docIds);
}
