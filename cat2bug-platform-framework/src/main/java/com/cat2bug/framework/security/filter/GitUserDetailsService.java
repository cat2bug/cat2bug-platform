package com.cat2bug.framework.security.filter;

import com.cat2bug.framework.web.service.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 复用平台登录校验（design.md D4），但将任何非 {@link UsernameNotFoundException} 的运行时异常
 * （如账号已停用/已删除抛出的 {@code ServiceException}）统一折叠为「用户不存在」。
 *
 * <p>原因：{@code /login} 走的是 Controller 手动调用 {@code AuthenticationManager.authenticate()}
 * 并自行 catch 所有异常（见 {@code SysLoginService}），而 {@code /git/**} 走 Spring Security 内置的
 * {@code httpBasic()} + {@code BasicAuthenticationFilter}，后者只把
 * {@link org.springframework.security.core.AuthenticationException} 交给
 * {@code AuthenticationEntryPoint}（→ 401）；其余 RuntimeException 会直接向上抛穿透成 500，
 * 且可能在响应/日志中带出内部错误信息。统一折叠为 401 避免：
 * (1) 用户名枚举差异化响应，(2) 内部异常信息泄露，(3) 意外的 500。</p>
 */
public class GitUserDetailsService implements UserDetailsService
{
    private static final Logger log = LoggerFactory.getLogger(GitUserDetailsService.class);

    private final UserDetailsServiceImpl delegate;

    public GitUserDetailsService(UserDetailsServiceImpl delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        try
        {
            // 不可调用 delegate.loadUserByUsername：其中 passwordService.validate 依赖
            // AuthenticationContextHolder（仅 /login 会写入），Basic 认证会 NPE/失败并被折叠成 401。
            return delegate.loadUserByUsernameWithoutPasswordCheck(username);
        }
        catch (UsernameNotFoundException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            log.warn("Git Basic 认证失败：{}", username);
            throw new UsernameNotFoundException(username, e);
        }
    }
}
