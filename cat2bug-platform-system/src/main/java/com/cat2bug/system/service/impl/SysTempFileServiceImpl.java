package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysTempFileMapper;
import com.cat2bug.system.domain.SysTempFile;
import com.cat2bug.system.service.ISysTempFileService;

/**
 * 临时文件Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-12-07
 */
@Service
public class SysTempFileServiceImpl implements ISysTempFileService 
{
    @Autowired
    private SysTempFileMapper sysTempFileMapper;

    /**
     * 查询临时文件
     * 
     * @param fileId 临时文件主键
     * @return 临时文件
     */
    @Override
    public SysTempFile selectSysTempFileByFileId(Long fileId)
    {
        return sysTempFileMapper.selectSysTempFileByFileId(fileId);
    }

    /**
     * 查询临时文件列表
     * 
     * @param sysTempFile 临时文件
     * @return 临时文件
     */
    @Override
    public List<SysTempFile> selectSysTempFileList(SysTempFile sysTempFile)
    {
        return sysTempFileMapper.selectSysTempFileList(sysTempFile);
    }

    /**
     * 新增临时文件
     * 
     * @param sysTempFile 临时文件
     * @return 结果
     */
    @Override
    public int insertSysTempFile(SysTempFile sysTempFile)
    {
        sysTempFile.setCreateTime(DateUtils.getNowDate());
        return sysTempFileMapper.insertSysTempFile(sysTempFile);
    }

    /**
     * 修改临时文件
     * 
     * @param sysTempFile 临时文件
     * @return 结果
     */
    @Override
    public int updateSysTempFile(SysTempFile sysTempFile)
    {
        return sysTempFileMapper.updateSysTempFile(sysTempFile);
    }

    /**
     * 批量删除临时文件
     * 
     * @param fileIds 需要删除的临时文件主键
     * @return 结果
     */
    @Override
    public int deleteSysTempFileByFileIds(Long[] fileIds)
    {
        return sysTempFileMapper.deleteSysTempFileByFileIds(fileIds);
    }

    /**
     * 删除临时文件信息
     * 
     * @param fileId 临时文件主键
     * @return 结果
     */
    @Override
    public int deleteSysTempFileByFileId(Long fileId)
    {
        return sysTempFileMapper.deleteSysTempFileByFileId(fileId);
    }
}
