package com.cat2bug.framework.config;

import com.cat2bug.framework.security.filter.GitPackAuthorizationFilter;
import com.cat2bug.framework.security.filter.GitUserDetailsService;
import com.cat2bug.framework.web.service.UserDetailsServiceImpl;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.service.git.GitRepositoryLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Git Smart HTTP 安全链（{@code /git/**}，design.md D4/D8，specs/git-smart-http/spec.md）。
 *
 * <p>与 {@code ApiSecurityConfig}（{@code /api/**}，@Order(1)）同构：用独立的
 * {@code securityMatcher} 把 {@code /git/**} 从主站 {@code SecurityConfig}（JWT，@Order(2)）
 * 的处理范围中完全切走，互不影响：
 * <ul>
 *   <li>认证：HTTP Basic，凭据校验复用平台登录（{@link UserDetailsServiceImpl}），
 *       而非 JWT/Session；</li>
 *   <li>CSRF：关闭（git 客户端不发 CSRF token，且本链 STATELESS 无 Cookie 会话）；</li>
 *   <li>授权：认证通过后交给 {@link GitPackAuthorizationFilter} 按项目角色矩阵判定
 *       pull/push/force-push（3.3/3.4）。</li>
 * </ul>
 */
@Order(0)
@Configuration
public class GitSecurityConfig
{
    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private GitRepositoryLookup gitRepositoryLookup;

    @Autowired
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Bean
    public GitUserDetailsService gitUserDetailsService()
    {
        return new GitUserDetailsService(userDetailsServiceImpl);
    }

    /**
     * 独立于主站 {@code manageAuthenticationManager}：同样的用户校验逻辑与密码编码器，
     * 但异常处理专为 Basic 挑战/响应语义定制（见 {@link GitUserDetailsService}），
     * 避免账号停用/删除等业务异常把 401 变成 500。
     */
    @Bean("gitAuthenticationManager")
    public AuthenticationManager gitAuthenticationManager()
    {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(gitUserDetailsService());
        provider.setPasswordEncoder(bCryptPasswordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public GitPackAuthorizationFilter gitPackAuthorizationFilter()
    {
        return new GitPackAuthorizationFilter(gitRepositoryLookup, gitRepositoryAccessService);
    }

    /**
     * 禁止 Spring Boot 把该 Filter 注册进全局 Servlet 链；仅由 {@link #gitFilterChain} 挂到 /git/**。
     */
    @Bean
    public FilterRegistrationBean<GitPackAuthorizationFilter> gitPackAuthorizationFilterRegistration(
            GitPackAuthorizationFilter gitPackAuthorizationFilter)
    {
        FilterRegistrationBean<GitPackAuthorizationFilter> registration =
                new FilterRegistrationBean<>(gitPackAuthorizationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain gitFilterChain(HttpSecurity httpSecurity,
            @Qualifier("gitAuthenticationManager") AuthenticationManager gitAuthenticationManager,
            GitPackAuthorizationFilter gitPackAuthorizationFilter) throws Exception
    {
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("Cat2Bug Git");

        httpSecurity
                .securityMatcher("/git/**")
                .authenticationManager(gitAuthenticationManager)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterAfter(gitPackAuthorizationFilter, BasicAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
