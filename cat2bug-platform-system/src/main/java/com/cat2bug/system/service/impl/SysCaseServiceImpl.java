package com.cat2bug.system.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.domain.type.SysDefectLogStateEnum;
import com.cat2bug.system.domain.type.SysDefectStateEnum;
import com.cat2bug.system.mapper.SysModuleMapper;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysCaseMapper;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.service.ISysCaseService;

/**
 * 测试用例Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-01-28
 */
@Service
public class SysCaseServiceImpl implements ISysCaseService 
{
    @Autowired
    private SysCaseMapper sysCaseMapper;

    @Autowired
    private SysModuleMapper sysModuleMapper;

    /**
     * 查询测试用例
     * 
     * @param caseId 测试用例主键
     * @return 测试用例
     */
    @Override
    public SysCase selectSysCaseByCaseId(Long caseId)
    {
        return sysCaseMapper.selectSysCaseByCaseId(caseId);
    }

    /**
     * 查询测试用例列表
     * 
     * @param sysCase 测试用例
     * @return 测试用例
     */
    @Override
    public List<SysCase> selectSysCaseList(SysCase sysCase)
    {
        return sysCaseMapper.selectSysCaseList(sysCase);
    }

    /**
     * 新增测试用例
     * 
     * @param sysCase 测试用例
     * @return 结果
     */
    @Override
    public int insertSysCase(SysCase sysCase)
    {
        long count = sysCaseMapper.getCaseMaxNumOfProject(sysCase.getProjectId());
        sysCase.setCaseNum(count+1);
        sysCase.setCreateById(SecurityUtils.getUserId());
        sysCase.setCreateTime(DateUtils.getNowDate());
        sysCase.setUpdateById(SecurityUtils.getUserId());
        sysCase.setUpdateTime(DateUtils.getNowDate());
        return sysCaseMapper.insertSysCase(sysCase);
    }

    /**
     * 修改测试用例
     * 
     * @param sysCase 测试用例
     * @return 结果
     */
    @Override
    public int updateSysCase(SysCase sysCase)
    {
        sysCase.setUpdateTime(DateUtils.getNowDate());
        return sysCaseMapper.updateSysCase(sysCase);
    }

    /**
     * 批量删除测试用例
     * 
     * @param caseIds 需要删除的测试用例主键
     * @return 结果
     */
    @Override
    public int deleteSysCaseByCaseIds(Long[] caseIds)
    {
        return sysCaseMapper.deleteSysCaseByCaseIds(caseIds);
    }

    /**
     * 删除测试用例信息
     * 
     * @param caseId 测试用例主键
     * @return 结果
     */
    @Override
    public int deleteSysCaseByCaseId(Long caseId)
    {
        return sysCaseMapper.deleteSysCaseByCaseId(caseId);
    }
}
