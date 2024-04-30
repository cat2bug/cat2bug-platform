package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.common.core.domain.entity.SysReport;
import com.cat2bug.system.service.IMemberFocusService;
import com.cat2bug.system.service.ISysReportService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * 报告Controller
 * 
 * @author yuzhantao
 * @date 2024-03-13
 */
@RestController
@RequestMapping("/system/report")
public class SysReportController extends BaseController
{
    private Logger log = LogManager.getLogger(SysReportController.class);
    private final static String MODULE_NAME = "report";
    @Autowired
    private ISysReportService sysReportService;

    @Autowired
    private IMemberFocusService memberFocusService;
    /**
     * 查询报告列表
     */
    @PreAuthorize("@ss.hasPermi('system:report:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysReport sysReport)
    {
        startPage();
        List<SysReport> list = sysReportService.selectSysReportList(sysReport);
        list.forEach(l->{
            List<SysUser> focusList = memberFocusService.getFocusMemberList(MODULE_NAME,l.getReportId());
            l.setFocusList(focusList);
        });
        return getDataTable(list);
    }

    /**
     * 导出报告列表
     */
    @PreAuthorize("@ss.hasPermi('system:report:export')")
    @Log(title = "报告", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysReport sysReport)
    {
        List<SysReport> list = sysReportService.selectSysReportList(sysReport);
        ExcelUtil<SysReport> util = new ExcelUtil<SysReport>(SysReport.class);
        util.exportExcel(response, list, "报告数据");
    }

    /**
     * 获取报告详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:report:query')")
    @GetMapping(value = "/{reportId}")
    public AjaxResult getInfo(@PathVariable("reportId") Long reportId)
    {
        SysReport sysReport = sysReportService.selectSysReportByReportId(reportId);
        if(sysReport!=null) {
            memberFocusService.setFocus(MODULE_NAME, reportId, this.getLoginUser().getUser());
            List<SysUser> focusList = memberFocusService.getFocusMemberList(MODULE_NAME,reportId);
            sysReport.setFocusList(focusList);
        }
        return success(sysReport);
    }

    /**
     * 关闭窗口
     */
    @PostMapping("/close-edit-window")
    public AjaxResult closeEditWindows()
    {
        memberFocusService.removeFocus(getUserId());
        return success();
    }

    /**
     * 新增报告
     */
    @PreAuthorize("@ss.hasPermi('system:report:add')")
    @Log(title = "报告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(HttpServletRequest request, @RequestBody SysReport sysReport)
    {
        // 获取客户端ip
        String ipAddress = null;
        try {
            ipAddress = request.getHeader("x-forwarded-for");
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
                if (ipAddress.equals("127.0.0.1")) {
                    // 根据网卡取本机配置的IP
                    InetAddress inet = null;
                    try {
                        inet = InetAddress.getLocalHost();
                    } catch (UnknownHostException e) {
                        log.warn(e);
                    }
                    ipAddress = inet.getHostAddress();
                }
            }
            // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
            if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length()
                // = 15
                if (ipAddress.indexOf(",") > 0) {
                    ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
                }
            }
        } catch (Exception e) {
            ipAddress="";
        }
        sysReport.setReportSource(ipAddress);
        return toAjax(sysReportService.insertSysReport(sysReport));
    }

    /**
     * 修改报告
     */
    @PreAuthorize("@ss.hasPermi('system:report:edit')")
    @Log(title = "报告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysReport sysReport)
    {
        return toAjax(sysReportService.updateSysReport(sysReport));
    }

    /**
     * 删除报告
     */
    @PreAuthorize("@ss.hasPermi('system:report:remove')")
    @Log(title = "报告", businessType = BusinessType.DELETE)
	@DeleteMapping("/{reportIds}")
    public AjaxResult remove(@PathVariable Long[] reportIds)
    {
        return toAjax(sysReportService.deleteSysReportByReportIds(reportIds));
    }
}
