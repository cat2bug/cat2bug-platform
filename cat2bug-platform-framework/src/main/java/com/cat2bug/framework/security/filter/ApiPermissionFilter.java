package com.cat2bug.framework.security.filter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysProjectApi;
import com.cat2bug.system.service.ISysProjectApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * API KEY 权限验证过滤器
 *
 * @author yuzhantao
 * @date 2024-04-08
 */
@Component
@ConditionalOnProperty(prefix = "cat2bug.api", name = "enabled", havingValue = "true")
public class ApiPermissionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiPermissionFilter.class);

    private static final String AUTH_TOKEN_HEADER_NAME = "CAT2BUG-API-KEY";

    @Autowired
    private ISysProjectApiService sysProjectApiService;

    /**
     * 权限映射：请求路径和方法 -> 所需权限
     */
    private static final Map<String, Map<String, String>> PERMISSION_MAPPING = new HashMap<>();

    static {
        // 缺陷管理权限
        Map<String, String> defectPermissions = new HashMap<>();
        defectPermissions.put("GET:/api/defect", "defect.list");
        defectPermissions.put("GET:/api/defect/{id}", "defect.query");
        defectPermissions.put("POST:/api/defect", "defect.create");
        defectPermissions.put("PUT:/api/defect", "defect.update");
        defectPermissions.put("DELETE:/api/defect", "defect.delete");
        defectPermissions.put("POST:/api/defect/{id}/assign", "defect.assign");
        defectPermissions.put("POST:/api/defect/{id}/reject", "defect.reject");
        defectPermissions.put("POST:/api/defect/{id}/repair", "defect.repair");
        defectPermissions.put("POST:/api/defect/{id}/pass", "defect.pass");
        defectPermissions.put("POST:/api/defect/{id}/close", "defect.close");
        defectPermissions.put("POST:/api/defect/{id}/open", "defect.open");
        PERMISSION_MAPPING.put("defect", defectPermissions);

        // 测试用例管理权限
        Map<String, String> casePermissions = new HashMap<>();
        casePermissions.put("GET:/api/case", "case.list");
        casePermissions.put("GET:/api/case/{id}", "case.query");
        casePermissions.put("POST:/api/case", "case.create");
        casePermissions.put("PUT:/api/case", "case.update");
        casePermissions.put("DELETE:/api/case/{id}", "case.delete");
        PERMISSION_MAPPING.put("case", casePermissions);

        // 交付物管理权限
        Map<String, String> deliverablePermissions = new HashMap<>();
        deliverablePermissions.put("GET:/api/deliverable", "deliverable.list");
        deliverablePermissions.put("GET:/api/deliverable/{id}", "deliverable.query");
        deliverablePermissions.put("POST:/api/deliverable", "deliverable.create");
        PERMISSION_MAPPING.put("deliverable", deliverablePermissions);

        // 文件管理权限
        Map<String, String> filePermissions = new HashMap<>();
        filePermissions.put("POST:/api/file/upload", "file.upload");
        PERMISSION_MAPPING.put("file", filePermissions);

        // 项目信息权限
        Map<String, String> projectPermissions = new HashMap<>();
        projectPermissions.put("GET:/api/project", "project.info");
        projectPermissions.put("GET:/api/member", "project.members");
        PERMISSION_MAPPING.put("project", projectPermissions);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // 只处理 /api/** 路径
        if (!requestPath.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // 获取 API KEY
        String apiKey = getApiKey(request);
        if (StringUtils.isEmpty(apiKey)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 查询 API KEY 配置
            SysProjectApi projectApi = sysProjectApiService.selectSysProjectApiByApiId(apiKey);

            if (projectApi == null) {
                chain.doFilter(request, response);
                return;
            }

            // 检查 API KEY 是否启用
            if (projectApi.getEnabled() != null && !projectApi.getEnabled()) {
                log.warn("API KEY 已禁用: {}", apiKey);
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "API KEY 已禁用");
                return;
            }

            // 检查权限
            if (!hasPermission(projectApi, method, requestPath)) {
                log.warn("API KEY 无权限访问: {} {}", method, requestPath);
                sendErrorResponse(response, HttpStatus.FORBIDDEN.value(), "无权限访问此资源");
                return;
            }

            // 权限验证通过，继续执行
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.error("API 权限验证异常", e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), "权限验证异常");
        }
    }

    /**
     * 从请求头获取 API KEY
     */
    private String getApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME);
        if (StringUtils.isNotEmpty(apiKey)) {
            apiKey = apiKey.replace("Bearer ", "").trim();
        }
        return apiKey;
    }

    /**
     * 检查是否有权限
     */
    private boolean hasPermission(SysProjectApi projectApi, String method, String requestPath) {
        // 如果没有配置 features，默认允许所有操作
        if (StringUtils.isEmpty(projectApi.getFeatures())) {
            return true;
        }

        try {
            // 解析 features JSON
            JSONObject featuresJson = JSON.parseObject(projectApi.getFeatures());

            // 获取请求所需的权限
            String requiredPermission = getRequiredPermission(method, requestPath);
            if (StringUtils.isEmpty(requiredPermission)) {
                // 未配置权限要求的接口，默认允许
                return true;
            }

            // 检查是否有对应的权限
            return checkPermissionInFeatures(featuresJson, requiredPermission);

        } catch (Exception e) {
            log.error("解析权限配置失败", e);
            // 解析失败时，默认拒绝访问
            return false;
        }
    }

    /**
     * 获取请求所需的权限
     */
    private String getRequiredPermission(String method, String requestPath) {
        // 标准化路径（移除路径参数）
        String normalizedPath = normalizePath(requestPath);
        String key = method + ":" + normalizedPath;

        // 遍历权限映射查找匹配的权限
        for (Map<String, String> permissions : PERMISSION_MAPPING.values()) {
            if (permissions.containsKey(key)) {
                return permissions.get(key);
            }
        }

        return null;
    }

    /**
     * 标准化路径，将路径参数替换为 {id}
     */
    private String normalizePath(String path) {
        // 简单处理：将数字 ID 替换为 {id}
        return path.replaceAll("/\\d+", "/{id}");
    }

    /**
     * 在 features 配置中检查权限
     */
    private boolean checkPermissionInFeatures(JSONObject featuresJson, String requiredPermission) {
        // requiredPermission 格式: "defect.query", "case.create" 等
        String[] parts = requiredPermission.split("\\.");
        if (parts.length != 2) {
            return false;
        }

        String module = parts[0];  // defect, case, deliverable, file, project
        String action = parts[1];  // query, create, update, delete 等

        // 检查模块是否存在
        if (!featuresJson.containsKey(module)) {
            return false;
        }

        // 获取模块的权限数组
        List<String> modulePermissions = featuresJson.getList(module, String.class);
        if (modulePermissions == null || modulePermissions.isEmpty()) {
            return false;
        }

        // 检查是否包含所需的操作权限
        return modulePermissions.contains(action);
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        AjaxResult result = AjaxResult.error(status, message);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
