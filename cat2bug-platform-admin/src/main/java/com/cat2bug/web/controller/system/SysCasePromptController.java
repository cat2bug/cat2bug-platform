package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysCasePrompt;
import com.cat2bug.system.service.ISysCasePromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 测试用例提示词Controller
 * 
 * @author yuzhantao
 * @date 2024-10-15
 */
@RestController
@RequestMapping("/system/CasePrompt")
public class SysCasePromptController extends BaseController
{
    @Autowired
    private ISysCasePromptService sysCasePromptService;

    /**
     * 查询测试用例提示词列表
     */
    @PreAuthorize("@ss.hasPermi('system:case:add')")
    @GetMapping("/list")
    public TableDataInfo list(SysCasePrompt sysCasePrompt)
    {
        startPage();
        List<SysCasePrompt> list = sysCasePromptService.selectSysCasePromptList(sysCasePrompt);
        return getDataTable(list);
    }

//    /**
//     * 导出测试用例提示词列表
//     */
//    @PreAuthorize("@ss.hasPermi('system:case:add')")
//    @Log(title = "测试用例提示词", businessType = BusinessType.EXPORT)
//    @PostMapping("/export")
//    public void export(HttpServletResponse response, SysCasePrompt sysCasePrompt)
//    {
//        List<SysCasePrompt> list = sysCasePromptService.selectSysCasePromptList(sysCasePrompt);
//        ExcelUtil<SysCasePrompt> util = new ExcelUtil<SysCasePrompt>(SysCasePrompt.class);
//        util.exportExcel(response, list, "测试用例提示词数据");
//    }

    /**
     * 获取测试用例提示词详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:case:add')")
    @GetMapping(value = "/{casePromptId}")
    public AjaxResult getInfo(@PathVariable("casePromptId") String casePromptId)
    {
        return success(sysCasePromptService.selectSysCasePromptByCasePromptId(casePromptId));
    }

    /**
     * 新增测试用例提示词
     */
    @PreAuthorize("@ss.hasPermi('system:case:add')")
    @Log(title = "测试用例提示词", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysCasePrompt sysCasePrompt)
    {
        return toAjax(sysCasePromptService.insertSysCasePrompt(sysCasePrompt));
    }

    /**
     * 修改测试用例提示词
     */
    @PreAuthorize("@ss.hasPermi('system:case:add')")
    @Log(title = "测试用例提示词", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysCasePrompt sysCasePrompt)
    {
        return toAjax(sysCasePromptService.updateSysCasePrompt(sysCasePrompt));
    }

    /**
     * 删除测试用例提示词
     */
    @PreAuthorize("@ss.hasPermi('system:case:add')")
    @Log(title = "测试用例提示词", businessType = BusinessType.DELETE)
	@DeleteMapping("/{casePromptIds}")
    public AjaxResult remove(@PathVariable String[] casePromptIds)
    {
        return toAjax(sysCasePromptService.deleteSysCasePromptByCasePromptIds(casePromptIds));
    }
}
