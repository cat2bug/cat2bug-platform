package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.framework.web.service.SysLoginService;
import com.cat2bug.framework.web.service.SysPermissionService;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.domain.SysUserProject;
import com.cat2bug.system.service.ISysProjectService;
import com.cat2bug.system.service.ISysRoleService;
import com.cat2bug.system.service.ISysUserConfigService;
import com.cat2bug.system.service.ISysUserProjectService;
import com.cat2bug.web.vo.PullProject;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 项目Controller
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
@RestController
@RequestMapping("/system/project")
public class SysProjectController extends BaseController
{
    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysProjectService sysProjectService;

    @Autowired
    private ISysUserProjectService sysUserProjectService;

    @Autowired
    private ISysRoleService sysRoleService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysUserConfigService sysUserConfigService;
    /**
     * 查询项目列表
     */
    @PreAuthorize("@ss.hasPermi('system:project:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysProject sysProject)
    {
        Preconditions.checkNotNull(sysProject.getTeamId(), MessageUtils.message("project.team_cannot_empty"));
        startPage();
        List<SysProject> list = sysProjectService.selectSysProjectList(sysProject);
        return getDataTable(list);
    }

    /**
     * 导出项目列表
     */
    @PreAuthorize("@ss.hasPermi('system:project:export')")
    @Log(title = "项目", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysProject sysProject)
    {
        List<SysProject> list = sysProjectService.selectSysProjectList(sysProject);
        ExcelUtil<SysProject> util = new ExcelUtil<SysProject>(SysProject.class);
        util.exportExcel(response, list, "项目数据");
    }

    /**
     * 导出项目列表
     */
    @PreAuthorize("@ss.hasPermi('system:project:list')")
    @Log(title = "项目", businessType = BusinessType.EXPORT)
    @GetMapping("/{projectId}/role")
    public TableDataInfo getRoles(@PathVariable Long projectId)
    {
        startPage();
        SysRole role = new SysRole();
        role.setIsProjectRole(true);
        List<SysRole> list = sysRoleService.selectRoleList(role);
        return getDataTable(list);
    }

    /**
     * 获取项目详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:project:query')")
    @GetMapping(value = "/{projectId}")
    public AjaxResult getInfo(@PathVariable("projectId") Long projectId)
    {
        return success(sysProjectService.selectSysProjectByProjectId(projectId));
    }

    /**
     * 新增项目
     */
    @PreAuthorize("@ss.hasPermi('system:project:add')")
    @Log(title = "项目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysProject sysProject)
    {
        return toAjax(sysProjectService.insertSysProject(sysProject));
    }

    /**
     * 新增项目
     */
    @PreAuthorize("@ss.hasPermi('system:project:push')")
    @Log(title = "项目", businessType = BusinessType.INSERT)
    @PostMapping("/{projectId}/push")
    public AjaxResult pull(@PathVariable Long projectId, @RequestBody PullProject pullProject) throws IOException {
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(getUserId());
        return toAjax(sysProjectService.pushToCloud(userConfig.getCurrentProjectId(), pullProject.getPullKey()));
    }

    @PreAuthorize("@ss.hasPermi('system:project:list')")
    @Log(title = "项目", businessType = BusinessType.INSERT)
    @PostMapping("/{projectId}/collect")
    public AjaxResult collect(@PathVariable Long projectId, @RequestBody SysProject sysProject)
    {
        SysUserProject sysUserProject = new SysUserProject();
        sysUserProject.setUserId(getUserId());
        sysUserProject.setProjectId(projectId);
        sysUserProject.setCollect(sysProject.isCollect());
        return toAjax(sysUserProjectService.updateSysUserProjectByUserIdAndProjectId(sysUserProject));
    }


    /**
     * 修改项目
     */
    @PreAuthorize("@ss.hasPermi('system:project:edit')")
    @Log(title = "项目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysProject sysProject)
    {
        return toAjax(sysProjectService.updateSysProject(sysProject));
    }

    /**
     * 删除项目
     */
    @PreAuthorize("@ss.hasPermi('system:project:remove')")
    @Log(title = "项目", businessType = BusinessType.DELETE)
	@DeleteMapping("/{projectId}")
    public AjaxResult remove(@PathVariable Long projectId, @RequestBody String password)
    {
        Preconditions.checkNotNull(password, MessageUtils.message("user.password.not_empty"));
        loginService.loginPreCheck(getUsername(),password);
        // 删除项目
        int ret = sysProjectService.deleteSysProjectByProjectId(projectId);
        // 更新用户权限
        permissionService.updateRoleAndPermissionOfCurrentUser();
        return toAjax(ret);
    }
}
