package com.cat2bug.framework.config;

import com.cat2bug.framework.security.filter.ApiAuthenticationTokenFilter;
import com.cat2bug.framework.security.filter.ApiPermissionFilter;
import com.cat2bug.framework.web.service.ApiTokenService;
import com.cat2bug.framework.web.service.ApiUserDetailsServiceImpl;
import com.cat2bug.system.service.ISysProjectApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Open API 路径安全链（/api/**）
 */
@Order(1)
@Configuration
@ConditionalOnProperty(prefix = "cat2bug.api", name = "enabled", havingValue = "true")
public class ApiSecurityConfig {

    @Autowired
    private ApiUserDetailsServiceImpl apiUserDetailsServiceImpl;

    @Bean
    public ApiAuthenticationTokenFilter apiAuthenticationTokenFilter(ApiTokenService apiTokenService) {
        return new ApiAuthenticationTokenFilter(apiTokenService);
    }

    @Bean
    public ApiPermissionFilter apiPermissionFilter(ISysProjectApiService sysProjectApiService) {
        return new ApiPermissionFilter(sysProjectApiService);
    }

    @Bean("apiAuthenticationManager")
    public AuthenticationManager apiAuthenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(apiUserDetailsServiceImpl);
        provider.setPasswordEncoder(apiPasswordEncoder());
        return new ProviderManager(provider);
    }

    private PasswordEncoder apiPasswordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "";
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return true;
            }
        };
    }

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity httpSecurity,
            ApiAuthenticationTokenFilter authenticationTokenFilter,
            ApiPermissionFilter apiPermissionFilter) throws Exception {
        httpSecurity
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(apiPermissionFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
