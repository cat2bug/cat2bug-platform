package com.cat2bug.api.controller;

import com.cat2bug.api.service.IApiReportService;
import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysReport;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.report.DefectReportCoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/report")
public class ApiReportController extends BaseController
{
    Logger log = LogManager.getLogger(ApiReportController.class);

    @Autowired
    private IApiReportService apiReportService;

    /**
     * 新增报告
     */
    @PreAuthorize("@ss.hasPermi('api:report:push')")
    @Log(title = "报告", businessType = BusinessType.INSERT)
    @PostMapping("/defect")
    public AjaxResult pushDefect(HttpServletRequest request, @RequestBody SysReport<List<SysDefect>> apiReport)
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

        apiReport.setReportSource(ipAddress);
        apiReport.setReportDataCoder(DefectReportCoder.class.getName());
        return toAjax(apiReportService.pushDefectReport(apiReport));
    }

}
