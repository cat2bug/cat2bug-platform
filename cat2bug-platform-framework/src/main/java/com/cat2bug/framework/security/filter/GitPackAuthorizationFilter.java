package com.cat2bug.framework.security.filter;

import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.framework.security.git.GitPackRequestContext;
import com.cat2bug.system.domain.git.GitRepositoryRef;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.service.git.GitRepositoryLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Git Smart HTTP 授权关卡（specs/git-smart-http/spec.md 全部四条 Requirement 的授权部分）。
 *
 * <p>在 {@code BasicAuthenticationFilter} 之后运行——此时已确认 HTTP Basic 凭据有效
 * （3.2）；本过滤器只做「这个已认证用户，对这个仓库，能不能做这个操作」的判定（3.3/3.4），
 * 是 pull/push/force-push 授权的唯一关卡：Smart HTTP 控制器（admin 模块）不重复查库/查权限，
 * 只信任本过滤器写入的 {@link GitPackRequestContext} 请求属性。
 *
 * <p>勿加 {@code @Component}，避免 Native 下 CGLIB 代理 {@code OncePerRequestFilter} NPE
 * （与 {@link ApiPermissionFilter} 同规则）；由 {@code GitSecurityConfig} 显式声明为 {@code @Bean}。</p>
 */
public class GitPackAuthorizationFilter extends OncePerRequestFilter
{
    private static final Logger log = LoggerFactory.getLogger(GitPackAuthorizationFilter.class);

    /**
     * design.md D8 / Open Questions：clone URL 采用 {@code /git/p/{projectId}/{slug}.git/**}。
     * 仅承载 info/refs 与两个 pack service；dumb protocol 不支持（design.md D1：不落地外部 git 进程）。
     */
    private static final Pattern PACK_PATH = Pattern.compile(
            "^/git/p/(\\d+)/([^/]+)\\.git/(info/refs|git-upload-pack|git-receive-pack)$");

    private static final String SERVICE_UPLOAD_PACK = "git-upload-pack";
    private static final String SERVICE_RECEIVE_PACK = "git-receive-pack";

    private final GitRepositoryLookup gitRepositoryLookup;

    private final GitRepositoryAccessService gitRepositoryAccessService;

    public GitPackAuthorizationFilter(GitRepositoryLookup gitRepositoryLookup,
            GitRepositoryAccessService gitRepositoryAccessService)
    {
        this.gitRepositoryLookup = gitRepositoryLookup;
        this.gitRepositoryAccessService = gitRepositoryAccessService;
    }

    @Override
    protected String getAlreadyFilteredAttributeName()
    {
        return "FILTERED_" + getClass().getName();
    }

    /**
     * 本过滤器只服务 Git Smart HTTP。即便被 Spring Boot 误注册为全局 Servlet Filter，
     * 也绝不能对非 /git/** 请求 sendError(404)（否则 /login、/version、/captchaImage 会全部 404）。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        return !requestPath(request).startsWith("/git/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        String path = requestPath(request);
        Matcher matcher = PACK_PATH.matcher(path);
        if (!matcher.matches())
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long projectId = Long.valueOf(matcher.group(1));
        String slug = matcher.group(2);
        String endpoint = matcher.group(3);

        String service = resolveService(request, endpoint);
        if (service == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long userId = currentUserId();
        if (userId == null)
        {
            // 未携带/无效 Basic 凭据：不在此处自行 401（会丢 WWW-Authenticate 挑战头，git 客户端不会重试）。
            // 交给下游 AuthorizationFilter + BasicAuthenticationEntryPoint 走标准挑战流程。
            chain.doFilter(request, response);
            return;
        }

        Optional<GitRepositoryRef> repository = gitRepositoryLookup.findByProjectIdAndSlug(projectId, slug);
        if (!repository.isPresent())
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        boolean granted = SERVICE_UPLOAD_PACK.equals(service)
                ? gitRepositoryAccessService.canPull(userId, projectId)
                : gitRepositoryAccessService.canPush(userId, projectId);
        if (!granted)
        {
            log.info("Git Smart HTTP 拒绝：userId={} projectId={} slug={} service={}", userId, projectId, slug, service);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        boolean forcePushAllowed = gitRepositoryAccessService.canManage(userId, projectId);
        request.setAttribute(GitPackRequestContext.REQUEST_ATTRIBUTE,
                new GitPackRequestContext(repository.get(), forcePushAllowed));

        chain.doFilter(request, response);
    }

    /**
     * info/refs 通过 {@code ?service=} 决定意图（pull 或 push 的“预检”）；两个 POST 端点由路径本身决定。
     */
    private String resolveService(HttpServletRequest request, String endpoint)
    {
        if ("info/refs".equals(endpoint))
        {
            String service = request.getParameter("service");
            if (SERVICE_UPLOAD_PACK.equals(service) || SERVICE_RECEIVE_PACK.equals(service))
            {
                return service;
            }
            return null;
        }
        return endpoint;
    }

    private Long currentUserId()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
        {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof LoginUser ? ((LoginUser) principal).getUserId() : null;
    }

    private String requestPath(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
        {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }
}
