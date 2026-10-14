package com.cat2bug.admin.service;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析并执行定时任务调用目标：{@code beanName.method(args)} 或 {@code 全限定类名.method(args)}。
 * 参数：'字符串' / "字符串"、true/false、数字L（Long）、数字D（Double）、其余按 Integer。
 */
public final class JobInvoker {

    private static final String[] WHITELIST = {"com.cat2bug"};
    private static final String[] ERROR_STR = {"java.net.URL", "javax.naming.InitialContext", "org.yaml.snakeyaml",
            "org.springframework", "org.apache", "com.cat2bug.common.utils.file", "com.cat2bug.common.config",
            "com.cat2bug.common.util", "com.cat2bug.framework"};

    private JobInvoker() {
    }

    public static void invoke(String invokeTarget) throws Exception {
        String beanName = beanName(invokeTarget);
        String methodName = methodName(invokeTarget);
        List<Object[]> params = methodParams(invokeTarget);
        if (isClassName(beanName)) {
            Object target = Class.forName(beanName).getDeclaredConstructor().newInstance();
            invokeMethod(target, methodName, params);
            return;
        }
        try (InstanceHandle<Object> handle = Arc.container().instance(beanName)) {
            if (!handle.isAvailable()) {
                throw new IllegalStateException("未找到任务 bean：" + beanName);
            }
            invokeMethod(handle.get(), methodName, params);
        }
    }

    /** 调用目标是否在白名单内（bean 所在包或全限定类名须属于 com.cat2bug，且不命中违规字符串）。 */
    public static boolean whiteList(String invokeTarget) {
        String packageName = substringBefore(invokeTarget, "(");
        if (countMatches(packageName, '.') > 1) {
            return containsAnyIgnoreCase(invokeTarget, WHITELIST);
        }
        String beanName = invokeTarget.split("\\.")[0];
        try (InstanceHandle<Object> handle = Arc.container().instance(beanName)) {
            if (!handle.isAvailable()) {
                return false;
            }
            String beanPackage = handle.get().getClass().getPackageName();
            return containsAnyIgnoreCase(beanPackage, WHITELIST) && !containsAnyIgnoreCase(beanPackage, ERROR_STR);
        }
    }

    public static boolean containsErrorString(String invokeTarget) {
        return containsAnyIgnoreCase(invokeTarget, ERROR_STR);
    }

    static boolean isClassName(String beanName) {
        return countMatches(beanName, '.') > 1;
    }

    static String beanName(String invokeTarget) {
        String name = substringBefore(invokeTarget, "(");
        int idx = name.lastIndexOf('.');
        return idx < 0 ? name : name.substring(0, idx);
    }

    static String methodName(String invokeTarget) {
        String name = substringBefore(invokeTarget, "(");
        int idx = name.lastIndexOf('.');
        return idx < 0 ? "" : name.substring(idx + 1);
    }

    static List<Object[]> methodParams(String invokeTarget) {
        int open = invokeTarget.indexOf('(');
        int close = invokeTarget.lastIndexOf(')');
        if (open < 0 || close <= open + 1) {
            return List.of();
        }
        String body = invokeTarget.substring(open + 1, close);
        if (body.isBlank()) {
            return List.of();
        }
        List<Object[]> params = new ArrayList<>();
        for (String raw : body.split(",(?=([^\"']*[\"'][^\"']*[\"'])*[^\"']*$)")) {
            String str = raw.trim();
            if (str.startsWith("'") || str.startsWith("\"")) {
                params.add(new Object[]{str.substring(1, str.length() - 1), String.class});
            } else if ("true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)) {
                params.add(new Object[]{Boolean.valueOf(str), Boolean.class});
            } else if (str.endsWith("L")) {
                params.add(new Object[]{Long.valueOf(str.substring(0, str.length() - 1)), Long.class});
            } else if (str.endsWith("D")) {
                params.add(new Object[]{Double.valueOf(str.substring(0, str.length() - 1)), Double.class});
            } else {
                params.add(new Object[]{Integer.valueOf(str), Integer.class});
            }
        }
        return params;
    }

    private static void invokeMethod(Object bean, String methodName, List<Object[]> params)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class<?>[] types = new Class<?>[params.size()];
        Object[] values = new Object[params.size()];
        for (int i = 0; i < params.size(); i++) {
            values[i] = params.get(i)[0];
            types[i] = (Class<?>) params.get(i)[1];
        }
        Method method = bean.getClass().getMethod(methodName, types);
        method.invoke(bean, values);
    }

    private static String substringBefore(String text, String separator) {
        int idx = text.indexOf(separator);
        return idx < 0 ? text : text.substring(0, idx);
    }

    private static int countMatches(String text, char c) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    static boolean containsAnyIgnoreCase(String text, String... needles) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase();
        for (String needle : needles) {
            if (lower.contains(needle.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
