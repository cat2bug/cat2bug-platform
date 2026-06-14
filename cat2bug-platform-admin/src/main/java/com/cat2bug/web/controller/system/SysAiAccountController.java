package com.cat2bug.web.controller.system;

import com.cat2bug.ai.domain.AiAccount;
import com.cat2bug.ai.service.IAiAccountService;
import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.web.excel.ExcelHttpSupport;
import com.cat2bug.web.service.excel.SystemExcelExportService;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * OpenAI账号Controller
 * 
 * @author yuzhantao
 * @date 2025-12-28
 */
@RestController
@RequestMapping("/system/ai/account")
public class SysAiAccountController extends BaseController
{
    @Autowired
    private IAiAccountService aiAccountService;

    @Autowired
    private SystemExcelExportService systemExcelExportService;

    /**
     * 查询OpenAI账号列表
     */
    @PreAuthorize("@ss.hasPermi('ai:account:list') || @ss.hasPermi('system:case:add')")
    @GetMapping("/list")
    public TableDataInfo list(AiAccount aiAccount)
    {
        startPage();
        List<AiAccount> list = aiAccountService.selectAiAccountList(aiAccount);
        return getDataTable(list);
    }

    /**
     * 导出OpenAI账号列表
     */
    @PreAuthorize("@ss.hasPermi('ai:account:export')")
    @Log(title = "OpenAI账号", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiAccount aiAccount) throws IOException
    {
        ExcelHttpSupport.write(response, systemExcelExportService.exportAiAccounts(aiAccount), "OpenAI账号数据.xlsx");
    }

    /**
     * 连通性测试：使用账号存储的地址、密钥与模型发起最小对话请求
     */
    @PreAuthorize("@ss.hasPermi('ai:account:edit')")
    @PostMapping("/test/{accountId}")
    public AjaxResult testConnection(@PathVariable("accountId") Long accountId)
    {
        return success(aiAccountService.testOpenAiConnection(accountId));
    }

    /**
     * 获取OpenAI账号详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:account:query')")
    @GetMapping(value = "/{accountId}")
    public AjaxResult getInfo(@PathVariable("accountId") Long accountId)
    {
        return success(aiAccountService.selectAiAccountByAccountId(accountId));
    }

    /**
     * 新增OpenAI账号
     */
    @PreAuthorize("@ss.hasPermi('ai:account:add')")
    @Log(title = "OpenAI账号", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiAccount aiAccount)
    {
        return toAjax(aiAccountService.insertAiAccount(aiAccount));
    }

    /**
     * 修改OpenAI账号
     */
    @PreAuthorize("@ss.hasPermi('ai:account:edit')")
    @Log(title = "OpenAI账号", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiAccount aiAccount)
    {
        return toAjax(aiAccountService.updateAiAccount(aiAccount));
    }

    /**
     * 删除OpenAI账号
     */
    @PreAuthorize("@ss.hasPermi('ai:account:remove')")
    @Log(title = "OpenAI账号", businessType = BusinessType.DELETE)
	@DeleteMapping("/{accountIds}")
    public AjaxResult remove(@PathVariable Long[] accountIds)
    {
        return toAjax(aiAccountService.deleteAiAccountByAccountIds(accountIds));
    }
}
