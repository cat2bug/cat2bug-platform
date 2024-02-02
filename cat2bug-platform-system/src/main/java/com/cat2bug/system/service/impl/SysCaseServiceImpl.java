package com.cat2bug.system.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.bean.BeanValidators;
import com.cat2bug.system.domain.type.SysDefectLogStateEnum;
import com.cat2bug.system.domain.type.SysDefectStateEnum;
import com.cat2bug.system.mapper.SysModuleMapper;
import com.google.common.base.Preconditions;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
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

    @Override
    public SysCase selectSysCaseByCaseName(Long projectId, String caseName) {
        return sysCaseMapper.selectSysCaseByCaseName(projectId,caseName);
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

    /**
     * 导入用例数据
     *
     * @param caseList 用例数据列表
     * @param projectId 项目id
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importCase(List<SysCase> caseList, Long projectId, String operName)
    {
        if (StringUtils.isNull(caseList) || caseList.size() == 0)
        {
            throw new ServiceException(MessageUtils.message("case.import-data-not-empty"));
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (SysCase c : caseList)
        {
            try
            {
                c.setProjectId(projectId);
                sysCaseMapper.insertSysCase(c);
                successNum++;
                successMsg.append("<br/>" + successNum + "、账号 " + c.getCaseName() + " 导入成功");

            } catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + c.getCaseName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }
}
