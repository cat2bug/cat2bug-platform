package com.cat2bug.admin.mcp;

import com.cat2bug.common.security.McpModules;
import com.cat2bug.framework.security.RequiresPermissions;
import io.quarkus.arc.Arc;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用通道的操作目录：扫描网页 REST 资源，只收录带 {@code @RequiresPermissions}、权限属于已映射模块、
 * 参数都是 JSON / 查询参数、且路径里的对象 ID 能做项目归属校验的接口。
 */
public class McpOperationCatalog {

    private static final Logger LOG = Logger.getLogger(McpOperationCatalog.class);

    private static final Pattern TEMPLATE_VAR = Pattern.compile("\\{([A-Za-z0-9_]+)(?::[^}]*)?}");

    /** 路径中出现即可校验归属的参数 */
    static final Set<String> OWNED_PARAMS = Set.of("projectId", "defectId", "defectLogId", "defectLogIds", "caseId",
            "planId", "planIds", "planItemId", "planItemIds", "moduleId", "moduleIds",
            "docId", "docIds", "repositoryId", "repositoryIds");

    /** 只有同一路径里还有 projectId / repositoryId 时才允许出现的从属参数 */
    static final Set<String> SCOPED_PARAMS = Set.of("memberId", "userId", "name");

    static final List<String> EXCLUDED_PATH_PARTS = List.of("/export", "/import", "importtemplate", "importdata",
            "upload", "download", "/template", "/raw", "/archive", "close-edit-window", "/shard");

    static final Set<String> EXCLUDED_TEMPLATES = Set.of(
            "GET /system/defect/config",
            "GET /system/case/module/{moduleId}/ids",
            "GET /system/case/level/{level}/ids",
            "POST /system/project/{projectId}/push",
            "GET /system/project/{projectId}/mcp",
            "PUT /system/project/{projectId}/mcp");

    public record Param(String name, String type, String in) {
    }

    public record Operation(String id, String httpMethod, String path, List<String> permissions, boolean anyOf,
                            List<String> modules, List<Param> params, String bodyType, List<Param> bodyFields,
                            String summary) {

        public boolean readOnly() {
            return "GET".equals(httpMethod);
        }

        public List<String> pathVariables() {
            List<String> vars = new ArrayList<>();
            Matcher m = TEMPLATE_VAR.matcher(path);
            while (m.find()) {
                vars.add(m.group(1));
            }
            return vars;
        }
    }

    private static volatile Map<String, Operation> operations;

    public static Map<String, Operation> operations() {
        Map<String, Operation> ops = operations;
        if (ops == null) {
            synchronized (McpOperationCatalog.class) {
                if (operations == null) {
                    operations = Collections.unmodifiableMap(scan(resourceClasses()));
                }
                ops = operations;
            }
        }
        return ops;
    }

    static List<Class<?>> resourceClasses() {
        List<Class<?>> out = new ArrayList<>();
        try {
            for (Bean<?> bean : Arc.container().beanManager().getBeans(Object.class, Any.Literal.INSTANCE)) {
                Class<?> type = bean.getBeanClass();
                if (type != null && type.getName().startsWith("com.cat2bug.admin.resource.")
                        && type.isAnnotationPresent(Path.class) && !out.contains(type)) {
                    out.add(type);
                }
            }
        } catch (RuntimeException e) {
            LOG.warnf("扫描 REST 资源失败: %s", e.getMessage());
        }
        out.sort(Comparator.comparing(Class::getName));
        return out;
    }

    static Map<String, Operation> scan(List<Class<?>> classes) {
        Map<String, Operation> out = new LinkedHashMap<>();
        for (Class<?> type : classes) {
            String base = type.getAnnotation(Path.class).value();
            RequiresPermissions classPerm = type.getAnnotation(RequiresPermissions.class);
            Method[] methods = type.getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName).thenComparing(Method::getParameterCount));
            for (Method method : methods) {
                Operation op = toOperation(type, base, classPerm, method);
                if (op != null) {
                    String id = op.id();
                    int n = 2;
                    while (out.containsKey(id)) {
                        id = op.id() + n++;
                    }
                    out.put(id, id.equals(op.id()) ? op : withId(op, id));
                }
            }
        }
        return out;
    }

    private static Operation withId(Operation op, String id) {
        return new Operation(id, op.httpMethod(), op.path(), op.permissions(), op.anyOf(), op.modules(), op.params(),
                op.bodyType(), op.bodyFields(), op.summary());
    }

    static Operation toOperation(Class<?> type, String base, RequiresPermissions classPerm, Method method) {
        if (Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
            return null;
        }
        String httpMethod = httpMethod(method);
        if (httpMethod == null) {
            return null;
        }
        RequiresPermissions perm = method.getAnnotation(RequiresPermissions.class);
        if (perm == null) {
            perm = classPerm;
        }
        if (perm == null || perm.value().length == 0) {
            return null;
        }
        Path methodPath = method.getAnnotation(Path.class);
        String path = normalize("/" + base + "/" + (methodPath == null ? "" : methodPath.value()));
        String template = httpMethod + " " + path;
        if (EXCLUDED_TEMPLATES.contains(template) || path.startsWith("/ai") || path.startsWith("/git/")
                || path.startsWith("/api/") || path.startsWith("/monitor") || path.startsWith("/tool")) {
            return null;
        }
        String lower = path.toLowerCase();
        for (String part : EXCLUDED_PATH_PARTS) {
            if (lower.contains(part)) {
                return null;
            }
        }
        if (!producesJson(type, method)) {
            return null;
        }
        List<String> permissions = Arrays.asList(perm.value());
        boolean anyOf = perm.logical() == RequiresPermissions.Logical.OR || permissions.size() == 1;
        Set<String> modules = new HashSet<>();
        for (String p : permissions) {
            String m = McpModules.moduleOf(p);
            if (m != null) {
                modules.add(m);
            } else if (!anyOf) {
                return null;
            }
        }
        if (modules.isEmpty()) {
            return null;
        }

        List<Param> params = new ArrayList<>();
        String bodyType = null;
        List<Param> bodyFields = List.of();
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(Context.class)) {
                continue;
            }
            PathParam pp = parameter.getAnnotation(PathParam.class);
            QueryParam qp = parameter.getAnnotation(QueryParam.class);
            if (pp != null) {
                params.add(new Param(pp.value(), parameter.getType().getSimpleName(), "path"));
            } else if (qp != null) {
                params.add(new Param(qp.value(), parameter.getType().getSimpleName(), "query"));
            } else if (parameter.isAnnotationPresent(FormParam.class) || parameter.isAnnotationPresent(BeanParam.class)
                    || hasAnnotationNamed(parameter, "RestForm") || hasAnnotationNamed(parameter, "RestPath")
                    || hasAnnotationNamed(parameter, "RestQuery")) {
                return null;
            } else if (bodyType == null) {
                bodyType = parameter.getParameterizedType().getTypeName().replace("java.lang.", "")
                        .replace("java.util.", "").replaceAll("com\\.cat2bug\\.[a-z.]+\\.", "");
                bodyFields = fieldsOf(parameter.getType());
            } else {
                return null;
            }
        }
        Set<String> pathVars = new HashSet<>();
        Matcher m = TEMPLATE_VAR.matcher(path);
        while (m.find()) {
            pathVars.add(m.group(1));
        }
        boolean hasScopeAnchor = pathVars.contains("projectId") || pathVars.contains("repositoryId");
        for (String v : pathVars) {
            if (!OWNED_PARAMS.contains(v) && !(SCOPED_PARAMS.contains(v) && hasScopeAnchor)) {
                return null;
            }
        }
        String id = lowerFirst(type.getSimpleName().replace("Resource", "")) + "." + method.getName();
        return new Operation(id, httpMethod, path, permissions, anyOf, List.copyOf(new java.util.TreeSet<>(modules)),
                List.copyOf(params), bodyType, bodyFields, httpMethod + " " + path);
    }

    static String normalize(String raw) {
        String p = raw.replaceAll("/+", "/");
        Matcher m = TEMPLATE_VAR.matcher(p);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement("{" + m.group(1) + "}"));
        }
        m.appendTail(sb);
        String out = sb.toString();
        if (out.length() > 1 && out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private static String httpMethod(Method method) {
        if (method.isAnnotationPresent(GET.class)) {
            return "GET";
        }
        if (method.isAnnotationPresent(POST.class)) {
            return "POST";
        }
        if (method.isAnnotationPresent(PUT.class)) {
            return "PUT";
        }
        if (method.isAnnotationPresent(DELETE.class)) {
            return "DELETE";
        }
        return null;
    }

    private static boolean producesJson(Class<?> type, Method method) {
        Produces produces = method.getAnnotation(Produces.class);
        if (produces == null) {
            produces = type.getAnnotation(Produces.class);
        }
        if (produces == null) {
            return true;
        }
        for (String v : produces.value()) {
            if (v.startsWith(MediaType.APPLICATION_JSON)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnnotationNamed(Parameter parameter, String simpleName) {
        for (Annotation a : parameter.getAnnotations()) {
            if (a.annotationType().getSimpleName().equals(simpleName)) {
                return true;
            }
        }
        return false;
    }

    static List<Param> fieldsOf(Class<?> type) {
        if (type == null || type.isPrimitive() || type.getName().startsWith("java.")) {
            return List.of();
        }
        List<Param> out = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                    continue;
                }
                out.add(new Param(f.getName(), f.getGenericType().getTypeName().replace("java.lang.", "")
                        .replace("java.util.", "").replaceAll("com\\.cat2bug\\.[a-z.]+\\.", ""), "body"));
            }
        }
        return out;
    }

    private static String lowerFirst(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /** 测试用：重新扫描。 */
    static void reset() {
        operations = null;
    }
}
