package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.AuthService;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.git.GitRepositoryRef;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.git.GitPackRequestContext;
import com.cat2bug.framework.security.git.GitSmartHttpPathSupport;
import com.cat2bug.framework.security.git.GitSmartHttpPathSupport.GitSmartHttpPathMatch;
import com.cat2bug.system.service.SysPermissionService;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.service.git.GitRepositoryLookup;
import com.cat2bug.system.service.git.GitWebhookEnqueueService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PacketLineOut;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.RefAdvertiser;
import org.eclipse.jgit.transport.UploadPack;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Git Smart HTTP：info/refs、git-upload-pack、git-receive-pack。
 *
 * <p>对齐 Spring {@code GitSmartHttpController}；JWT 过滤器已跳过本路径，此处自行做 HTTP Basic
 * + 仓级 pull/push 授权，并写入 {@link GitPackRequestContext} 语义（force-push 仅 manage）。</p>
 */
@Path("/")
public class GitSmartHttpResource {

    private static final Logger LOG = Logger.getLogger(GitSmartHttpResource.class);

    private static final String SERVICE_UPLOAD_PACK = "git-upload-pack";
    private static final String SERVICE_RECEIVE_PACK = "git-receive-pack";
    private static final String BASIC_REALM = "Basic realm=\"Cat2Bug Git\"";

    @Inject
    AuthService authService;

    @Inject
    SysPermissionService sysPermissionService;

    @Inject
    AuthContext authContext;

    @Inject
    GitRepositoryLookup gitRepositoryLookup;

    @Inject
    GitRepositoryAccessService gitRepositoryAccessService;

    @Inject
    GitWebhookEnqueueService gitWebhookEnqueueService;

    @GET
    @Path("git/p/{projectId}/{slug}.git/info/refs")
    public Response infoRefsLegacy(@PathParam("projectId") Long projectId,
                                   @PathParam("slug") String slug,
                                   @QueryParam("service") String service,
                                   @Context HttpHeaders headers,
                                   @Context UriInfo uriInfo) {
        return infoRefs(service, headers, uriInfo);
    }

    @GET
    @Path("{teamKey}/{projectKey}/code/{slug}.git/info/refs")
    public Response infoRefsNamespaced(@PathParam("teamKey") String teamKey,
                                       @PathParam("projectKey") String projectKey,
                                       @PathParam("slug") String slug,
                                       @QueryParam("service") String service,
                                       @Context HttpHeaders headers,
                                       @Context UriInfo uriInfo) {
        return infoRefs(service, headers, uriInfo);
    }

    @POST
    @Path("git/p/{projectId}/{slug}.git/git-upload-pack")
    @Consumes("application/x-git-upload-pack-request")
    @Produces("application/x-git-upload-pack-result")
    public Response uploadPackLegacy(@Context HttpHeaders headers,
                                     @Context UriInfo uriInfo,
                                     InputStream body) {
        return uploadPack(headers, uriInfo, body);
    }

    @POST
    @Path("{teamKey}/{projectKey}/code/{slug}.git/git-upload-pack")
    @Consumes("application/x-git-upload-pack-request")
    @Produces("application/x-git-upload-pack-result")
    public Response uploadPackNamespaced(@Context HttpHeaders headers,
                                         @Context UriInfo uriInfo,
                                         InputStream body) {
        return uploadPack(headers, uriInfo, body);
    }

    @POST
    @Path("git/p/{projectId}/{slug}.git/git-receive-pack")
    @Consumes("application/x-git-receive-pack-request")
    @Produces("application/x-git-receive-pack-result")
    public Response receivePackLegacy(@Context HttpHeaders headers,
                                      @Context UriInfo uriInfo,
                                      InputStream body) {
        return receivePack(headers, uriInfo, body);
    }

    @POST
    @Path("{teamKey}/{projectKey}/code/{slug}.git/git-receive-pack")
    @Consumes("application/x-git-receive-pack-request")
    @Produces("application/x-git-receive-pack-result")
    public Response receivePackNamespaced(@Context HttpHeaders headers,
                                          @Context UriInfo uriInfo,
                                          InputStream body) {
        return receivePack(headers, uriInfo, body);
    }

    private Response infoRefs(String service, HttpHeaders headers, UriInfo uriInfo) {
        if (!SERVICE_UPLOAD_PACK.equals(service) && !SERVICE_RECEIVE_PACK.equals(service)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        AuthOutcome auth = authorize(headers, uriInfo, service);
        if (auth.response != null) {
            return auth.response;
        }
        GitPackRequestContext context = auth.context;
        return Response.ok((StreamingOutput) output -> {
            try (Repository repository = openRepository(context.getRepository())) {
                if (repository == null) {
                    throw new IOException("repository missing");
                }
                PacketLineOut pckOut = new PacketLineOut(output);
                pckOut.writeString("# service=" + service + "\n");
                pckOut.end();
                RefAdvertiser.PacketLineOutRefAdvertiser advertiser =
                        new RefAdvertiser.PacketLineOutRefAdvertiser(pckOut);
                if (SERVICE_UPLOAD_PACK.equals(service)) {
                    UploadPack uploadPack = new UploadPack(repository);
                    uploadPack.setBiDirectionalPipe(false);
                    uploadPack.sendAdvertisedRefs(advertiser);
                } else {
                    ReceivePack receivePack = new ReceivePack(repository);
                    receivePack.setBiDirectionalPipe(false);
                    receivePack.sendAdvertisedRefs(advertiser);
                }
                output.flush();
            }
        }).type("application/x-" + service + "-advertisement")
                .header("Cache-Control", "no-cache")
                .build();
    }

    private Response uploadPack(HttpHeaders headers, UriInfo uriInfo, InputStream body) {
        AuthOutcome auth = authorize(headers, uriInfo, SERVICE_UPLOAD_PACK);
        if (auth.response != null) {
            return auth.response;
        }
        GitPackRequestContext context = auth.context;
        InputStream packBody;
        try {
            packBody = materializeBody(body, headers);
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok((StreamingOutput) output -> {
            try (Repository repository = openRepository(context.getRepository());
                 InputStream in = packBody) {
                if (repository == null) {
                    throw new IOException("repository missing");
                }
                UploadPack uploadPack = new UploadPack(repository);
                uploadPack.setBiDirectionalPipe(false);
                uploadPack.upload(in, output, null);
                output.flush();
            }
        }).type("application/x-git-upload-pack-result")
                .header("Cache-Control", "no-cache")
                .build();
    }

    private Response receivePack(HttpHeaders headers, UriInfo uriInfo, InputStream body) {
        AuthOutcome auth = authorize(headers, uriInfo, SERVICE_RECEIVE_PACK);
        if (auth.response != null) {
            return auth.response;
        }
        GitPackRequestContext context = auth.context;
        Long pusherUserId = auth.userId;
        InputStream packBody;
        try {
            packBody = materializeBody(body, headers);
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok((StreamingOutput) output -> {
            try (Repository repository = openRepository(context.getRepository());
                 InputStream in = packBody) {
                if (repository == null) {
                    throw new IOException("repository missing");
                }
                ReceivePack receivePack = new ReceivePack(repository);
                receivePack.setBiDirectionalPipe(false);
                receivePack.setAllowNonFastForwards(context.isForcePushAllowed());
                GitRepositoryRef repoRef = context.getRepository();
                receivePack.setPostReceiveHook((pack, commands) -> {
                    try {
                        gitWebhookEnqueueService.onReceivePackSuccessful(
                                repoRef.getRepositoryId(), pusherUserId, pack.getRepository(), commands);
                    } catch (Exception ex) {
                        LOG.warnf("Webhook PostReceiveHook 失败：repositoryId=%s %s",
                                repoRef.getRepositoryId(), ex.toString());
                    }
                });
                receivePack.receive(in, output, null);
                output.flush();
            }
        }).type("application/x-git-receive-pack-result")
                .header("Cache-Control", "no-cache")
                .build();
    }

    private AuthOutcome authorize(HttpHeaders headers, UriInfo uriInfo, String service) {
        String path = absolutePath(uriInfo);
        Optional<GitSmartHttpPathMatch> pathMatch = GitSmartHttpPathSupport.parsePackPath(path);
        if (pathMatch.isEmpty()) {
            return AuthOutcome.error(Response.status(Response.Status.NOT_FOUND).build());
        }
        LoginUser loginUser = authenticateBasic(headers);
        if (loginUser == null) {
            return AuthOutcome.error(Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_REALM)
                    .build());
        }
        authContext.setLoginUser(loginUser);
        Long userId = loginUser.getUserId();

        GitSmartHttpPathMatch match = pathMatch.get();
        Optional<GitRepositoryRef> repository = match.isLegacy()
                ? gitRepositoryLookup.findByProjectIdAndSlug(match.getProjectId(), match.getSlug())
                : gitRepositoryLookup.findByTeamKeyAndProjectKeyAndSlug(
                        match.getTeamKey(), match.getProjectKey(), match.getSlug());
        if (repository.isEmpty()) {
            return AuthOutcome.error(Response.status(Response.Status.NOT_FOUND).build());
        }
        Long repositoryId = repository.get().getRepositoryId();
        boolean granted = SERVICE_UPLOAD_PACK.equals(service)
                ? gitRepositoryAccessService.canPull(userId, repositoryId)
                : gitRepositoryAccessService.canPush(userId, repositoryId);
        if (!granted) {
            LOG.infof("Git Smart HTTP 拒绝：userId=%s repositoryId=%s service=%s",
                    userId, repositoryId, service);
            return AuthOutcome.error(Response.status(Response.Status.FORBIDDEN).build());
        }
        boolean forcePushAllowed = gitRepositoryAccessService.canManage(userId, repositoryId);
        return AuthOutcome.ok(new GitPackRequestContext(repository.get(), forcePushAllowed), userId);
    }

    private LoginUser authenticateBasic(HttpHeaders headers) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            return null;
        }
        String encoded = authorization.substring(6).trim();
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        int colon = decoded.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String username = decoded.substring(0, colon);
        String password = decoded.substring(colon + 1);
        SysUser user = authService.authenticate(username, password);
        if (user == null) {
            return null;
        }
        return sysPermissionService.createLoginUser(user);
    }

    private static Repository openRepository(GitRepositoryRef ref) throws IOException {
        File gitDir = new File(ref.getDiskPath()).getAbsoluteFile();
        if (!gitDir.isDirectory()) {
            LOG.warnf("Git 仓库磁盘路径不存在：repositoryId=%s path=%s",
                    ref.getRepositoryId(), gitDir.getAbsolutePath());
            return null;
        }
        return new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .setBare()
                .setMustExist(true)
                .build();
    }

    private static InputStream materializeBody(InputStream input, HttpHeaders headers) throws IOException {
        InputStream stream = input;
        String encoding = headers.getHeaderString("Content-Encoding");
        if (encoding != null && encoding.toLowerCase().contains("gzip")) {
            stream = new GZIPInputStream(stream);
        }
        String contentLength = headers.getHeaderString(HttpHeaders.CONTENT_LENGTH);
        if (contentLength != null) {
            try {
                if (Long.parseLong(contentLength) >= 0) {
                    return stream;
                }
            } catch (NumberFormatException ignore) {
                // fall through to materialize
            }
        }
        java.nio.file.Path tmp = Files.createTempFile("cat2bug-git-pack-", ".bin");
        try {
            Files.copy(stream, tmp, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            stream.close();
        }
        return Files.newInputStream(tmp, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE);
    }

    private static String absolutePath(UriInfo uriInfo) {
        String path = uriInfo.getPath();
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static final class AuthOutcome {
        final Response response;
        final GitPackRequestContext context;
        final Long userId;

        private AuthOutcome(Response response, GitPackRequestContext context, Long userId) {
            this.response = response;
            this.context = context;
            this.userId = userId;
        }

        static AuthOutcome error(Response response) {
            return new AuthOutcome(response, null, null);
        }

        static AuthOutcome ok(GitPackRequestContext context, Long userId) {
            return new AuthOutcome(null, context, userId);
        }
    }
}
