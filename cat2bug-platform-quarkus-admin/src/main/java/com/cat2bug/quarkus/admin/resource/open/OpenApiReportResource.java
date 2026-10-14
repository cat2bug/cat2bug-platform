package com.cat2bug.quarkus.admin.resource.open;

import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.domain.open.OpenApiDefectReportRequest;
import com.cat2bug.quarkus.common.report.DefectReportCoder;
import com.cat2bug.quarkus.system.open.OpenApiReportService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Path("/api/report")
@Produces(MediaType.APPLICATION_JSON)
public class OpenApiReportResource {

    private static final Logger LOG = Logger.getLogger(OpenApiReportResource.class);

    @Inject
    OpenApiReportService openApiReportService;

    @POST
    @Path("/defect")
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult pushDefect(@Context ContainerRequestContext requestContext,
                                 OpenApiDefectReportRequest apiReport) {
        apiReport.setReportSource(resolveClientIp(requestContext));
        apiReport.setReportDataCoder(DefectReportCoder.class.getName());
        return toAjax(openApiReportService.pushDefectReport(apiReport));
    }

    private static AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }

    private static String resolveClientIp(ContainerRequestContext requestContext) {
        try {
            String ipAddress = requestContext.getHeaderString("x-forwarded-for");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = requestContext.getHeaderString("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = requestContext.getHeaderString("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = requestContext.getUriInfo().getRequestUri().getHost();
                if ("127.0.0.1".equals(ipAddress) || "localhost".equalsIgnoreCase(ipAddress)) {
                    try {
                        ipAddress = InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException e) {
                        LOG.warn("Failed to resolve local host address", e);
                    }
                }
            }
            if (ipAddress != null && ipAddress.length() > 15 && ipAddress.indexOf(',') > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(','));
            }
            return ipAddress == null ? "" : ipAddress;
        } catch (Exception e) {
            return "";
        }
    }
}
