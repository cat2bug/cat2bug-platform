package com.cat2bug.ai.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.ai.mapper.AiAccountMapper;
import com.cat2bug.ai.domain.AiAccount;
import com.cat2bug.ai.service.IAiAccountService;

/**
 * OpenAI账号Service业务层处理
 * 
 * @author yuzhantao
 * @date 2025-12-28
 */
@Service
public class AiAccountServiceImpl implements IAiAccountService 
{
    @Autowired
    private AiAccountMapper aiAccountMapper;

    /**
     * 查询OpenAI账号
     * 
     * @param accountId OpenAI账号主键
     * @return OpenAI账号
     */
    @Override
    public AiAccount selectAiAccountByAccountId(Long accountId)
    {
        return aiAccountMapper.selectAiAccountByAccountId(accountId);
    }

    /**
     * 查询OpenAI账号列表
     * 
     * @param aiAccount OpenAI账号
     * @return OpenAI账号
     */
    @Override
    public List<AiAccount> selectAiAccountList(AiAccount aiAccount)
    {
        return aiAccountMapper.selectAiAccountList(aiAccount);
    }

    /**
     * 新增OpenAI账号
     * 
     * @param aiAccount OpenAI账号
     * @return 结果
     */
    @Override
    public int insertAiAccount(AiAccount aiAccount)
    {
        return aiAccountMapper.insertAiAccount(aiAccount);
    }

    /**
     * 修改OpenAI账号
     * 
     * @param aiAccount OpenAI账号
     * @return 结果
     */
    @Override
    public int updateAiAccount(AiAccount aiAccount)
    {
        return aiAccountMapper.updateAiAccount(aiAccount);
    }

    /**
     * 批量删除OpenAI账号
     * 
     * @param accountIds 需要删除的OpenAI账号主键
     * @return 结果
     */
    @Override
    public int deleteAiAccountByAccountIds(Long[] accountIds)
    {
        return aiAccountMapper.deleteAiAccountByAccountIds(accountIds);
    }

    /**
     * 删除OpenAI账号信息
     * 
     * @param accountId OpenAI账号主键
     * @return 结果
     */
    @Override
    public int deleteAiAccountByAccountId(Long accountId)
    {
        return aiAccountMapper.deleteAiAccountByAccountId(accountId);
    }
}
