package com.cat2bug.web.controller.admin;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.framework.web.service.SysLoginService;
import com.cat2bug.framework.web.service.SysPermissionService;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.domain.SysTeam;
import com.cat2bug.system.service.ISysProjectService;
import com.cat2bug.system.service.ISysRoleService;
import com.cat2bug.system.service.ISysUserConfigService;
import com.cat2bug.system.service.ISysUserProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目Controller
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
@RestController
@RequestMapping("/admin/project")
public class AdminProjectController extends BaseController
{
    @Autowired
    private ISysProjectService sysProjectService;
    /**
     * 查询项目列表
     */
    @PreAuthorize("@ss.hasPermi('admin:project:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysProject sysProject)
    {
        startPage();
        List<SysProject> list = sysProjectService.selectSysProjectList(sysProject);
        return getDataTable(list);
    }

    /**
     * 邀请团队成员
     */
    @PreAuthorize("@ss.hasPermi('admin:project:lock')")
    @Log(title = "团队", businessType = BusinessType.UPDATE)
    @PutMapping("/{projectId}/lock")
    public AjaxResult lock(@PathVariable("projectId") Long projectId, @RequestBody SysProject project)
    {
        SysProject updateProject = new SysProject();
        updateProject.setProjectId(projectId);
        updateProject.setLock(project.getLock());
        updateProject.setLockRemark(project.getLockRemark());
        return toAjax(sysProjectService.updateSysProject(updateProject));
    }
}
