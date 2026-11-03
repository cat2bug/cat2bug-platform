package com.cat2bug.admin.service;

import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.domain.SysCaseType;
import com.cat2bug.common.domain.SysComment;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysDefectLog;
import com.cat2bug.common.domain.SysDocument;
import com.cat2bug.common.domain.SysGitRepository;
import com.cat2bug.common.domain.SysGitRepositoryMember;
import com.cat2bug.common.domain.SysModule;
import com.cat2bug.common.domain.SysPlan;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysProjectDefectField;
import com.cat2bug.common.domain.SysProjectMcp;
import com.cat2bug.common.domain.SysProjectPush;
import com.cat2bug.common.domain.SysProjectWorkCalendar;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.CompressUtil;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.config.Cat2BugProfileConfig;
import com.cat2bug.system.mapper.SysCaseTypeMapper;
import com.cat2bug.system.mapper.SysCommentMapper;
import com.cat2bug.system.mapper.SysDefectMapper;
import com.cat2bug.system.mapper.SysDocumentMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMemberMapper;
import com.cat2bug.system.mapper.SysPlanMapper;
import com.cat2bug.system.mapper.SysProjectDefectFieldMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysProjectWorkCalendarMapper;
import com.cat2bug.system.service.SysCaseService;
import com.cat2bug.system.service.SysDefectLogService;
import com.cat2bug.system.service.SysModuleService;
import com.cat2bug.system.service.SysUserProjectService;
import com.cat2bug.system.service.mcp.SysProjectMcpService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProjectCloudPushService {

    private static final Logger LOG = Logger.getLogger(ProjectCloudPushService.class);
    private static final String AUTH_TOKEN_HEADER_NAME = "CAT2BUG-API-KEY";
    private static final String DEFECT_LOG_COMMENT_TYPE = "defect_log";
    private static final int LIST_ALL_LIMIT = Integer.MAX_VALUE;

    @Inject
    SysProjectMapper sysProjectMapper;

    @Inject
    SysUserProjectService sysUserProjectService;

    @Inject
    SysCaseService sysCaseService;

    @Inject
    SysDefectMapper sysDefectMapper;

    @Inject
    SysDefectLogService sysDefectLogService;

    @Inject
    SysCommentMapper sysCommentMapper;

    @Inject
    SysPlanMapper sysPlanMapper;

    @Inject
    SysModuleService sysModuleService;

    @Inject
    SysDocumentMapper sysDocumentMapper;

    @Inject
    SysCaseTypeMapper sysCaseTypeMapper;

    @Inject
    SysProjectDefectFieldMapper sysProjectDefectFieldMapper;

    @Inject
    SysProjectWorkCalendarMapper sysProjectWorkCalendarMapper;

    @Inject
    SysProjectMcpService sysProjectMcpService;

    @Inject
    SysGitRepositoryMapper sysGitRepositoryMapper;

    @Inject
    SysGitRepositoryMemberMapper sysGitRepositoryMemberMapper;

    @Inject
    Cat2BugProfileConfig profileConfig;

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "2020")
    int httpPort;

    @ConfigProperty(name = "cat2bug.temp", defaultValue = "./uploadTemp")
    String tempPath;

    @ConfigProperty(name = "cat2bug.version", defaultValue = "1.0.0")
    String systemVersion;

    @ConfigProperty(name = "cat2bug.cloud.host", defaultValue = "https://cloud.cat2bug.com/prod-api")
    String cloudUrl;

    public interface PushProgress {
        void phase(int percent, String phase, String detail);

        void log(String level, String message);
    }

    private static final PushProgress NOOP_PROGRESS = new PushProgress() {
        @Override
        public void phase(int percent, String phase, String detail) {
        }

        @Override
        public void log(String level, String message) {
        }
    };

    public boolean pushToCloud(Long projectId, String pullKey) throws IOException {
        return pushToCloud(projectId, pullKey, NOOP_PROGRESS);
    }

    public boolean pushToCloud(Long projectId, String pullKey, PushProgress progress) throws IOException {
        PushProgress sink = progress != null ? progress : NOOP_PROGRESS;
        if (projectId == null) {
            throw new ApiException("项目ID不能为空");
        }
        if (StringUtils.isBlank(pullKey)) {
            throw new ApiException("同步密钥不能为空");
        }
        Path tempDir = Path.of(tempPath);
        Files.createDirectories(tempDir);
        String fileName = "tempPullProject-" + UUID.randomUUID() + ".zip";
        String compressFileName = tempDir.resolve(fileName).toString();
        CompressUtil compressUtil = CompressUtil.createStream(compressFileName);
        try {
            sink.phase(2, "写入元数据", "写入 system / project 元数据");
            SysProjectPush projectPush = new SysProjectPush();
            projectPush.setVersion(systemVersion);
            projectPush.setPushKey(pullKey);
            projectPush.setPackageSchema(SysProjectPush.PACKAGE_SCHEMA_1_1);
            compressUtil.addJsonFile("system.json", projectPush);

            SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
            compressUtil.addJsonFile("project.json", project);

            sink.phase(8, "打包成员", null);
            List<SysUser> memberList = sysUserProjectService.selectSysUserListByProjectId(projectId);
            compressUtil.addJsonFile("member.json", memberList);
            for (SysUser member : memberList) {
                if (StringUtils.isNotBlank(member.getAvatar())) {
                    compressFile(compressUtil, member.getAvatar());
                }
            }

            sink.phase(15, "打包团队与项目配置", null);
            packTeamAndProjectConfig(compressUtil, project);

            sink.phase(25, "打包用例", null);
            SysCase caseQuery = new SysCase();
            caseQuery.setProjectId(projectId);
            List<SysCase> caseList = sysCaseService.selectSysCaseListAll(caseQuery);
            compressUtil.addJsonFile("case.json", caseList);
            for (SysCase c : caseList) {
                compressUrls(compressUtil, c.getImgUrls());
                compressUrls(compressUtil, c.getAnnexUrls());
            }
            sink.log("INFO", "用例 " + (caseList == null ? 0 : caseList.size()) + " 条");

            sink.phase(40, "打包缺陷", null);
            SysDefect defectQuery = new SysDefect();
            defectQuery.setProjectId(projectId);
            List<SysDefect> defectList = sysDefectMapper.selectSysDefectList(defectQuery, 0, LIST_ALL_LIMIT);
            compressUtil.addJsonFile("defect.json", defectList);
            for (SysDefect d : defectList) {
                compressUrls(compressUtil, d.getImgUrls());
                compressUrls(compressUtil, d.getAnnexUrls());
                compressCustomFieldPaths(compressUtil, d.getCustomFields());
            }
            sink.log("INFO", "缺陷 " + (defectList == null ? 0 : defectList.size()) + " 条");

            sink.phase(55, "打包缺陷日志与评论", null);
            List<SysDefectLog> defectLogList = new ArrayList<>();
            for (SysDefect d : defectList) {
                SysDefectLog logQuery = new SysDefectLog();
                logQuery.setDefectId(d.getDefectId());
                defectLogList.addAll(sysDefectLogService.selectSysDefectLogList(logQuery));
            }
            defectLogList = defectLogList.stream().filter(l -> l != null).collect(Collectors.toList());
            compressUtil.addJsonFile("defect_log.json", defectLogList);

            List<SysComment> defectCommentList = new ArrayList<>();
            for (SysDefectLog log : defectLogList) {
                SysComment comment = new SysComment();
                comment.setCorrelationId(log.getDefectId());
                comment.setModuleType(DEFECT_LOG_COMMENT_TYPE);
                defectCommentList.addAll(sysCommentMapper.selectSysCommentList(comment, 0, LIST_ALL_LIMIT));
            }
            defectCommentList = defectCommentList.stream().filter(l -> l != null).collect(Collectors.toList());
            compressUtil.addJsonFile("defect_log_comment.json", defectCommentList);
            sink.log("INFO", "缺陷日志 " + defectLogList.size() + " 条，评论 " + defectCommentList.size() + " 条");

            sink.phase(65, "打包计划与交付物", null);
            SysPlan planQuery = new SysPlan();
            planQuery.setProjectId(projectId);
            List<SysPlan> planList = sysPlanMapper.selectSysPlanList(planQuery, 0, LIST_ALL_LIMIT);
            for (SysPlan plan : planList) {
                plan.setSysPlanItemList(sysPlanMapper.selectSysPlanItemListByPlanId(plan.getPlanId()));
            }
            compressUtil.addJsonFile("plan.json", planList);

            SysModule moduleQuery = new SysModule();
            moduleQuery.setProjectId(projectId);
            compressUtil.addJsonFile("module.json", sysModuleService.selectSysModuleList(moduleQuery));

            sink.phase(72, "打包文档", null);
            SysDocument documentQuery = new SysDocument();
            documentQuery.setProjectId(projectId);
            List<SysDocument> documentList =
                    sysDocumentMapper.selectSysDocumentList(documentQuery, 0, LIST_ALL_LIMIT);
            compressUtil.addJsonFile("document.json", documentList);
            for (SysDocument doc : documentList) {
                if (StringUtils.isNotBlank(doc.getFileUrl())) {
                    compressFile(compressUtil, doc.getFileUrl());
                }
            }
            sink.log("INFO", "文档 " + (documentList == null ? 0 : documentList.size()) + " 条");

            sink.phase(80, "打包 Git 仓库", "开始打包 Git bare（可能较慢）");
            packGitRepositories(compressUtil, projectId, sink);
        } catch (Exception e) {
            LOG.error("打包项目同步文件失败", e);
            sink.log("ERROR", "打包失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            throw e instanceof IOException ? (IOException) e : new IOException(e);
        } finally {
            compressUtil.build();
        }

        File compressFile = new File(compressFileName);
        try {
            long size = compressFile.length();
            sink.phase(90, "上传到云站", "上传同步包 " + formatSize(size) + " → " + cloudUrl);
            String url = cloudUrl + "/api/project/push";
            boolean ok = postZip(url, pullKey.trim(), systemVersion, fileName, compressFile);
            if (ok) {
                sink.phase(99, "上传完成", "云站已接收同步包");
            } else {
                sink.log("ERROR", "云站返回非成功状态");
            }
            return ok;
        } finally {
            if (!compressFile.delete()) {
                LOG.warnf("临时同步文件删除失败: %s", compressFileName);
            }
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void packTeamAndProjectConfig(CompressUtil compressUtil, SysProject project) throws IOException {
        if (project == null || project.getTeamId() == null) {
            return;
        }
        Long teamId = project.getTeamId();
        Map<String, Object> teamConfig = new LinkedHashMap<>();
        teamConfig.put("sourceTeamId", teamId);
        List<SysCaseType> caseTypes = sysCaseTypeMapper.selectByTeamId(teamId);
        teamConfig.put("caseTypes", caseTypes == null ? List.of() : caseTypes);
        teamConfig.put("caseReviewTemplate", sysCaseTypeMapper.selectReviewTemplate(teamId));
        teamConfig.put("defectBuiltinLayout",
                sysProjectDefectFieldMapper.selectDefectBuiltinFieldConfig(teamId));
        List<SysProjectDefectField> fields = sysProjectDefectFieldMapper.selectListByTeamId(teamId);
        teamConfig.put("defectFields", fields == null ? List.of() : fields);
        compressUtil.addJsonFile("team_config.json", teamConfig);

        Map<String, Object> projectConfig = new LinkedHashMap<>();
        SysProjectWorkCalendar calendar =
                sysProjectWorkCalendarMapper.selectByProjectId(project.getProjectId());
        projectConfig.put("workCalendar", calendar);
        SysProjectMcp mcp = sysProjectMcpService.get(project.getProjectId());
        Map<String, Object> mcpSlice = new LinkedHashMap<>();
        mcpSlice.put("enabled", mcp != null && mcp.isEnabled());
        mcpSlice.put("modules", mcp == null || mcp.getModules() == null ? List.of() : mcp.getModules());
        projectConfig.put("mcp", mcpSlice);
        compressUtil.addJsonFile("project_config.json", projectConfig);
    }

    private void packGitRepositories(CompressUtil compressUtil, Long projectId, PushProgress sink)
            throws IOException {
        SysGitRepository query = new SysGitRepository();
        query.setProjectId(projectId);
        List<SysGitRepository> repos = sysGitRepositoryMapper.selectSysGitRepositoryList(query);
        if (repos == null) {
            repos = List.of();
        }
        List<Map<String, Object>> meta = new ArrayList<>();
        int index = 0;
        for (SysGitRepository repo : repos) {
            if (StringUtils.isBlank(repo.getSlug())) {
                continue;
            }
            index++;
            String slug = repo.getSlug().trim();
            String barePrefix = "git/bare/" + slug + "/";
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", repo.getName());
            row.put("slug", slug);
            row.put("defaultBranch", repo.getDefaultBranch());
            row.put("visibility", repo.getVisibility());
            row.put("remark", repo.getRemark());
            row.put("bareEntryPrefix", barePrefix);
            List<Map<String, Object>> members = new ArrayList<>();
            List<SysGitRepositoryMember> acl =
                    sysGitRepositoryMemberMapper.selectMembersByRepositoryId(repo.getRepositoryId());
            if (acl != null) {
                for (SysGitRepositoryMember m : acl) {
                    if (m.getUserId() == null) {
                        continue;
                    }
                    Map<String, Object> member = new LinkedHashMap<>();
                    member.put("srcUserId", m.getUserId());
                    member.put("role", m.getRole());
                    members.add(member);
                }
            }
            row.put("members", members);
            meta.add(row);

            if (StringUtils.isNotBlank(repo.getDiskPath())) {
                Path bare = Path.of(repo.getDiskPath());
                if (Files.isDirectory(bare)) {
                    sink.log("INFO", "打包 Git[" + index + "] " + slug + " …");
                    compressUtil.addDirectoryTree(barePrefix, bare);
                    sink.log("INFO", "Git[" + index + "] " + slug + " 完成");
                } else {
                    LOG.warnf("Git bare 目录不存在，跳过内容: %s", repo.getDiskPath());
                    sink.log("WARN", "Git[" + index + "] " + slug + " bare 目录不存在，已跳过内容");
                }
            }
        }
        compressUtil.addJsonFile("git_repositories.json", meta);
        sink.log("INFO", "Git 仓库元数据 " + meta.size() + " 个");
    }

    private void compressCustomFieldPaths(CompressUtil compressUtil, Map<String, Object> customFields)
            throws IOException {
        if (customFields == null || customFields.isEmpty()) {
            return;
        }
        for (String path : collectPathStrings(customFields, new ArrayList<>())) {
            compressFile(compressUtil, path);
        }
    }

    private static List<String> collectPathStrings(Object node, List<String> out) {
        if (node == null) {
            return out;
        }
        if (node instanceof String s) {
            String value = s.trim();
            if (value.startsWith("/profile/") || value.startsWith("profile/")) {
                out.add(value.startsWith("/") ? value : "/" + value);
            }
            return out;
        }
        if (node instanceof Map<?, ?> map) {
            for (Object v : map.values()) {
                collectPathStrings(v, out);
            }
            return out;
        }
        if (node instanceof Collection<?> col) {
            for (Object v : col) {
                collectPathStrings(v, out);
            }
        }
        return out;
    }

    private static boolean postZip(String url, String pullKey, String version, String fileName, File file)
            throws IOException {
        String boundary = "----Cat2Bug" + UUID.randomUUID().toString().replace("-", "");
        String preamble = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"version\"\r\n\r\n"
                + version + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/zip\r\n\r\n";
        byte[] epilogue = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        // 大包勿整包进内存；写成临时 multipart 再 ofFile 流式上传
        Path bodyFile = Files.createTempFile("cat2bug-push-body-", ".multipart");
        try {
            try (var out = Files.newOutputStream(bodyFile)) {
                out.write(preamble.getBytes(StandardCharsets.UTF_8));
                Files.copy(file.toPath(), out);
                out.write(epilogue);
            }
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header(AUTH_TOKEN_HEADER_NAME, pullKey)
                    .timeout(Duration.ofMinutes(60))
                    .POST(HttpRequest.BodyPublishers.ofFile(bodyFile))
                    .build();
            try {
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                int code = response.statusCode();
                if (code < 200 || code >= 300) {
                    throw new IOException("云站推送失败 HTTP " + code + "，目标 " + url);
                }
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("上传被中断: " + url, e);
            } catch (IOException e) {
                String detail = e.getMessage();
                if (detail == null || detail.isBlank()) {
                    detail = e.getClass().getSimpleName();
                }
                if (detail.contains("云站推送失败")) {
                    throw e;
                }
                throw new IOException("上传同步包失败（" + formatSize(file.length()) + "）→ " + url + ": " + detail, e);
            }
        } finally {
            try {
                Files.deleteIfExists(bodyFile);
            } catch (IOException ignored) {
                // 临时文件清理失败不影响上传结果
            }
        }
    }

    private void compressUrls(CompressUtil compressUtil, String urls) throws IOException {
        if (StringUtils.isBlank(urls)) {
            return;
        }
        for (String url : urls.split(",")) {
            if (StringUtils.isNotBlank(url)) {
                compressFile(compressUtil, url.trim());
            }
        }
    }

    private void compressFile(CompressUtil compressUtil, String fileName) throws IOException {
        String path = fileName.startsWith("/") ? fileName : "/" + fileName;
        // zip 条目用相对路径，避免入站解压时绝对路径条目冲掉临时目录
        String entryName = path.startsWith("/") ? path.substring(1) : path;
        // 优先读本地 profile，避免 HTTP 回源/SPA 假 200 导致附件静默丢失
        String relative = path.startsWith("/profile/") ? path.substring("/profile/".length()) : path.replaceFirst("^/", "");
        try {
            Path local = profileConfig.resolveUploadPath(relative);
            if (Files.isRegularFile(local)) {
                compressUtil.addBytes(entryName, Files.readAllBytes(local));
                return;
            }
        } catch (RuntimeException ignored) {
            // 非法路径等：回退 HTTP
        }
        compressUtil.addUrlFile(entryName, "http://127.0.0.1:" + httpPort + path);
    }
}
