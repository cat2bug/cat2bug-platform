package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysCasePrompt;

/**
 * 测试用例提示词Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-10-15
 */
public interface SysCasePromptMapper 
{
    /**
     * 查询测试用例提示词
     * 
     * @param casePromptId 测试用例提示词主键
     * @return 测试用例提示词
     */
    public SysCasePrompt selectSysCasePromptByCasePromptId(String casePromptId);

    /**
     * 查询测试用例提示词列表
     * 
     * @param sysCasePrompt 测试用例提示词
     * @return 测试用例提示词集合
     */
    public List<SysCasePrompt> selectSysCasePromptList(SysCasePrompt sysCasePrompt);

    /**
     * 新增测试用例提示词
     * 
     * @param sysCasePrompt 测试用例提示词
     * @return 结果
     */
    public int insertSysCasePrompt(SysCasePrompt sysCasePrompt);

    /**
     * 修改测试用例提示词
     * 
     * @param sysCasePrompt 测试用例提示词
     * @return 结果
     */
    public int updateSysCasePrompt(SysCasePrompt sysCasePrompt);

    /**
     * 删除测试用例提示词
     * 
     * @param casePromptId 测试用例提示词主键
     * @return 结果
     */
    public int deleteSysCasePromptByCasePromptId(String casePromptId);

    /**
     * 批量删除测试用例提示词
     * 
     * @param casePromptIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysCasePromptByCasePromptIds(String[] casePromptIds);
}
