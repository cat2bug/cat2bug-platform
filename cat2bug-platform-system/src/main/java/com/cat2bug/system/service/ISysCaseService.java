package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.vo.ExcelImportResultVo;

/**
 * 测试用例Service接口
 * 
 * @author yuzhantao
 * @date 2024-01-28
 */
public interface ISysCaseService 
{
    /**
     * 导入用例数据
     *
     * @param caseList 用例数据列表
     * @param projectId 项目id
     * @return 结果
     */
    public ExcelImportResultVo importCase(List<SysCase> caseList, Long projectId);
    /**
     * 查询测试用例
     * 
     * @param caseId 测试用例主键
     * @return 测试用例
     */
    public SysCase selectSysCaseByCaseId(Long caseId);

    /**
     * 查询测试用例
     * @param projectId 项目id
     * @param caseName  测试用例名称
     * @return  测试用例
     */
    public SysCase selectSysCaseByCaseName(Long projectId, String caseName);

    /**
     * 查询测试用例列表
     * 
     * @param sysCase 测试用例
     * @return 测试用例集合
     */
    public List<SysCase> selectSysCaseList(SysCase sysCase);

    /**
     * 新增测试用例
     * 
     * @param sysCase 测试用例
     * @return 结果
     */
    public int insertSysCase(SysCase sysCase);

    /**
     * 修改测试用例
     * 
     * @param sysCase 测试用例
     * @return 结果
     */
    public int updateSysCase(SysCase sysCase);

    /**
     * 批量删除测试用例
     * 
     * @param caseIds 需要删除的测试用例主键集合
     * @return 结果
     */
    public int deleteSysCaseByCaseIds(Long[] caseIds);

    /**
     * 删除测试用例信息
     * 
     * @param caseId 测试用例主键
     * @return 结果
     */
    public int deleteSysCaseByCaseId(Long caseId);
}
