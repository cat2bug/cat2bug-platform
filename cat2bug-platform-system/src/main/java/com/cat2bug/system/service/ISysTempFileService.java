package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysTempFile;

/**
 * 临时文件Service接口
 * 
 * @author yuzhantao
 * @date 2023-12-07
 */
public interface ISysTempFileService 
{
    /**
     * 查询临时文件
     * 
     * @param fileId 临时文件主键
     * @return 临时文件
     */
    public SysTempFile selectSysTempFileByFileId(Long fileId);

    /**
     * 查询临时文件列表
     * 
     * @param sysTempFile 临时文件
     * @return 临时文件集合
     */
    public List<SysTempFile> selectSysTempFileList(SysTempFile sysTempFile);

    /**
     * 新增临时文件
     * 
     * @param sysTempFile 临时文件
     * @return 结果
     */
    public int insertSysTempFile(SysTempFile sysTempFile);

    /**
     * 修改临时文件
     * 
     * @param sysTempFile 临时文件
     * @return 结果
     */
    public int updateSysTempFile(SysTempFile sysTempFile);

    /**
     * 批量删除临时文件
     * 
     * @param fileIds 需要删除的临时文件主键集合
     * @return 结果
     */
    public int deleteSysTempFileByFileIds(Long[] fileIds);

    /**
     * 删除临时文件信息
     * 
     * @param fileId 临时文件主键
     * @return 结果
     */
    public int deleteSysTempFileByFileId(Long fileId);
}
