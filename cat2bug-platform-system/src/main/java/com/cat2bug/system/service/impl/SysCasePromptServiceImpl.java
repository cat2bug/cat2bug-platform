package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.uuid.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysCasePromptMapper;
import com.cat2bug.system.domain.SysCasePrompt;
import com.cat2bug.system.service.ISysCasePromptService;

/**
 * 测试用例提示词Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-10-15
 */
@Service
public class SysCasePromptServiceImpl implements ISysCasePromptService 
{
    @Autowired
    private SysCasePromptMapper sysCasePromptMapper;

    /**
     * 查询测试用例提示词
     * 
     * @param casePromptId 测试用例提示词主键
     * @return 测试用例提示词
     */
    @Override
    public SysCasePrompt selectSysCasePromptByCasePromptId(String casePromptId)
    {
        return sysCasePromptMapper.selectSysCasePromptByCasePromptId(casePromptId);
    }

    /**
     * 查询测试用例提示词列表
     * 
     * @param sysCasePrompt 测试用例提示词
     * @return 测试用例提示词
     */
    @Override
    public List<SysCasePrompt> selectSysCasePromptList(SysCasePrompt sysCasePrompt)
    {
        return sysCasePromptMapper.selectSysCasePromptList(sysCasePrompt);
    }

    /**
     * 新增测试用例提示词
     * 
     * @param sysCasePrompt 测试用例提示词
     * @return 结果
     */
    @Override
    public int insertSysCasePrompt(SysCasePrompt sysCasePrompt)
    {
        sysCasePrompt.setCasePromptId(UUID.fastUUID().toString());
        sysCasePrompt.setUpdateById(SecurityUtils.getUserId());
        sysCasePrompt.setCreateTime(DateUtils.getNowDate());
        return sysCasePromptMapper.insertSysCasePrompt(sysCasePrompt);
    }

    /**
     * 修改测试用例提示词
     * 
     * @param sysCasePrompt 测试用例提示词
     * @return 结果
     */
    @Override
    public int updateSysCasePrompt(SysCasePrompt sysCasePrompt)
    {
        return sysCasePromptMapper.updateSysCasePrompt(sysCasePrompt);
    }

    /**
     * 批量删除测试用例提示词
     * 
     * @param casePromptIds 需要删除的测试用例提示词主键
     * @return 结果
     */
    @Override
    public int deleteSysCasePromptByCasePromptIds(String[] casePromptIds)
    {
        return sysCasePromptMapper.deleteSysCasePromptByCasePromptIds(casePromptIds);
    }

    /**
     * 删除测试用例提示词信息
     * 
     * @param casePromptId 测试用例提示词主键
     * @return 结果
     */
    @Override
    public int deleteSysCasePromptByCasePromptId(String casePromptId)
    {
        return sysCasePromptMapper.deleteSysCasePromptByCasePromptId(casePromptId);
    }
}
