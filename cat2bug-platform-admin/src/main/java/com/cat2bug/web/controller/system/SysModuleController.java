package com.cat2bug.web.controller.system;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.service.ISysModuleService;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * 模块Controller
 * 
 * @author yuzhantao
 * @date 2023-11-26
 */
@RestController
@RequestMapping("/system/module")
public class SysModuleController extends BaseController
{
    @Autowired
    private ISysModuleService sysModuleService;

    /**
     * 查询模块列表
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list') || @ss.hasPermi('system:module:list')")
    @GetMapping("/list")
    public AjaxResult list(SysModule sysModule)
    {
        List<SysModule> list = sysModuleService.selectSysModuleList(sysModule);
        return success(list);
    }

    /**
     * 查询模块树形结构
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list') || @ss.hasPermi('system:module:list')")
    @GetMapping("/tree")
    public AjaxResult tree(SysModule sysModule)
    {
        if(sysModule.getModulePid()==0) {
            return success(sysModuleService.selectSysModuleList(sysModule));
        }
        SysModule findParentModule = sysModule;
        List<SysModule> parentModule = new LinkedList<>();
        // 从要获取的交付物往上轮训查询所有关联父交付物
        while(findParentModule!= null) {
            findParentModule = sysModuleService.selectSysModuleByModuleId(findParentModule.getModulePid());
            if(findParentModule!=null) {
                parentModule.add(0, findParentModule);
            }
        }
        System.out.println("parentModule"+ JSON.toJSONString(parentModule));
        // 查出所有关联交付物
        List<SysModule> treeModule = null;          // 交付物树形集合
        List<SysModule> prevLayerModuleList = null; // 上一层的父交付物
        for(SysModule module : parentModule) {
            SysModule params = new SysModule();
            params.setModulePid(module.getModulePid());
            params.setProjectId(sysModule.getProjectId());
            // 查询当前父交付下的所有子交付物
            List<SysModule> children = sysModuleService.selectSysModuleList(params);
            if(treeModule==null) {
                treeModule = children;
            } else if(prevLayerModuleList!=null && children!=null && children.size()>0) {
                // 将子集合设置到父交付物中
                Long parentModuleId = children.get(0).getModulePid();
                Optional<SysModule> prevParentModule = prevLayerModuleList.stream().filter(prevModule->prevModule.getModuleId().equals(parentModuleId)).findFirst();
                if(prevParentModule.isPresent()) {
                    prevParentModule.get().setChildren(children);
                    prevParentModule.get().setChildrenCount(children.size());
                }
            }
            // 记录当前层的父交付物是哪个
            prevLayerModuleList = children;
        }
        return success(treeModule);
    }

    /**
     * 导出模块列表
     */
    @PreAuthorize("@ss.hasPermi('system:module:export')")
    @Log(title = "模块", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysModule sysModule)
    {
        List<SysModule> list = sysModuleService.selectSysModuleList(sysModule);
        ExcelUtil<SysModule> util = new ExcelUtil<SysModule>(SysModule.class);
        util.exportExcel(response, list, "模块数据");
    }

    /**
     * 获取模块详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:module:query')")
    @GetMapping(value = "/{moduleId}")
    public AjaxResult getInfo(@PathVariable("moduleId") Long moduleId)
    {
        return success(sysModuleService.selectSysModuleByModuleId(moduleId));
    }

    /**
     * 新增模块
     */
    @PreAuthorize("@ss.hasPermi('system:module:add')")
    @Log(title = "模块", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysModule sysModule)
    {
        return success(sysModuleService.insertSysModule(sysModule));
    }

    /**
     * 修改模块
     */
    @PreAuthorize("@ss.hasPermi('system:module:edit')")
    @Log(title = "模块", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysModule sysModule)
    {
        return toAjax(sysModuleService.updateSysModule(sysModule));
    }

    /**
     * 删除模块
     */
    @PreAuthorize("@ss.hasPermi('system:module:remove')")
    @Log(title = "模块", businessType = BusinessType.DELETE)
	@DeleteMapping("/{moduleIds}")
    public AjaxResult remove(@PathVariable Long[] moduleIds)
    {
        return toAjax(sysModuleService.deleteSysModuleByModuleIds(moduleIds));
    }
}
