package com.cat2bug.web.controller.git;

import com.cat2bug.framework.security.git.GitPackRequestContext;
import com.cat2bug.system.domain.git.GitRepositoryRef;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PacketLineOut;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.RefAdvertiser;
import org.eclipse.jgit.transport.UploadPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;

/**
 * Git Smart HTTP：info/refs、git-upload-pack、git-receive-pack。
 *
 * <p>specs/git-smart-http/spec.md Requirement「Smart HTTP clone and push via JGit」——
 * 全程只用 {@code org.eclipse.jgit.transport} 里的 {@link UploadPack}/{@link ReceivePack}/
 * {@link PacketLineOut}/{@link RefAdvertiser} 手工拼协议，<b>不使用</b> {@code org.eclipse.jgit.http.server}
 * 的 Servlet（JGit 该模块仍是 {@code javax.servlet} API，与本项目 Spring Boot 3 / Jakarta Servlet
 * 不兼容），更绝不 fork 系统 {@code git} 进程（design.md D1）。</p>
 *
 * <p>鉴权已在 {@code GitPackAuthorizationFilter}（framework 模块，Spring Security
 * {@code /git/**} 链）完成：本控制器只信任其写入的 {@link GitPackRequestContext} 请求属性
 * （不存在时视为异常兜底，直接 403），不重复查角色。</p>
 */
@RestController
@ConditionalOnClass({Repository.class, GitPackRequestContext.class})
public class GitSmartHttpController
{
    private static final Logger log = LoggerFactory.getLogger(GitSmartHttpController.class);

    private static final String SERVICE_UPLOAD_PACK = "git-upload-pack";
    private static final String SERVICE_RECEIVE_PACK = "git-receive-pack";

    private static final String LEGACY_BASE = "/git/p/{projectId}/{slug}.git";
    private static final String NAMESPACED_BASE = "/{teamKey}/{projectKey}/code/{slug}.git";

    @GetMapping({LEGACY_BASE + "/info/refs", NAMESPACED_BASE + "/info/refs"})
    public void infoRefs(@RequestParam(value = "service", required = false) String service,
            HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if (!SERVICE_UPLOAD_PACK.equals(service) && !SERVICE_RECEIVE_PACK.equals(service))
        {
            // dumb protocol 不支持（design.md D1）
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        GitPackRequestContext context = requireContext(request, response);
        if (context == null)
        {
            return;
        }

        Repository repository = openRepository(context.getRepository(), response);
        if (repository == null)
        {
            return;
        }
        try
        {
            response.setContentType("application/x-" + service + "-advertisement");
            response.setHeader("Cache-Control", "no-cache");

            PacketLineOut pckOut = new PacketLineOut(response.getOutputStream());
            pckOut.writeString("# service=" + service + "\n");
            pckOut.end();

            RefAdvertiser.PacketLineOutRefAdvertiser advertiser = new RefAdvertiser.PacketLineOutRefAdvertiser(pckOut);
            if (SERVICE_UPLOAD_PACK.equals(service))
            {
                UploadPack uploadPack = new UploadPack(repository);
                uploadPack.setBiDirectionalPipe(false);
                uploadPack.sendAdvertisedRefs(advertiser);
            }
            else
            {
                ReceivePack receivePack = new ReceivePack(repository);
                receivePack.setBiDirectionalPipe(false);
                receivePack.sendAdvertisedRefs(advertiser);
            }
            response.flushBuffer();
        }
        finally
        {
            repository.close();
        }
    }

    @PostMapping(value = {LEGACY_BASE + "/git-upload-pack", NAMESPACED_BASE + "/git-upload-pack"},
            consumes = "application/x-git-upload-pack-request")
    public void uploadPack(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        GitPackRequestContext context = requireContext(request, response);
        if (context == null)
        {
            return;
        }

        Repository repository = openRepository(context.getRepository(), response);
        if (repository == null)
        {
            return;
        }
        try
        {
            response.setContentType("application/x-git-upload-pack-result");
            response.setHeader("Cache-Control", "no-cache");

            UploadPack uploadPack = new UploadPack(repository);
            uploadPack.setBiDirectionalPipe(false);
            uploadPack.upload(requestBody(request), response.getOutputStream(), null);
            response.flushBuffer();
        }
        finally
        {
            repository.close();
        }
    }

    @PostMapping(value = {LEGACY_BASE + "/git-receive-pack", NAMESPACED_BASE + "/git-receive-pack"},
            consumes = "application/x-git-receive-pack-request")
    public void receivePack(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        GitPackRequestContext context = requireContext(request, response);
        if (context == null)
        {
            return;
        }

        Repository repository = openRepository(context.getRepository(), response);
        if (repository == null)
        {
            return;
        }
        try
        {
            response.setContentType("application/x-git-receive-pack-result");
            response.setHeader("Cache-Control", "no-cache");

            ReceivePack receivePack = new ReceivePack(repository);
            receivePack.setBiDirectionalPipe(false);
            // 3.4：非 manage 用户不允许非快进；JGit 内建校验会按此逐条拒绝非快进的 ref update
            receivePack.setAllowNonFastForwards(context.isForcePushAllowed());
            // 推送人身份仅由 HTTP Basic + 项目 push 权限识别，不强制 commit author/committer 与账号一致
            // （便于导入既有历史；提交作者仍保留在 Git 元数据中供浏览展示）
            try
            {
                receivePack.receive(requestBody(request), response.getOutputStream(), null);
                response.flushBuffer();
            }
            catch (IOException e)
            {
                log.error("git-receive-pack 失败：project path={}, msg={}",
                        request.getRequestURI(), e.toString());
                throw e;
            }
        }
        finally
        {
            repository.close();
        }
    }

    private GitPackRequestContext requireContext(HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        Object attribute = request.getAttribute(GitPackRequestContext.REQUEST_ATTRIBUTE);
        if (attribute instanceof GitPackRequestContext)
        {
            return (GitPackRequestContext) attribute;
        }
        // GitPackAuthorizationFilter 未放行本请求本不该到达这里；兜底拒绝而不是裸抛异常
        log.warn("Git Smart HTTP 请求缺少鉴权上下文：{}", request.getRequestURI());
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return null;
    }

    private Repository openRepository(GitRepositoryRef ref, HttpServletResponse response) throws IOException
    {
        // 历史数据可能存相对路径；统一转绝对路径，避免进程 cwd 变化后找不到仓
        File gitDir = new File(ref.getDiskPath()).getAbsoluteFile();
        if (!gitDir.isDirectory())
        {
            log.warn("Git 仓库磁盘路径不存在：repositoryId={}, path={}", ref.getRepositoryId(), gitDir.getAbsolutePath());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        return new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .setBare()
                .setMustExist(true)
                .build();
    }

    /**
     * git 客户端可能对 upload-pack/receive-pack 请求体做 gzip 压缩（{@code Content-Encoding: gzip}）。
     * <p>当请求体超过客户端 {@code http.postBuffer}（默认约 1MiB）时，Git 会改用
     * {@code Transfer-Encoding: chunked}。部分环境下 JGit 对流式 chunked 输入读取不完整，
     * 表现为 {@code unexpected disconnect while reading sideband packet}；因此对未知长度
     * （chunked）请求先落到临时文件再交给 JGit。</p>
     */
    private InputStream requestBody(HttpServletRequest request) throws IOException
    {
        InputStream input = request.getInputStream();
        String encoding = request.getHeader("Content-Encoding");
        if (encoding != null && encoding.toLowerCase().contains("gzip"))
        {
            input = new GZIPInputStream(input);
        }
        long contentLength = request.getContentLengthLong();
        if (contentLength >= 0)
        {
            return input;
        }
        // chunked / 未知长度：物化后再读，避免 receive-pack 过早 EOF
        Path tmp = Files.createTempFile("cat2bug-git-pack-", ".bin");
        try
        {
            Files.copy(input, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        finally
        {
            input.close();
        }
        return Files.newInputStream(tmp, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE);
    }
}
