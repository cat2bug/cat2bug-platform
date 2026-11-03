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
import com.cat2bug.common.domain.SysPlanItem;
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
import com.cat2bug.system.service.SysDocumentService;
import com.cat2bug.system.service.SysModuleService;
import com.cat2bug.system.service.SysUserProjectService;
import com.cat2bug.system.service.mcp.SysProjectMcpService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ProjectCloudPushService {

    private static final Logger LOG = Logger.getLogger(ProjectCloudPushService.class);
    private static final String AUTH_TOKEN_HEADER_NAME = "CAT2BUG-API-KEY";
    private static final String DEFECT_LOG_COMMENT_TYPE = "defect_log";
    private static final int LIST_ALL_LIMIT = Integer.MAX_VALUE;
    private static final String MISSING_FILE_HINT = "本地文件缺失，无法勾选";
    private static final String EMPTY_FOLDER_HINT = "文件夹内没有可同步的本地文件";

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
    SysDocumentService sysDocumentService;

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

    @ConfigProperty(name = "cat2bug.cloud.push.max-total-bytes", defaultValue = "524288000")
    long maxTotalBytes;

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

    public long maxTotalBytes() {
        return maxTotalBytes > 0 ? maxTotalBytes : 524288000L;
    }

    public boolean pushToCloud(Long projectId, String pullKey) throws IOException {
        return pushToCloud(projectId, pullKey, NOOP_PROGRESS);
    }

    /** 旧单包：一个 zip 含全部文档与 Git，上传到不带 session 头的 /api/project/push。 */
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
            SysProject project = requireProject(projectId);
            sink.phase(2, "写入元数据", "写入 system / project 元数据");
            writeCoreEntries(compressUtil, project, pullKey.trim(), sink, false, null, null);
            sink.phase(72, "打包文档", null);
            writeAllDocuments(compressUtil, projectId, sink);
            sink.phase(80, "打包 Git 仓库", "开始打包 Git bare（可能较慢）");
            writeAllGit(compressUtil, projectId, sink);
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
            sink.phase(90, "上传到云站", "上传同步包 " + CloudPushSupport.formatSize(size) + " → " + cloudUrl);
            String url = trimSlash(cloudUrl) + "/api/project/push";
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

    /** 预估必选四件套、Git 目录占用、文档树可读性。体积为约数。 */
    public Map<String, Object> estimate(Long projectId) {
        if (projectId == null) {
            throw new ApiException("项目ID不能为空");
        }
        SysProject project = requireProject(projectId);
        Set<String> seen = new HashSet<>();

        List<SysDefect> defects = orEmpty(loadDefects(projectId));
        List<SysDefectLog> defectLogs = loadDefectLogs(defects);
        List<SysComment> defectComments = loadDefectComments(defectLogs);
        long defectJson = jsonSize(defects) + jsonSize(defectLogs) + jsonSize(defectComments);
        long defectFiles = 0L;
        for (SysDefect defect : defects) {
            defectFiles += takeFileBytes(defect.getImgUrls(), seen);
            defectFiles += takeFileBytes(defect.getAnnexUrls(), seen);
            defectFiles += takeCustomFieldBytes(defect.getCustomFields(), seen);
        }
        for (SysDefectLog log : defectLogs) {
            defectFiles += takeFileBytes(log.getAnnexUrls(), seen);
        }
        long defectBytes = CloudPushSupport.estimateZipBytes(defectJson, defectFiles);

        List<SysCase> cases = orEmpty(loadCases(projectId));
        long caseJson = jsonSize(cases);
        long caseFiles = 0L;
        for (SysCase row : cases) {
            caseFiles += takeFileBytes(row.getImgUrls(), seen);
            caseFiles += takeFileBytes(row.getAnnexUrls(), seen);
        }
        long caseBytes = CloudPushSupport.estimateZipBytes(caseJson, caseFiles);

        List<SysPlan> plans = orEmpty(loadPlans(projectId));
        long planJson = jsonSize(plans);
        long planFiles = 0L;
        for (SysPlan plan : plans) {
            if (plan.getSysPlanItemList() == null) {
                continue;
            }
            for (SysPlanItem item : plan.getSysPlanItemList()) {
                planFiles += takeFileBytes(item.getImgUrls(), seen);
                planFiles += takeFileBytes(item.getAnnexUrls(), seen);
            }
        }
        long planBytes = CloudPushSupport.estimateZipBytes(planJson, planFiles);

        List<SysModule> modules = orEmpty(loadModules(projectId));
        long moduleJson = jsonSize(modules);
        long moduleFiles = 0L;
        for (SysModule module : modules) {
            moduleFiles += takeFileBytes(module.getAnnexUrls(), seen);
        }
        long moduleBytes = CloudPushSupport.estimateZipBytes(moduleJson, moduleFiles);

        List<SysUser> members = orEmpty(sysUserProjectService.selectSysUserListByProjectId(projectId));
        long silentJson = jsonSize(members) + jsonSize(project);
        long silentFiles = 0L;
        for (SysUser member : members) {
            silentFiles += takeFileBytes(member.getAvatar(), seen);
        }
        Map<String, Object> teamConfig = buildTeamConfig(project);
        Map<String, Object> projectConfig = buildProjectConfig(project);
        if (teamConfig != null) {
            silentJson += jsonSize(teamConfig);
        }
        silentJson += jsonSize(projectConfig);
        silentJson += 512;
        long silent = CloudPushSupport.estimateZipBytes(silentJson, silentFiles);

        long requiredBytes = defectBytes + caseBytes + planBytes + moduleBytes + silent;
        List<Map<String, Object>> required = new ArrayList<>();
        required.add(requiredRow("defects", "缺陷", defectBytes, defectJson + defectFiles));
        required.add(requiredRow("cases", "用例", caseBytes, caseJson + caseFiles));
        required.add(requiredRow("plans", "计划", planBytes, planJson + planFiles));
        required.add(requiredRow("deliverables", "交付物", moduleBytes, moduleJson + moduleFiles));

        List<Map<String, Object>> git = new ArrayList<>();
        for (SysGitRepository repo : orEmpty(loadRepos(projectId))) {
            if (StringUtils.isBlank(repo.getSlug())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("repositoryId", repo.getRepositoryId());
            row.put("slug", repo.getSlug().trim());
            row.put("name", repo.getName());
            boolean bare = StringUtils.isNotBlank(repo.getDiskPath()) && Files.isDirectory(Path.of(repo.getDiskPath()));
            long bytes = bare ? directorySize(Path.of(repo.getDiskPath())) : 0L;
            row.put("bytes", bytes);
            row.put("selectable", true);
            row.put("barePresent", bare);
            row.put("hint", bare ? null : "本地 bare 目录不存在，勾选后仅包含仓库元数据");
            git.add(row);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("maxTotalBytes", maxTotalBytes());
        body.put("approximate", true);
        body.put("estimateMode", "zip");
        body.put("required", required);
        body.put("silentBytes", silent);
        body.put("requiredBytes", requiredBytes);
        body.put("git", git);
        body.put("repositories", git);
        body.put("documents", documentTree(projectId));
        return body;
    }

    /** 把请求勾选展开成 part 计划。超限只打标，由调用方决定拒绝启动。 */
    public CloudPushPlan prepare(Long projectId, List<String> gitSlugs, List<Long> documentIds) {
        Map<String, Object> estimate = estimate(projectId);
        boolean legacy = CloudPushSupport.isLegacySelection(gitSlugs, documentIds);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> gitRows = (List<Map<String, Object>>) estimate.get("git");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docTree = (List<Map<String, Object>>) estimate.get("documents");
        List<Map<String, Object>> docRows = new ArrayList<>();
        flattenDocuments(docTree, docRows);
        Map<String, Map<String, Object>> gitBySlug = new LinkedHashMap<>();
        for (Map<String, Object> row : gitRows) {
            gitBySlug.put(String.valueOf(row.get("slug")), row);
        }
        Map<Long, Map<String, Object>> docById = new LinkedHashMap<>();
        Map<Long, List<Map<String, Object>>> children = new HashMap<>();
        for (Map<String, Object> row : docRows) {
            long id = longVal(row.get("docId"));
            docById.put(id, row);
            long pid = longVal(row.get("docPid"));
            children.computeIfAbsent(pid, key -> new ArrayList<>()).add(row);
        }

        List<String> selectedGit = new ArrayList<>();
        List<Long> selectedDocs = new ArrayList<>();
        if (legacy) {
            selectedGit.addAll(gitBySlug.keySet());
            for (Map<String, Object> row : docRows) {
                if (!boolVal(row.get("folder")) && boolVal(row.get("selectable"))) {
                    selectedDocs.add(longVal(row.get("docId")));
                }
            }
        } else {
            if (gitSlugs != null) {
                LinkedHashSet<String> uniq = new LinkedHashSet<>();
                for (String slug : gitSlugs) {
                    if (StringUtils.isBlank(slug)) {
                        continue;
                    }
                    String trimmed = slug.trim();
                    if (!gitBySlug.containsKey(trimmed)) {
                        throw new ApiException("源码库不存在：" + trimmed);
                    }
                    uniq.add(trimmed);
                }
                selectedGit.addAll(uniq);
            }
            if (documentIds != null) {
                LinkedHashSet<Long> files = new LinkedHashSet<>();
                for (Long docId : documentIds) {
                    if (docId == null) {
                        continue;
                    }
                    Map<String, Object> node = docById.get(docId);
                    if (node == null) {
                        throw new ApiException("文档不存在或不属于当前项目");
                    }
                    if (boolVal(node.get("folder"))) {
                        List<Long> under = new ArrayList<>();
                        collectSelectableFiles(docId, children, under);
                        if (under.isEmpty()) {
                            throw new ApiException("文件夹「" + node.get("docName") + "」下没有可同步的本地文件");
                        }
                        files.addAll(under);
                    } else if (!boolVal(node.get("selectable"))) {
                        throw new ApiException("文档「" + node.get("docName") + "」本地文件缺失，无法同步");
                    } else {
                        files.add(docId);
                    }
                }
                selectedDocs.addAll(files);
            }
        }

        CloudPushPlan plan = new CloudPushPlan();
        plan.setLegacyFull(legacy);
        plan.setMaxTotalBytes(longVal(estimate.get("maxTotalBytes")));
        plan.setRequiredBytes(longVal(estimate.get("requiredBytes")));
        plan.setGitSlugs(selectedGit);
        plan.setDocumentIds(selectedDocs);

        List<CloudPushPlan.Part> parts = new ArrayList<>();
        CloudPushPlan.Part core = new CloudPushPlan.Part();
        core.setPartId(CloudPushSupport.PART_CORE);
        core.setType(CloudPushSupport.TYPE_CORE);
        core.setSortOrder(0);
        core.setEstimatedBytes(plan.getRequiredBytes());
        parts.add(core);
        int sort = 1;
        long optional = 0;
        for (Long docId : selectedDocs) {
            Map<String, Object> node = docById.get(docId);
            CloudPushPlan.Part part = new CloudPushPlan.Part();
            part.setPartId(CloudPushSupport.docPartId(docId));
            part.setType(CloudPushSupport.TYPE_DOC);
            part.setRefKey(Long.toString(docId));
            part.setSortOrder(sort++);
            long bytes = node == null ? 0 : longVal(node.get("bytes"));
            part.setEstimatedBytes(bytes);
            optional += bytes;
            parts.add(part);
        }
        for (String slug : selectedGit) {
            Map<String, Object> node = gitBySlug.get(slug);
            CloudPushPlan.Part part = new CloudPushPlan.Part();
            part.setPartId(CloudPushSupport.gitPartId(slug));
            part.setType(CloudPushSupport.TYPE_GIT);
            part.setRefKey(slug);
            part.setSortOrder(sort++);
            long bytes = node == null ? 0 : longVal(node.get("bytes"));
            part.setEstimatedBytes(bytes);
            optional += bytes;
            parts.add(part);
        }
        plan.setParts(parts);
        plan.setEstimatedBytes(plan.getRequiredBytes() + optional);
        plan.setOverLimit(CloudPushSupport.wouldExceed(0, plan.getEstimatedBytes(), plan.getMaxTotalBytes()));
        return plan;
    }

    /**
     * 打一个 part zip。core 在最后打包，manifestParts 写入 system.json（core 自身 sha256 可为空）。
     */
    public Path packPartFile(Long projectId, String pullKey, CloudPushPlan.Part part, Path dir,
                             CloudPushPlan plan, List<Map<String, Object>> manifestParts) throws IOException {
        Files.createDirectories(dir);
        Path target = dir.resolve(part.getPartId() + ".zip");
        Files.deleteIfExists(target);
        CompressUtil compressUtil = CompressUtil.createStream(target.toString());
        try {
            Map<String, Object> marker = new LinkedHashMap<>();
            marker.put("partId", part.getPartId());
            marker.put("type", part.getType());
            marker.put("packageSchema", SysProjectPush.PACKAGE_SCHEMA_1_2);
            if (StringUtils.isNotBlank(part.getRefKey())) {
                marker.put("refKey", part.getRefKey());
            }
            compressUtil.addJsonFile("part.json", marker);
            if (CloudPushSupport.TYPE_CORE.equals(part.getType())) {
                SysProject project = requireProject(projectId);
                writeCoreEntries(compressUtil, project, pullKey, NOOP_PROGRESS, true, plan, manifestParts);
            } else if (CloudPushSupport.TYPE_DOC.equals(part.getType())) {
                writeOneDocument(compressUtil, projectId, Long.valueOf(part.getRefKey()));
            } else if (CloudPushSupport.TYPE_GIT.equals(part.getType())) {
                writeOneGit(compressUtil, projectId, part.getRefKey(), NOOP_PROGRESS);
            } else {
                throw new IOException("未知分包类型：" + part.getType());
            }
        } catch (Exception e) {
            try {
                compressUtil.build();
            } catch (IOException ignored) {
                // 关闭失败时仍抛出原始错误
            }
            Files.deleteIfExists(target);
            throw e instanceof IOException ? (IOException) e : new IOException(e);
        }
        compressUtil.build();
        return target;
    }

    public String overLimitMessage(long estimated, long max) {
        return "所选内容约 " + CloudPushSupport.formatSize(estimated) + "，"
                + CloudPushSupport.SIZE_LIMIT_MARK + " " + CloudPushSupport.formatSize(max)
                + "，请取消部分源码库或文档后再试";
    }

    private void writeCoreEntries(CompressUtil compressUtil, SysProject project, String pullKey,
                                  PushProgress sink, boolean multipart, CloudPushPlan plan,
                                  List<Map<String, Object>> manifestParts) throws IOException {
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("version", systemVersion);
        system.put("pushKey", pullKey);
        system.put("packageSchema", multipart ? SysProjectPush.PACKAGE_SCHEMA_1_2 : SysProjectPush.PACKAGE_SCHEMA_1_1);
        if (multipart && plan != null) {
            system.put("include", includeMap(plan));
            system.put("parts", manifestParts == null ? List.of() : manifestParts);
        }
        compressUtil.addJsonFile("system.json", system);
        compressUtil.addJsonFile("project.json", project);

        sink.phase(8, "打包成员", null);
        Long projectId = project.getProjectId();
        List<SysUser> memberList = orEmpty(sysUserProjectService.selectSysUserListByProjectId(projectId));
        compressUtil.addJsonFile("member.json", memberList);
        for (SysUser member : memberList) {
            if (StringUtils.isNotBlank(member.getAvatar())) {
                compressFile(compressUtil, member.getAvatar());
            }
        }

        sink.phase(15, "打包团队与项目配置", null);
        Map<String, Object> teamConfig = buildTeamConfig(project);
        if (teamConfig != null) {
            compressUtil.addJsonFile("team_config.json", teamConfig);
        }
        compressUtil.addJsonFile("project_config.json", buildProjectConfig(project));

        sink.phase(25, "打包用例", null);
        List<SysCase> caseList = orEmpty(loadCases(projectId));
        compressUtil.addJsonFile("case.json", caseList);
        for (SysCase row : caseList) {
            compressUrls(compressUtil, row.getImgUrls());
            compressUrls(compressUtil, row.getAnnexUrls());
        }
        sink.log("INFO", "用例 " + caseList.size() + " 条");

        sink.phase(40, "打包缺陷", null);
        List<SysDefect> defectList = orEmpty(loadDefects(projectId));
        compressUtil.addJsonFile("defect.json", defectList);
        for (SysDefect defect : defectList) {
            compressUrls(compressUtil, defect.getImgUrls());
            compressUrls(compressUtil, defect.getAnnexUrls());
            compressCustomFieldPaths(compressUtil, defect.getCustomFields());
        }
        sink.log("INFO", "缺陷 " + defectList.size() + " 条");

        sink.phase(55, "打包缺陷日志与评论", null);
        List<SysDefectLog> defectLogList = loadDefectLogs(defectList);
        compressUtil.addJsonFile("defect_log.json", defectLogList);
        for (SysDefectLog log : defectLogList) {
            compressUrls(compressUtil, log.getAnnexUrls());
        }
        List<SysComment> defectCommentList = loadDefectComments(defectLogList);
        compressUtil.addJsonFile("defect_log_comment.json", defectCommentList);
        sink.log("INFO", "缺陷日志 " + defectLogList.size() + " 条，评论 " + defectCommentList.size() + " 条");

        sink.phase(65, "打包计划与交付物", null);
        List<SysPlan> planList = orEmpty(loadPlans(projectId));
        compressUtil.addJsonFile("plan.json", planList);
        for (SysPlan planRow : planList) {
            if (planRow.getSysPlanItemList() == null) {
                continue;
            }
            for (SysPlanItem item : planRow.getSysPlanItemList()) {
                compressUrls(compressUtil, item.getImgUrls());
                compressUrls(compressUtil, item.getAnnexUrls());
            }
        }
        List<SysModule> modules = orEmpty(loadModules(projectId));
        compressUtil.addJsonFile("module.json", modules);
        for (SysModule module : modules) {
            compressUrls(compressUtil, module.getAnnexUrls());
        }
    }

    private void writeAllDocuments(CompressUtil compressUtil, Long projectId, PushProgress sink) throws IOException {
        List<SysDocument> documentList = orEmpty(loadDocuments(projectId));
        for (SysDocument doc : documentList) {
            if (isFolder(doc)) {
                continue;
            }
            try {
                sysDocumentService.ensureEmptyMarkdownFile(doc, true);
            } catch (RuntimeException e) {
                LOG.warnf(e, "heal empty markdown before pack-all failed, docId=%s", doc.getDocId());
            }
        }
        compressUtil.addJsonFile("document.json", documentList);
        for (SysDocument doc : documentList) {
            if (StringUtils.isNotBlank(doc.getFileUrl())) {
                compressFile(compressUtil, doc.getFileUrl());
            }
        }
        sink.log("INFO", "文档 " + documentList.size() + " 条");
    }

    private void writeOneDocument(CompressUtil compressUtil, Long projectId, Long docId) throws IOException {
        List<SysDocument> all = orEmpty(loadDocuments(projectId));
        Map<Long, SysDocument> byId = new HashMap<>();
        for (SysDocument doc : all) {
            byId.put(doc.getDocId(), doc);
        }
        SysDocument doc = byId.get(docId);
        if (doc == null || isFolder(doc)) {
            throw new IOException("文档不存在或不是文件：" + docId);
        }
        try {
            sysDocumentService.ensureEmptyMarkdownFile(doc, true);
        } catch (RuntimeException e) {
            throw new IOException("文档「" + doc.getDocName() + "」无法创建本地文件：" + e.getMessage(), e);
        }
        if (resolveReadable(doc.getFileUrl()) == null) {
            throw new IOException("文档「" + doc.getDocName() + "」本地文件缺失，无法打包");
        }
        compressUtil.addJsonFile("document.json", ancestorChain(doc, byId));
        compressFile(compressUtil, doc.getFileUrl());
    }

    private void writeAllGit(CompressUtil compressUtil, Long projectId, PushProgress sink) throws IOException {
        List<SysGitRepository> repos = orEmpty(loadRepos(projectId));
        List<Map<String, Object>> meta = new ArrayList<>();
        int index = 0;
        for (SysGitRepository repo : repos) {
            if (StringUtils.isBlank(repo.getSlug())) {
                continue;
            }
            index++;
            meta.add(gitMeta(repo));
            addGitBare(compressUtil, repo, index, sink);
        }
        compressUtil.addJsonFile("git_repositories.json", meta);
        sink.log("INFO", "Git 仓库元数据 " + meta.size() + " 个");
    }

    private void writeOneGit(CompressUtil compressUtil, Long projectId, String slug, PushProgress sink)
            throws IOException {
        SysGitRepository found = null;
        for (SysGitRepository repo : orEmpty(loadRepos(projectId))) {
            if (repo.getSlug() != null && repo.getSlug().trim().equals(slug)) {
                found = repo;
                break;
            }
        }
        if (found == null) {
            throw new IOException("源码库不存在：" + slug);
        }
        compressUtil.addJsonFile("git_repositories.json", List.of(gitMeta(found)));
        addGitBare(compressUtil, found, 1, sink);
    }

    private Map<String, Object> gitMeta(SysGitRepository repo) {
        String slug = repo.getSlug().trim();
        String barePrefix = "git/bare/" + slug + "/";
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", repo.getName());
        row.put("slug", slug);
        row.put("defaultBranch", repo.getDefaultBranch());
        row.put("visibility", repo.getVisibility());
        row.put("kind", "bare");
        row.put("remark", repo.getRemark());
        row.put("bareEntryPrefix", barePrefix);
        List<Map<String, Object>> members = new ArrayList<>();
        List<SysGitRepositoryMember> acl =
                sysGitRepositoryMemberMapper.selectMembersByRepositoryId(repo.getRepositoryId());
        if (acl != null) {
            for (SysGitRepositoryMember member : acl) {
                if (member.getUserId() == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("srcUserId", member.getUserId());
                item.put("role", member.getRole());
                members.add(item);
            }
        }
        row.put("members", members);
        return row;
    }

    private void addGitBare(CompressUtil compressUtil, SysGitRepository repo, int index, PushProgress sink)
            throws IOException {
        String slug = repo.getSlug().trim();
        if (StringUtils.isBlank(repo.getDiskPath())) {
            sink.log("WARN", "Git[" + index + "] " + slug + " 未配置 bare 目录，仅写入元数据");
            return;
        }
        Path bare = Path.of(repo.getDiskPath());
        if (!Files.isDirectory(bare)) {
            LOG.warnf("Git bare 目录不存在，跳过内容: %s", repo.getDiskPath());
            sink.log("WARN", "Git[" + index + "] " + slug + " bare 目录不存在，已跳过内容");
            return;
        }
        sink.log("INFO", "打包 Git[" + index + "] " + slug + " …");
        compressUtil.addDirectoryTree("git/bare/" + slug + "/", bare);
        sink.log("INFO", "Git[" + index + "] " + slug + " 完成");
    }

    private Map<String, Object> includeMap(CloudPushPlan plan) {
        Map<String, Object> include = new LinkedHashMap<>();
        include.put("required", List.of("defects", "cases", "plans", "deliverables"));
        include.put("silent", List.of("members", "teamConfig", "workCalendar", "mcp"));
        include.put("gitSlugs", plan.getGitSlugs());
        include.put("documentIds", plan.getDocumentIds());
        include.put("legacyFull", plan.isLegacyFull());
        return include;
    }

    private Map<String, Object> buildTeamConfig(SysProject project) {
        if (project == null || project.getTeamId() == null) {
            return null;
        }
        Long teamId = project.getTeamId();
        Map<String, Object> teamConfig = new LinkedHashMap<>();
        teamConfig.put("sourceTeamId", teamId);
        List<SysCaseType> caseTypes = sysCaseTypeMapper.selectByTeamId(teamId);
        teamConfig.put("caseTypes", caseTypes == null ? List.of() : caseTypes);
        teamConfig.put("caseReviewTemplate", sysCaseTypeMapper.selectReviewTemplate(teamId));
        teamConfig.put("defectBuiltinLayout", sysProjectDefectFieldMapper.selectDefectBuiltinFieldConfig(teamId));
        List<SysProjectDefectField> fields = sysProjectDefectFieldMapper.selectListByTeamId(teamId);
        teamConfig.put("defectFields", fields == null ? List.of() : fields);
        return teamConfig;
    }

    private Map<String, Object> buildProjectConfig(SysProject project) {
        Map<String, Object> projectConfig = new LinkedHashMap<>();
        SysProjectWorkCalendar calendar = project == null
                ? null
                : sysProjectWorkCalendarMapper.selectByProjectId(project.getProjectId());
        projectConfig.put("workCalendar", calendar);
        SysProjectMcp mcp = project == null ? null : sysProjectMcpService.get(project.getProjectId());
        Map<String, Object> mcpSlice = new LinkedHashMap<>();
        mcpSlice.put("enabled", mcp != null && mcp.isEnabled());
        mcpSlice.put("modules", mcp == null || mcp.getModules() == null ? List.of() : mcp.getModules());
        projectConfig.put("mcp", mcpSlice);
        return projectConfig;
    }

    private List<Map<String, Object>> documentTree(Long projectId) {
        List<SysDocument> docs = orEmpty(loadDocuments(projectId));
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<Long, Map<String, Object>> byId = new HashMap<>();
        Map<Long, List<Map<String, Object>>> children = new HashMap<>();
        for (SysDocument doc : docs) {
            boolean folder = isFolder(doc);
            // 在线新建 md 曾允许无 fileUrl；估价前补空文件，避免误报「本地缺失」
            if (!folder) {
                try {
                    sysDocumentService.ensureEmptyMarkdownFile(doc, true);
                } catch (RuntimeException e) {
                    LOG.warnf(e, "heal empty markdown file failed, docId=%s", doc.getDocId());
                }
            }
            Map<String, Object> node = new LinkedHashMap<>();
            long id = doc.getDocId() == null ? 0L : doc.getDocId();
            long pid = doc.getDocPid() == null ? 0L : doc.getDocPid();
            node.put("docId", doc.getDocId());
            node.put("docPid", pid);
            node.put("docName", doc.getDocName());
            node.put("docType", doc.getDocType());
            node.put("folder", folder);
            Path local = folder ? null : resolveReadable(doc.getFileUrl());
            boolean readable = local != null;
            long bytes = 0;
            if (readable) {
                try {
                    bytes = Files.size(local);
                } catch (IOException ignored) {
                    readable = false;
                }
            }
            node.put("bytes", folder ? 0L : bytes);
            node.put("subtreeBytes", folder ? 0L : bytes);
            node.put("localReadable", folder || readable);
            node.put("localMissing", !folder && !readable);
            node.put("selectable", !folder && readable);
            node.put("missingHint", (!folder && !readable) ? MISSING_FILE_HINT : null);
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.add(node);
            byId.put(id, node);
            children.computeIfAbsent(pid, key -> new ArrayList<>()).add(node);
        }
        Set<Long> visiting = new HashSet<>();
        for (Map<String, Object> node : nodes) {
            foldDocument(longVal(node.get("docId")), byId, children, visiting);
        }
        for (Map<String, Object> node : nodes) {
            node.remove("_folded");
            node.remove("_hasFile");
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            long id = longVal(node.get("docId"));
            long pid = longVal(node.get("docPid"));
            Map<String, Object> parent = pid == id ? null : byId.get(pid);
            if (parent == null) {
                roots.add(node);
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> childList = (List<Map<String, Object>>) parent.get("children");
            childList.add(node);
        }
        return roots;
    }

    private long foldDocument(long id, Map<Long, Map<String, Object>> byId,
                              Map<Long, List<Map<String, Object>>> children, Set<Long> visiting) {
        Map<String, Object> node = byId.get(id);
        if (node == null) {
            return 0;
        }
        if (node.containsKey("_folded")) {
            return longVal(node.get("subtreeBytes"));
        }
        if (!visiting.add(id)) {
            return 0;
        }
        if (!boolVal(node.get("folder"))) {
            node.put("_folded", true);
            visiting.remove(id);
            return longVal(node.get("bytes"));
        }
        long sum = 0;
        boolean selectable = false;
        boolean hasFile = false;
        for (Map<String, Object> child : children.getOrDefault(id, List.of())) {
            long childId = longVal(child.get("docId"));
            sum += foldDocument(childId, byId, children, visiting);
            selectable = selectable || boolVal(child.get("selectable"));
            hasFile = hasFile || !boolVal(child.get("folder")) || boolVal(child.get("_hasFile"));
        }
        node.put("subtreeBytes", sum);
        node.put("selectable", selectable);
        node.put("_hasFile", hasFile);
        if (!selectable && hasFile) {
            node.put("missingHint", EMPTY_FOLDER_HINT);
        }
        node.put("_folded", true);
        visiting.remove(id);
        return sum;
    }

    private static void flattenDocuments(List<Map<String, Object>> nodes, List<Map<String, Object>> out) {
        if (nodes == null) {
            return;
        }
        for (Map<String, Object> node : nodes) {
            out.add(node);
            Object nested = node.get("children");
            if (nested instanceof List<?> list) {
                List<Map<String, Object>> children = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> child = (Map<String, Object>) map;
                        children.add(child);
                    }
                }
                flattenDocuments(children, out);
            }
        }
    }

    private void collectSelectableFiles(long folderId, Map<Long, List<Map<String, Object>>> children, List<Long> out) {
        for (Map<String, Object> child : children.getOrDefault(folderId, List.of())) {
            if (boolVal(child.get("folder"))) {
                collectSelectableFiles(longVal(child.get("docId")), children, out);
            } else if (boolVal(child.get("selectable"))) {
                out.add(longVal(child.get("docId")));
            }
        }
    }

    private List<SysDocument> ancestorChain(SysDocument doc, Map<Long, SysDocument> byId) {
        List<SysDocument> parents = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        Long pid = doc.getDocPid();
        while (pid != null && pid != 0L && seen.add(pid)) {
            SysDocument parent = byId.get(pid);
            if (parent == null) {
                break;
            }
            parents.add(parent);
            pid = parent.getDocPid();
        }
        List<SysDocument> chain = new ArrayList<>();
        for (int i = parents.size() - 1; i >= 0; i--) {
            chain.add(parents.get(i));
        }
        chain.add(doc);
        return chain;
    }

    private static boolean isFolder(SysDocument doc) {
        return doc.getDocType() != null && doc.getDocType() == 0L;
    }

    private SysProject requireProject(Long projectId) {
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
        if (project == null) {
            throw new ApiException("项目不存在");
        }
        return project;
    }

    private List<SysCase> loadCases(Long projectId) {
        SysCase query = new SysCase();
        query.setProjectId(projectId);
        return sysCaseService.selectSysCaseListAll(query);
    }

    private List<SysDefect> loadDefects(Long projectId) {
        SysDefect query = new SysDefect();
        query.setProjectId(projectId);
        return sysDefectMapper.selectSysDefectList(query, 0, LIST_ALL_LIMIT);
    }

    private List<SysDefectLog> loadDefectLogs(List<SysDefect> defects) {
        List<SysDefectLog> logs = new ArrayList<>();
        for (SysDefect defect : defects) {
            SysDefectLog query = new SysDefectLog();
            query.setDefectId(defect.getDefectId());
            logs.addAll(orEmpty(sysDefectLogService.selectSysDefectLogList(query)));
        }
        return logs.stream().filter(row -> row != null).toList();
    }

    private List<SysComment> loadDefectComments(List<SysDefectLog> logs) {
        List<SysComment> comments = new ArrayList<>();
        Set<Long> seenDefects = new HashSet<>();
        for (SysDefectLog log : logs) {
            if (log.getDefectId() == null || !seenDefects.add(log.getDefectId())) {
                continue;
            }
            SysComment comment = new SysComment();
            comment.setCorrelationId(log.getDefectId());
            comment.setModuleType(DEFECT_LOG_COMMENT_TYPE);
            comments.addAll(orEmpty(sysCommentMapper.selectSysCommentList(comment, 0, LIST_ALL_LIMIT)));
        }
        return comments.stream().filter(row -> row != null).toList();
    }

    private List<SysPlan> loadPlans(Long projectId) {
        SysPlan query = new SysPlan();
        query.setProjectId(projectId);
        List<SysPlan> plans = sysPlanMapper.selectSysPlanList(query, 0, LIST_ALL_LIMIT);
        if (plans == null) {
            return List.of();
        }
        for (SysPlan plan : plans) {
            plan.setSysPlanItemList(sysPlanMapper.selectSysPlanItemListByPlanId(plan.getPlanId()));
        }
        return plans;
    }

    private List<SysModule> loadModules(Long projectId) {
        SysModule query = new SysModule();
        query.setProjectId(projectId);
        return sysModuleService.selectSysModuleList(query);
    }

    private List<SysDocument> loadDocuments(Long projectId) {
        SysDocument query = new SysDocument();
        query.setProjectId(projectId);
        return sysDocumentMapper.selectSysDocumentList(query, 0, LIST_ALL_LIMIT);
    }

    private List<SysGitRepository> loadRepos(Long projectId) {
        SysGitRepository query = new SysGitRepository();
        query.setProjectId(projectId);
        return sysGitRepositoryMapper.selectSysGitRepositoryList(query);
    }

    private long jsonSize(Object value) {
        try {
            return CloudPushSupport.JSON.writeValueAsBytes(value).length;
        } catch (Exception e) {
            return 0;
        }
    }

    private long takeFileBytes(String urls, Set<String> seen) {
        long total = 0;
        for (String url : splitUrls(urls)) {
            String key = normalizeKey(url);
            if (!seen.add(key)) {
                continue;
            }
            Path local = resolveReadable(url);
            if (local == null) {
                continue;
            }
            try {
                total += Files.size(local);
            } catch (IOException ignored) {
                // 读不到的文件不计入预估
            }
        }
        return total;
    }

    private long takeCustomFieldBytes(Map<String, Object> customFields, Set<String> seen) {
        if (customFields == null || customFields.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (String path : collectPathStrings(customFields, new ArrayList<>())) {
            total += takeFileBytes(path, seen);
        }
        return total;
    }

    private static List<String> splitUrls(String urls) {
        if (StringUtils.isBlank(urls)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String url : urls.split(",")) {
            if (StringUtils.isNotBlank(url)) {
                out.add(url.trim());
            }
        }
        return out;
    }

    private static String normalizeKey(String fileName) {
        String path = fileName.startsWith("/") ? fileName : "/" + fileName;
        return path;
    }

    private Path resolveReadable(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        String path = fileName.startsWith("/") ? fileName : "/" + fileName;
        String relative = path.startsWith("/profile/")
                ? path.substring("/profile/".length())
                : path.replaceFirst("^/", "");
        try {
            Path local = profileConfig.resolveUploadPath(relative);
            if (Files.isRegularFile(local) && Files.isReadable(local)) {
                return local;
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static Map<String, Object> requiredRow(String key, String label, long bytes, long rawBytes) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("label", label);
        row.put("bytes", bytes);
        row.put("rawBytes", rawBytes);
        row.put("required", true);
        return row;
    }

    private static long longVal(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static boolean boolVal(Object value) {
        return value instanceof Boolean flag && flag;
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static long directorySize(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            return 0;
        }
        long total = 0;
        try (var walk = Files.walk(root)) {
            var iterator = walk.filter(Files::isRegularFile).iterator();
            while (iterator.hasNext()) {
                try {
                    total += Files.size(iterator.next());
                } catch (IOException ignored) {
                    // 单个文件读失败不让整仓预估失败
                }
            }
        } catch (IOException e) {
            LOG.warnf("统计目录体积失败: %s", root);
        }
        return total;
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
                throw new IOException("上传同步包失败（" + CloudPushSupport.formatSize(file.length()) + "）→ " + url + ": " + detail, e);
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
        for (String url : splitUrls(urls)) {
            compressFile(compressUtil, url);
        }
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
        if (node instanceof String text) {
            String value = text.trim();
            if (value.startsWith("/profile/") || value.startsWith("profile/")) {
                out.add(value.startsWith("/") ? value : "/" + value);
            }
            return out;
        }
        if (node instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                collectPathStrings(value, out);
            }
            return out;
        }
        if (node instanceof Collection<?> col) {
            for (Object value : col) {
                collectPathStrings(value, out);
            }
        }
        return out;
    }

    private void compressFile(CompressUtil compressUtil, String fileName) throws IOException {
        String path = fileName.startsWith("/") ? fileName : "/" + fileName;
        String entryName = path.startsWith("/") ? path.substring(1) : path;
        Path local = resolveReadable(fileName);
        if (local != null) {
            try (InputStream in = Files.newInputStream(local)) {
                compressUtil.addInputStream(entryName, in, Files.size(local));
            }
            return;
        }
        compressUtil.addUrlFile(entryName, "http://127.0.0.1:" + httpPort + path);
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        String value = url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
