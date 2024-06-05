package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysCase;
import org.apache.ibatis.annotations.Param;

/**
 * 测试用例Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-01-28
 */
public interface SysCaseMapper 
{
    /**
     * 获取项目中的用例最大数量
     * @param projectId
     * @return
     */

    public long getCaseMaxNumOfProject(Long projectId);

    /**
     * 获取项目中的用例数量
     * @param projectId
     * @return
     */
    public long totalByProjectId(Long projectId);

    /**
     * 查询测试用例
     * @param projectId 项目id
     * @param caseName  测试用例名称
     * @return  测试用例
     */
    public SysCase selectSysCaseByCaseName(@Param("projectId") Long projectId,@Param("caseName") String caseName);

    /**
     * 查询测试用例
     * 
     * @param caseId 测试用例主键
     * @return 测试用例
     */
    public SysCase selectSysCaseByCaseId(Long caseId);

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
     * 删除测试用例
     * 
     * @param caseId 测试用例主键
     * @return 结果
     */
    public int deleteSysCaseByCaseId(Long caseId);

    /**
     * 批量删除测试用例
     * 
     * @param caseIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysCaseByCaseIds(Long[] caseIds);
}
