package com.cat2bug.admin.config;

import com.cat2bug.admin.setup.UpgradeService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.config.UpgradeSupport;
import com.cat2bug.system.service.InstallStateService;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * 升级进行中时全锁业务 API，仅放行升级向导白名单。
 * <p>对齐已删除的 Spring {@code UpgradeFilter}；热路径只读升级状态，不触发 Flyway。</p>
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION - 50)
@ApplicationScoped
public class UpgradeFilter implements ContainerRequestFilter {

    static final String UPGRADE_LOCK_MESSAGE = "系统正在升级，请先完成升级向导";

    @Inject
    UpgradeService upgradeService;

    @Inject
    InstallStateService installStateService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestUriPath(requestContext);
        String method = requestContext.getMethod();

        // 与历史 SetupFilter /upgrade/status 一致：状态查询在 Filter 层直接返回
        if ("/upgrade/status".equals(path) && "GET".equalsIgnoreCase(method)) {
            requestContext.abortWith(Response.ok(AjaxResult.success(upgradeService.getStatus()))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
            return;
        }

        if (!installStateService.isInstalled()) {
            return;
        }
        if (upgradeService.isUpgradeSkipped() || installStateService.needsRestart()) {
            return;
        }

        String state = upgradeService.resolveState();
        if (!UpgradeSupport.isActiveState(state) && !UpgradeSupport.STATE_RESTART_REQUIRED.equals(state)) {
            return;
        }

        if (UpgradeSupport.STATE_RESTART_REQUIRED.equals(state)) {
            if (isAllowedDuringUpgradeRestart(path, method)) {
                return;
            }
        } else if (isAllowedDuringUpgrade(path, method)) {
            return;
        }

        requestContext.abortWith(Response.ok(AjaxResult.error(UPGRADE_LOCK_MESSAGE))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
    }

    static String requestUriPath(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    static boolean isAllowedDuringUpgrade(String path, String method) {
        if (isUpgradePath(path)) {
            return true;
        }
        if (isSetupTestPath(path)) {
            return true;
        }
        if ("/setup/status".equals(path)) {
            return true;
        }
        if ("/version".equals(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method)) {
            if ("/".equals(path) || "/index".equals(path)) {
                return true;
            }
            if (path.startsWith("/static/") || path.startsWith("/profile/") || path.startsWith("/docs/")) {
                return true;
            }
            if (path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".js")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUpgradePath(String path) {
        return "/upgrade/status".equals(path)
                || "/upgrade".equals(path)
                || path.startsWith("/upgrade/");
    }

    /** 升级预检复用 setup 连接/路径测试 API */
    static boolean isSetupTestPath(String path) {
        return path.startsWith("/setup/test/");
    }

    static boolean isAllowedDuringUpgradeRestart(String path, String method) {
        if (isAllowedDuringUpgrade(path, method)) {
            return true;
        }
        return "/login".equals(path)
                || "/register".equals(path)
                || "/captchaImage".equals(path);
    }
}
