package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.DictUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.domain.SysDefectShard;
import com.cat2bug.system.service.ISysDefectLogService;
import com.cat2bug.system.service.ISysDefectService;
import com.cat2bug.system.service.ISysDefectShardService;
import com.cat2bug.system.service.ISysUserProjectService;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分享缺陷关联Controller
 * 
 * @author yuzhantao
 * @date 2024-04-10
 */
@RestController
@RequestMapping("/system/defect")
public class SysDefectShardController extends BaseController
{
    @Autowired
    private ISysDefectShardService sysDefectShardService;
    @Autowired
    private ISysDefectService sysDefectService;
    @Autowired
    private ISysUserProjectService sysUserProjectService;
    @Autowired
    private ISysDefectLogService sysDefectLogService;

    /**
     * 获取分享缺陷关联详细信息
     */
    @GetMapping(value = "/{defectId}/shard/{defectShardId}")
    public AjaxResult getDefectInfo(@PathVariable("defectId") Long defectId,@PathVariable("defectShardId") String defectShardId,SysDefectShard sysDefectShard)
    {
        Map<String, Object> ret = new HashMap<>();
        SysDefectShard shard = sysDefectShardService.selectSysDefectShardByDefectShardId(defectShardId);
        if(shard==null) {
            return AjaxResult.error("没有找到分享的缺陷!");
        }

        if(shard.getAgingTime()!=null && shard.getAgingTime().getTime()>0 && shard.getAgingTime().getTime()<System.currentTimeMillis()) {
            return AjaxResult.error("分享已过期!");
        }
        Long memberId = null;
        try {
            memberId = getUserId();
        } catch (Exception e){}

        SysDefect sysDefect = sysDefectService.selectSysDefectByDefectId(shard.getDefectId(),memberId);

        if(sysDefect==null) {
            return AjaxResult.error("没有找到分享的缺陷!");
        }

        SysDefectLog sysDefectLog = new SysDefectLog();
        sysDefectLog.setDefectId(sysDefect.getDefectId());
        startPage();
        List<SysDefectLog> logList = sysDefectLogService.selectSysDefectLogList(sysDefectLog);
        ret.put("logs",logList);
        ret.put("defect",sysDefect);
        ret.put("defectLevel",DictUtils.getDictCache("defect_level"));


        // 如果不用输入密码，直接返回缺陷信息
        if(StringUtils.isBlank(shard.getPassword())) {
            return AjaxResult.success(ret);
        }

        // 如果当前用户在项目组里，直接返回
        if(memberId!=null) {
            List<SysUser>  users = sysUserProjectService.selectSysUserListByProjectId(shard.getProjectId(), new SysUser());
            Long finalMemberId = memberId;
            if(users!=null && users.stream().anyMatch(u->u.getUserId().equals(finalMemberId))) {
                return AjaxResult.success(ret);
            }
        }

        if(StringUtils.isNotBlank(shard.getPassword()) && StringUtils.isBlank(sysDefectShard.getPassword())) {
            return AjaxResult.error("分享密码不能为空!");
        }

        if(StringUtils.isNotBlank(shard.getPassword()) && shard.getPassword().equals(sysDefectShard.getPassword())==false) {
            return AjaxResult.error("分享密码错误!");
        }

        return AjaxResult.success(ret);
    }

    /**
     * 获取分享缺陷关联详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @GetMapping(value = "/{defectId}/shard")
    public AjaxResult getInfo(@PathVariable("defectShardId") Long defectId)
    {
        return success(sysDefectShardService.selectSysDefectShardByDefectIdAndMemberId(defectId,getUserId()));
    }

    /**
     * 分享缺陷关联
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @Log(title = "分享缺陷关联", businessType = BusinessType.INSERT)
    @PostMapping(value = "/{defectId}/shard")
    public AjaxResult shard(@PathVariable("defectId") String defectId,@RequestBody SysDefectShard sysDefectShard)
    {
        return success(sysDefectShardService.insertSysDefectShard(sysDefectShard));
    }
}
