package com.cat2bug.framework.config;

import com.cat2bug.framework.config.properties.PermitAllUrlProperties;
import com.cat2bug.framework.security.filter.ApiAuthenticationTokenFilter;
import com.cat2bug.framework.security.handle.AuthenticationEntryPointImpl;
import com.cat2bug.framework.security.handle.LogoutSuccessHandlerImpl;
import com.cat2bug.framework.web.service.ApiUserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * spring security配置
 * 
 * @author ruoyi
 */
@Order(1)
@Configuration
@DependsOn("apiAuthenticationTokenFilter")
@ConditionalOnProperty(prefix = "cat2bug.api", name = "enabled", havingValue = "true")
public class ApiSecurityConfig extends WebSecurityConfigurerAdapter
{
    /**
     * 认证失败处理类
     */
    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;

    /**
     * 退出处理类
     */
    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    /**
     * token认证过滤器
     */
    @Autowired
    private ApiAuthenticationTokenFilter authenticationTokenFilter;
    /**
     * 跨域过滤器
     */
    @Autowired
    private CorsFilter corsFilter;

    /**
     * 允许匿名访问的地址
     */
    @Autowired
    private PermitAllUrlProperties permitAllUrl;

    @Autowired
    private ApiUserDetailsServiceImpl apiUserDetailsServiceImpl;

    /**
     * 解决 无法直接注入 AuthenticationManager
     *
     * @return
     * @throws Exception
     */
    @Bean("apiAuthenticationManager")
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception
    {
        return super.authenticationManagerBean();
    }

    /**
     * anyRequest          |   匹配所有请求路径
     * access              |   SpringEl表达式结果为true时可以访问
     * anonymous           |   匿名可以访问
     * denyAll             |   用户不能访问
     * fullyAuthenticated  |   用户完全认证可以访问（非remember-me下自动登录）
     * hasAnyAuthority     |   如果有参数，参数表示权限，则其中任何一个权限可以访问
     * hasAnyRole          |   如果有参数，参数表示角色，则其中任何一个角色可以访问
     * hasAuthority        |   如果有参数，参数表示权限，则其权限可以访问
     * hasIpAddress        |   如果有参数，参数表示IP地址，如果用户IP和参数匹配，则可以访问
     * hasRole             |   如果有参数，参数表示角色，则其角色可以访问
     * permitAll           |   用户可以任意访问
     * rememberMe          |   允许通过remember-me登录的用户访问
     * authenticated       |   用户登录后可访问
     */
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception
    {
        // 注解标记允许匿名访问的url
//        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry = httpSecurity.authorizeRequests();
//        permitAllUrl.getUrls().forEach(url -> registry.antMatchers(url).permitAll());

        httpSecurity
                .csrf().disable()
                .requestMatchers(matchers -> matchers
                        .antMatchers("/api/**") // apply JWTSecurityConfig to requests matching "/api/**"
                )
                .authorizeRequests(authz -> authz
                        .anyRequest().authenticated()
                )
                .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * 身份认证接口
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception
    {
         auth.userDetailsService(apiUserDetailsServiceImpl).passwordEncoder(new PasswordEncoder() {
             @Override
             public String encode(CharSequence rawPassword) {
                 return null;
             }

             @Override
             public boolean matches(CharSequence rawPassword, String encodedPassword) {
                 return true;
             }
         });
    }
}
