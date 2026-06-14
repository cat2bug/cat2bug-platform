package com.cat2bug.nativeimage;

import com.cat2bug.framework.security.context.PermissionContextHolder;
import com.cat2bug.framework.web.service.PermissionService;
import com.cat2bug.framework.web.service.SysPermissionService;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;

/**
 * Spring Security @PreAuthorize SpEL（@ss.hasPermi 等）在 Native 下需反射注册。
 */
public class SecurityNativeRuntimeHints implements RuntimeHintsRegistrar
{
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader)
    {
        hints.reflection().registerType(PermissionService.class, MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(SysPermissionService.class, MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(PermissionContextHolder.class, MemberCategory.values());
        hints.reflection().registerType(DefaultMethodSecurityExpressionHandler.class, MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(MethodSecurityExpressionOperations.class, MemberCategory.INVOKE_PUBLIC_METHODS);
    }
}
