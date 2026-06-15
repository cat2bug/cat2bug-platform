package com.cat2bug.framework.security.filter;

import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.web.service.ApiTokenService;
import com.cat2bug.framework.web.service.TokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Open API CAT2BUG-API-KEY 鉴权（勿加 @Component，避免 Native 下 CGLIB 代理 OncePerRequestFilter NPE）。
 */
public class ApiAuthenticationTokenFilter extends OncePerRequestFilter
{
    private final ApiTokenService apiTokenService;

    public ApiAuthenticationTokenFilter(ApiTokenService apiTokenService)
    {
        this.apiTokenService = apiTokenService;
    }

    @Override
    protected String getAlreadyFilteredAttributeName() {
        return "FILTERED_" + getClass().getName();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        LoginUser loginUser = apiTokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication()))
        {
            apiTokenService.verifyToken(loginUser);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        chain.doFilter(request, response);
    }
}
