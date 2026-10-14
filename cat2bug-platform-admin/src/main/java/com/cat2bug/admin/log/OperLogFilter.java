package com.cat2bug.admin.log;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysOperLog;
import com.cat2bug.framework.security.SecurityRequestContext;
import com.cat2bug.framework.web.ClientInfo;
import com.cat2bug.system.service.SysOperLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.multipart.FormValue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 操作日志（sys_oper_log），与 Spring {@code LogAspect} 对等：按规则表匹配接口，
 * 记录请求参数（脱敏密码字段）、响应 JSON、耗时、操作人与 IP，异步写库。
 */
@Provider
@Priority(Priorities.USER + 100)
public class OperLogFilter implements ContainerRequestFilter, ContainerResponseFilter, ReaderInterceptor {

    private static final Logger LOG = Logger.getLogger(OperLogFilter.class);

    static final String RULE_PROPERTY = "cat2bug.operlog.rule";
    static final String START_PROPERTY = "cat2bug.operlog.start";
    static final String BODY_PROPERTY = "cat2bug.operlog.body";
    static final Set<String> EXCLUDE_PROPERTIES = Set.of("password", "oldPassword", "newPassword", "confirmPassword");
    static final int OPERATOR_MANAGE = 1;
    static final int STATUS_SUCCESS = 0;
    static final int STATUS_FAIL = 1;
    static final int MAX_TEXT = 2000;
    static final int BUSINESS_OTHER = 0;

    @Context
    ResourceInfo resourceInfo;

    @Inject
    OperLogRules operLogRules;

    @Inject
    SysOperLogService sysOperLogService;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void filter(ContainerRequestContext ctx) {
        operLogRules.find(ctx.getMethod(), resourceInfo.getResourceClass(), resourceInfo.getResourceMethod())
                .or(() -> mcpFallbackRule(ctx))
                .ifPresent(rule -> {
                    ctx.setProperty(RULE_PROPERTY, rule);
                    ctx.setProperty(START_PROPERTY, System.currentTimeMillis());
                });
    }

    /** MCP 会话下的写请求即使规则表未收录也要留痕。 */
    private java.util.Optional<OperLogRules.Rule> mcpFallbackRule(ContainerRequestContext ctx) {
        if ("GET".equals(ctx.getMethod()) || resourceInfo.getResourceClass() == null) {
            return java.util.Optional.empty();
        }
        LoginUser loginUser = SecurityRequestContext.getLoginUser(ctx);
        if (loginUser == null || !loginUser.isMcpSession()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new OperLogRules.Rule(resourceInfo.getResourceClass().getSimpleName(),
                BUSINESS_OTHER, true, false, Set.of()));
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        Object body = context.proceed();
        if (context.getProperty(RULE_PROPERTY) != null && body != null) {
            context.setProperty(BODY_PROPERTY, body);
        }
        return body;
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext resp) {
        if (!(req.getProperty(RULE_PROPERTY) instanceof OperLogRules.Rule rule)) {
            return;
        }
        try {
            SysOperLog operLog = new SysOperLog();
            operLog.setTitle(rule.title());
            operLog.setBusinessType(rule.businessType());
            operLog.setOperatorType(OPERATOR_MANAGE);
            operLog.setRequestMethod(req.getMethod());
            operLog.setOperUrl(truncate("/" + req.getUriInfo().getPath().replaceFirst("^/+", ""), 255));
            operLog.setMethod(resourceInfo.getResourceClass().getName() + "."
                    + resourceInfo.getResourceMethod().getName() + "()");
            LoginUser loginUser = SecurityRequestContext.getLoginUser(req);
            if (loginUser != null && loginUser.getUser() != null) {
                operLog.setOperName(loginUser.getUser().getUserName());
            }
            if (loginUser != null) {
                operLog.setOperChannel(loginUser.getOperChannel());
                operLog.setClientName(truncate(loginUser.getClientName(), 128));
            }
            Object start = req.getProperty(START_PROPERTY);
            operLog.setCostTime(start instanceof Long s ? System.currentTimeMillis() - s : 0L);

            Object entity = resp.getEntity();
            boolean failed = resp.getStatus() >= 400;
            operLog.setStatus(failed ? STATUS_FAIL : STATUS_SUCCESS);
            if (failed) {
                operLog.setErrorMsg(truncate(errorMessage(entity, resp.getStatus()), MAX_TEXT));
            }
            if (rule.saveRequest()) {
                operLog.setOperParam(truncate(requestParams(req, rule.excludeParams()), MAX_TEXT));
            }
            if (rule.saveResponse() && !failed && isJsonEntity(entity)) {
                operLog.setJsonResult(truncate(objectMapper.writeValueAsString(entity), MAX_TEXT));
            }
            ClientInfo client = ClientInfo.current();
            operLog.setOperIp(client.ip());
            Infrastructure.getDefaultWorkerPool().execute(() -> {
                try {
                    operLog.setOperLocation(ClientInfo.resolveLocation(operLog.getOperIp()));
                    sysOperLogService.insertOperlog(operLog);
                } catch (RuntimeException e) {
                    LOG.warnf("记录操作日志失败 %s: %s", operLog.getOperUrl(), e.getMessage());
                }
            });
        } catch (Exception e) {
            LOG.warnf("构建操作日志失败: %s", e.getMessage());
        }
    }

    /** 与 Spring 一致：有 query/form 参数记参数；否则 POST/PUT 记请求体 JSON。 */
    String requestParams(ContainerRequestContext req, Set<String> extraExcludes) throws Exception {
        Set<String> excludes = new java.util.HashSet<>(EXCLUDE_PROPERTIES);
        excludes.addAll(extraExcludes);
        Map<String, String> params = new LinkedHashMap<>();
        MultivaluedMap<String, String> query = req.getUriInfo().getQueryParameters();
        query.forEach((k, v) -> params.put(k, String.join(",", v)));
        params.putAll(formParams());
        excludes.forEach(params::remove);
        String method = req.getMethod();
        Object body = req.getProperty(BODY_PROPERTY);
        if (params.isEmpty() && ("POST".equals(method) || "PUT".equals(method))) {
            if (body == null || !isJsonEntity(body)) {
                return "";
            }
            JsonNode tree = objectMapper.valueToTree(body);
            stripExcluded(tree, excludes);
            return objectMapper.writeValueAsString(tree);
        }
        return objectMapper.writeValueAsString(params);
    }

    private static Map<String, String> formParams() {
        Map<String, String> params = new LinkedHashMap<>();
        ResteasyReactiveRequestContext current = CurrentRequestManager.get();
        FormData form = current != null ? current.getFormData() : null;
        if (form == null) {
            return params;
        }
        for (Iterator<String> it = form.iterator(); it.hasNext(); ) {
            String name = it.next();
            Deque<FormValue> values = form.get(name);
            if (values == null || values.stream().anyMatch(FormValue::isFileItem)) {
                continue;
            }
            params.put(name, values.stream().map(FormValue::getValue).collect(Collectors.joining(",")));
        }
        return params;
    }

    static void stripExcluded(JsonNode node, Set<String> excludes) {
        if (node instanceof ObjectNode obj) {
            obj.remove(excludes);
            obj.forEach(child -> stripExcluded(child, excludes));
        } else if (node instanceof ArrayNode arr) {
            arr.forEach(child -> stripExcluded(child, excludes));
        }
    }

    private static boolean isJsonEntity(Object entity) {
        return entity != null
                && !(entity instanceof byte[])
                && !(entity instanceof InputStream)
                && !(entity instanceof StreamingOutput)
                && !(entity instanceof File)
                && !(entity instanceof java.nio.file.Path)
                && !(entity instanceof List<?> list && list.stream().anyMatch(o -> o instanceof FormValue));
    }

    private static String errorMessage(Object entity, int status) {
        if (entity instanceof AjaxResult result && result.get(AjaxResult.MSG_TAG) != null) {
            return String.valueOf(result.get(AjaxResult.MSG_TAG));
        }
        return "HTTP " + status;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
