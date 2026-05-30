package com.cat2bug.framework.config;

import com.cat2bug.framework.config.properties.PermitAllUrlProperties;
import com.cat2bug.framework.security.filter.JwtAuthenticationTokenFilter;
import com.cat2bug.framework.security.handle.AuthenticationEntryPointImpl;
import com.cat2bug.framework.security.handle.LogoutSuccessHandlerImpl;
import com.cat2bug.framework.web.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.CorsFilter;

/**
 * Spring Security 6 配置（主站）
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Order(2)
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;

    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    @Autowired
    private JwtAuthenticationTokenFilter authenticationTokenFilter;

    @Autowired
    private CorsFilter corsFilter;

    @Autowired
    private PermitAllUrlProperties permitAllUrl;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(bCryptPasswordEncoder());
        return provider;
    }

    @Bean("manageAuthenticationManager")
    public AuthenticationManager manageAuthenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authenticationProvider(daoAuthenticationProvider())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.cacheControl(cache -> cache.disable()).frameOptions(frame -> frame.disable()))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(registry -> {
                    // @Anonymous 扫描的 URL 与 Ant 风格通配（含 {id}→*），避免 PathPattern 解析失败
                    permitAllUrl.getUrls().forEach(url ->
                            registry.requestMatchers(new AntPathRequestMatcher(url)).permitAll());
                    // 已安装后仍允许查询安装状态，供前端路由判断（与 SetupFilter 一致）
                    registry.requestMatchers(ant(HttpMethod.GET, "/setup/status")).permitAll();
                    registry.requestMatchers(ant(HttpMethod.GET, "/upgrade/status")).permitAll();
                    // 安装/升级向导由 SetupFilter、UpgradeFilter 管控；此处 permitAll 避免匿名请求被误判 401
                    registry.requestMatchers(ant("/setup/**")).permitAll();
                    registry.requestMatchers(ant("/upgrade/**")).permitAll();
                    registry.requestMatchers(
                            ant("/login"),
                            ant("/**/login"),
                            ant("/**/bind"),
                            ant("/logout"),
                            ant("/register"),
                            ant("/captchaImage"),
                            ant("/version"),
                            ant("/system/defect/*/shard/**"),
                            ant("/websocket/**")
                    ).permitAll();
                    registry.requestMatchers(
                            ant(HttpMethod.GET, "/"),
                            ant(HttpMethod.GET, "/index"),
                            ant(HttpMethod.GET, "/static/**"),
                            ant(HttpMethod.GET, "/*.html"),
                            ant(HttpMethod.GET, "/**/*.html"),
                            ant(HttpMethod.GET, "/**/*.css"),
                            ant(HttpMethod.GET, "/**/*.js"),
                            ant(HttpMethod.GET, "/profile/**"),
                            ant(HttpMethod.GET, "/docs/images/**")
                    ).permitAll();
                    registry.requestMatchers(
                            ant("/doc.html"),
                            ant("/swagger-ui.html"),
                            ant("/swagger-ui/**"),
                            ant("/swagger-resources/**"),
                            ant("/webjars/**"),
                            ant("/*/api-docs"),
                            ant("/druid/**"),
                            ant("/h2/**"),
                            ant("/v3/api-docs/**"),
                            ant("/doc.html/**")
                    ).permitAll();
                    registry.anyRequest().authenticated();
                })
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler));

        httpSecurity.addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterBefore(corsFilter, LogoutFilter.class);

        return httpSecurity.build();
    }

    private static AntPathRequestMatcher ant(String pattern) {
        return new AntPathRequestMatcher(pattern);
    }

    private static AntPathRequestMatcher ant(HttpMethod method, String pattern) {
        return new AntPathRequestMatcher(pattern, method.name());
    }
}
