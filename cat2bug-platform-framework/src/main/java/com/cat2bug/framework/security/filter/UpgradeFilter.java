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
 * 升级进行中时全锁业务 API，仅放行升级向导白名单。
 */
@Component
@Order(-199)
public class UpgradeFilter extends OncePerRequestFilter
{
    @Autowired
    private UpgradeService upgradeService;

    @Autowired
    private InstallService installService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        // 未安装：仅 setup 路径，由 SetupFilter 管控，此处不拦截
        if (!installService.isInstalled())
        {
            chain.doFilter(request, response);
            return;
        }
        if (upgradeService.isUpgradeSkipped() || installService.needsRestart())
        {
            chain.doFilter(request, response);
            return;
        }

        // 热路径仅读磁盘升级状态，禁止在此触发 Flyway（避免与业务争抢连接池）
        String state = upgradeService.resolveState();
        if (!UpgradeSupport.isActiveState(state) && !UpgradeSupport.STATE_RESTART_REQUIRED.equals(state))
        {
            chain.doFilter(request, response);
            return;
        }

        String path = requestUriPath(request);
        if (UpgradeSupport.STATE_RESTART_REQUIRED.equals(state))
        {
            if (isAllowedDuringUpgradeRestart(path, request.getMethod()))
            {
                chain.doFilter(request, response);
                return;
            }
        }
        if (isAllowedDuringUpgrade(path, request.getMethod()))
        {
            chain.doFilter(request, response);
            return;
        }

        ServletUtils.renderString(response, JSON.toJSONString(
                AjaxResult.error("系统正在升级，请先完成升级向导")));
    }

    static String requestUriPath(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.isNotEmpty(contextPath) && uri.startsWith(contextPath))
        {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    static boolean isAllowedDuringUpgrade(String path, String method)
    {
        if (isUpgradePath(path))
        {
            return true;
        }
        if (isSetupTestPath(path))
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
            if (path.startsWith("/static/") || path.startsWith("/profile/") || path.startsWith("/docs/images/"))
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

    private static boolean isUpgradePath(String path)
    {
        return "/upgrade/status".equals(path)
                || path.equals("/upgrade")
                || path.startsWith("/upgrade/");
    }

    /** 升级预检复用 setup 连接/路径测试 API */
    static boolean isSetupTestPath(String path)
    {
        return path.startsWith("/setup/test/");
    }

    static boolean isAllowedDuringUpgradeRestart(String path, String method)
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
