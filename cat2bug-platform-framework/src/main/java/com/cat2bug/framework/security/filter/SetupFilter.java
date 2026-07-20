package com.cat2bug.framework.security.filter;

import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.utils.ServletUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.service.InstallService;
import com.cat2bug.common.config.UpgradeSupport;
import com.cat2bug.framework.service.UpgradeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 未安装时仅允许 setup 与白名单接口；安装后禁止再次访问 setup API
 */
@Component
@Order(-200)
public class SetupFilter extends OncePerRequestFilter
{
    @Autowired
    private InstallService installService;

    @Autowired
    private UpgradeService upgradeService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        String path = requestUriPath(request);

        // 安装状态查询：在 Filter 层直接返回，避免 MVC/异常转发到 /error 后被 Security 挡成 401，
        // 导致前端误判「未安装」而锁死在安装向导。
        if (isSetupStatusPath(path) && HttpMethod.GET.matches(request.getMethod()))
        {
            java.util.Map<String, Object> data = new java.util.HashMap<>(4);
            data.put("installed", installService.isInstalled());
            data.put("skipped", installService.isInstallSkipped());
            data.put("restartRequired", installService.needsRestart());
            ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.success(data)));
            return;
        }

        if (installService.needsRestart())
        {
            if (isAllowedDuringRestartPending(path, request.getMethod()))
            {
                chain.doFilter(request, response);
                return;
            }
            if (UpgradeSupport.STATE_RESTART_REQUIRED.equals(upgradeService.resolveState())
                    && isAllowedDuringUpgradeRestart(path, request.getMethod()))
            {
                chain.doFilter(request, response);
                return;
            }
            ServletUtils.renderString(response, JSON.toJSONString(
                    AjaxResult.error("安装配置已保存，请重启应用后再登录；切换 MySQL 或 Redis 后必须重启才能生效")));
            return;
        }

        boolean installed = installService.isInstalled();

        if (!installed)
        {
            if (isAllowedDuringRestartPending(path, request.getMethod()) || "/captchaImage".equals(path))
            {
                chain.doFilter(request, response);
                return;
            }
            ServletUtils.renderString(response, JSON.toJSONString(
                    AjaxResult.error("系统尚未完成安装，请先完成安装向导")));
            return;
        }

        if (isSetupPath(path) && !isSetupStatusPath(path))
        {
            if (UpgradeFilter.isSetupTestPath(path)
                    && (upgradeService.isUpgradeRequired() || upgradeService.isUpgradeActive()))
            {
                chain.doFilter(request, response);
                return;
            }
            ServletUtils.renderString(response, JSON.toJSONString(
                    AjaxResult.error("系统已完成安装，无法再次访问安装接口")));
            return;
        }

        chain.doFilter(request, response);
    }

    private static String requestUriPath(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.isNotEmpty(contextPath) && uri.startsWith(contextPath))
        {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private static boolean isSetupPath(String path)
    {
        return path.equals("/setup") || path.startsWith("/setup/");
    }

    /** 已安装时仍允许查询安装状态，供前端路由判断 */
    private static boolean isSetupStatusPath(String path)
    {
        return "/setup/status".equals(path);
    }

    /** 安装已提交但未重启：仅允许 setup 页与静态资源 */
    private static boolean isAllowedDuringRestartPending(String path, String method)
    {
        if (isSetupPath(path))
        {
            return true;
        }
        if ("/version".equals(path))
        {
            return true;
        }
        if (HttpMethod.GET.matches(method))
        {
            if (path.equals("/") || path.equals("/index"))
            {
                return true;
            }
            if (path.startsWith("/static/") || path.startsWith("/profile/") || path.startsWith("/docs/"))
            {
                return true;
            }
            if (path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".js"))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowedDuringUpgrade(String path, String method)
    {
        if (path.equals("/upgrade") || path.startsWith("/upgrade/"))
        {
            return true;
        }
        if (UpgradeFilter.isSetupTestPath(path))
        {
            return true;
        }
        if ("/setup/status".equals(path))
        {
            return true;
        }
        if ("/version".equals(path))
        {
            return true;
        }
        if (HttpMethod.GET.matches(method))
        {
            if (path.equals("/") || path.equals("/index"))
            {
                return true;
            }
            if (path.startsWith("/static/") || path.startsWith("/profile/") || path.startsWith("/docs/"))
            {
                return true;
            }
            if (path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".js"))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowedDuringUpgradeRestart(String path, String method)
    {
        if (isAllowedDuringUpgrade(path, method))
        {
            return true;
        }
        return "/login".equals(path)
                || "/register".equals(path)
                || "/captchaImage".equals(path);
    }
}
