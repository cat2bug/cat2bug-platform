package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.domain.SysProjectDefectTabs;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 缺陷状态Controller
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
@RestController
@RequestMapping("/system/defect/state")
public class SysDefectStateController extends BaseController
{
    @Autowired
    private ISysDefectService sysDefectService;
    /**
     * 查询缺陷列表
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping
    public AjaxResult states()
    {
        return success(this.sysDefectService.getDefectStateList());
    }
}
