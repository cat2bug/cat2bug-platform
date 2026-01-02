package com.cat2bug.ai.service;

import java.util.List;
import com.cat2bug.ai.domain.AiAccount;

/**
 * OpenAI账号Service接口
 * 
 * @author yuzhantao
 * @date 2025-12-28
 */
public interface IAiAccountService 
{
    /**
     * 查询OpenAI账号
     * 
     * @param accountId OpenAI账号主键
     * @return OpenAI账号
     */
    public AiAccount selectAiAccountByAccountId(Long accountId);

    /**
     * 查询OpenAI账号列表
     * 
     * @param aiAccount OpenAI账号
     * @return OpenAI账号集合
     */
    public List<AiAccount> selectAiAccountList(AiAccount aiAccount);

    /**
     * 新增OpenAI账号
     * 
     * @param aiAccount OpenAI账号
     * @return 结果
     */
    public int insertAiAccount(AiAccount aiAccount);

    /**
     * 修改OpenAI账号
     * 
     * @param aiAccount OpenAI账号
     * @return 结果
     */
    public int updateAiAccount(AiAccount aiAccount);

    /**
     * 批量删除OpenAI账号
     * 
     * @param accountIds 需要删除的OpenAI账号主键集合
     * @return 结果
     */
    public int deleteAiAccountByAccountIds(Long[] accountIds);

    /**
     * 删除OpenAI账号信息
     * 
     * @param accountId OpenAI账号主键
     * @return 结果
     */
    public int deleteAiAccountByAccountId(Long accountId);
}
