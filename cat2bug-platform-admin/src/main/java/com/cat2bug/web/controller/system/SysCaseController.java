package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysCaseExcelTepmplate;
import com.cat2bug.system.domain.vo.ExcelImportResultVo;
import com.cat2bug.system.service.ISysCaseService;
import com.cat2bug.system.service.ISysModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 测试用例Controller
 * 
 * @author yuzhantao
 * @date 2024-01-28
 */
@RestController
@RequestMapping("/system/case")
public class SysCaseController extends BaseController
{
    @Autowired
    private ISysCaseService sysCaseService;
    @Autowired
    private ISysModuleService sysModuleService;

    /**
     * 查询测试用例列表
     */
    @PreAuthorize("@ss.hasPermi('system:case:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysCase sysCase)
    {
        if(sysCase.getParams()!=null && sysCase.getParams().get("modulePid") != null) {
            Long pid = Long.parseLong(String.valueOf(sysCase.getParams().get("modulePid")));
            Set<Long> moduleIds = sysModuleService.getAllChildIds(pid);
            if(sysCase.getParams()==null){
                sysCase.setParams(new HashMap<>());
            }
            moduleIds.add(pid);
            sysCase.getParams().put("moduleIds",moduleIds);
        }
        startPage();
        List<SysCase> list = sysCaseService.selectSysCaseList(sysCase);
        return getDataTable(list);
    }

    /**
     * 导出测试用例列表
     */
    @PreAuthorize("@ss.hasPermi('system:case:export')")
    @Log(title = "测试用例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysCase sysCase)
    {
        List<SysCase> list = sysCaseService.selectSysCaseList(sysCase);
        ExcelUtil<SysCase> util = new ExcelUtil<SysCase>(SysCase.class);
        util.exportExcel(response, list, "测试用例数据");
    }

    /**
     * 导入数据
     * @param file      文件
     * @param projectId 项目id
     * @return
     * @throws Exception
     */
    @Log(title = "测试用例", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:case:import')")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, Long projectId) throws Exception
    {
        ExcelUtil<SysCase> util = new ExcelUtil<SysCase>(SysCase.class);
        List<SysCase> caseList = util.importExcel(file.getInputStream());
        ExcelImportResultVo message = sysCaseService.importCase(caseList, projectId);
        return success(message);
    }

    /**
     * 下载导入模版
     * @param response  反馈对象
     * @param projectId 项目id
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response,Long projectId)
    {
        ExcelUtil<SysCase> util = new ExcelUtil<SysCase>(SysCase.class);
        util.importTemplateExcel(response, MessageUtils.message("case.test_case"));
    }

    /**
     * 获取测试用例详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:case:query')")
    @GetMapping(value = "/{caseId}")
    public AjaxResult getInfo(@PathVariable("caseId") Long caseId)
    {
        return success(sysCaseService.selectSysCaseByCaseId(caseId));
    }

    /**
     * 新增测试用例
     */
    @PreAuthorize("@ss.hasPermi('system:case:add')")
    @Log(title = "测试用例", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysCase sysCase)
    {
        return success(sysCaseService.insertSysCase(sysCase));
    }

    /**
     * 新增测试用例
     */
    @PreAuthorize("@ss.hasPermi('system:case:add')")
    @Log(title = "测试用例", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batchAdd(@RequestBody List<SysCase> list)
    {
        return success(sysCaseService.batchInsertSysCase(list));
    }

    /**
     * 修改测试用例
     */
    @PreAuthorize("@ss.hasPermi('system:case:edit')")
    @Log(title = "测试用例", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysCase sysCase)
    {
        return toAjax(sysCaseService.updateSysCase(sysCase));
    }

    /**
     * 删除测试用例
     */
    @PreAuthorize("@ss.hasPermi('system:case:remove')")
    @Log(title = "测试用例", businessType = BusinessType.DELETE)
	@DeleteMapping("/{caseIds}")
    public AjaxResult remove(@PathVariable Long[] caseIds)
    {
        return toAjax(sysCaseService.deleteSysCaseByCaseIds(caseIds));
    }
}
