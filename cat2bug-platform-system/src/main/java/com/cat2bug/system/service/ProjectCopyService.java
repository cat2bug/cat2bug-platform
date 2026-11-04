package com.cat2bug.system.service;

import com.cat2bug.common.domain.AiAccount;
import com.cat2bug.common.domain.ProjectCopyJob;
import com.cat2bug.common.domain.ProjectCopySelection;
import com.cat2bug.common.domain.SysAiModuleConfig;
import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.domain.SysCaseType;
import com.cat2bug.common.domain.SysDocument;
import com.cat2bug.common.domain.SysGitRepository;
import com.cat2bug.common.domain.SysModule;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysProjectMcp;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysTeamPlan;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserProject;
import com.cat2bug.common.domain.SysUserProjectRole;
import com.cat2bug.common.domain.SysUserTeam;
import com.cat2bug.common.domain.SysUserTeamRole;
import com.cat2bug.common.domain.im.IMProjectConfig;
import com.cat2bug.common.domain.vo.BatchUserRoleVo;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.config.Cat2BugProfileConfig;
import com.cat2bug.system.mapper.AiAccountMapper;
import com.cat2bug.system.mapper.IMProjectConfigMapper;
import com.cat2bug.system.mapper.ProjectCopyJobMapper;
import com.cat2bug.system.mapper.SysAiModuleConfigMapper;
import com.cat2bug.system.mapper.SysCaseMapper;
import com.cat2bug.system.mapper.SysCaseTypeMapper;
import com.cat2bug.system.mapper.SysDocumentMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysModuleMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysProjectMcpMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectRoleMapper;
import com.cat2bug.system.mapper.SysUserTeamMapper;
import com.cat2bug.system.mapper.SysUserTeamRoleMapper;
import com.cat2bug.system.service.git.GitPathValidator;
import com.cat2bug.system.service.mcp.SysProjectMcpService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 新建项目后的按勾选复制：先校验预估不超过项目空间，再异步灌入。
 * 单块失败保留项目与已成功的块，并记入 items。
 */
@ApplicationScoped
public class ProjectCopyService {

    private static final Logger LOG = Logger.getLogger(ProjectCopyService.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern SECRET_KEY = Pattern.compile("(?i).*(secret|token|password|cipher).*");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 与种子角色一致：创建人 / 项目管理员 / 团队普通成员 */
    private static final long ROLE_PROJECT_ADMIN = 6L;
    private static final long ROLE_PROJECT_CREATOR = 11L;
    private static final long ROLE_TEAM_DEFAULT = 5L;

    private static final long MODULE_ROW_BYTES = 2048L;
    private static final long CASE_ROW_BYTES = 8192L;
    private static final long CONFIG_BLOCK_BYTES = 16384L;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "project-copy");
        thread.setDaemon(true);
        return thread;
    });

    @Inject
    ProjectCopyJobMapper projectCopyJobMapper;

    @Inject
    ProjectCopyJobStore store;

    @Inject
    SysProjectMapper sysProjectMapper;

    @Inject
    SysTeamPlanService sysTeamPlanService;

    @Inject
    SysModuleMapper sysModuleMapper;

    @Inject
    SysCaseMapper sysCaseMapper;

    @Inject
    SysCaseTypeMapper sysCaseTypeMapper;

    @Inject
    SysDocumentMapper sysDocumentMapper;

    @Inject
    SysGitRepositoryMapper sysGitRepositoryMapper;

    @Inject
    GitPathValidator gitPathValidator;

    @Inject
    SysUserProjectService sysUserProjectService;

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

    @Inject
    SysUserProjectRoleMapper sysUserProjectRoleMapper;

    @Inject
    SysUserTeamMapper sysUserTeamMapper;

    @Inject
    SysUserTeamRoleMapper sysUserTeamRoleMapper;

    @Inject
    AiAccountMapper aiAccountMapper;

    @Inject
    SysAiModuleConfigMapper sysAiModuleConfigMapper;

    @Inject
    SysProjectMcpMapper sysProjectMcpMapper;

    @Inject
    SysProjectMcpService sysProjectMcpService;

    @Inject
    IMProjectConfigMapper imProjectConfigMapper;

    @Inject
    ProjectMembershipAuth projectMembershipAuth;

    @Inject
    Cat2BugProfileConfig profileConfig;

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public List<SysProject> listCopySources(Long userId) {
        if (userId == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return sysProjectMapper.selectCopySourceProjects(userId);
    }

    /** 粗估勾选体积。超限只打标，由创建接口拒绝开跑。 */
    public Map<String, Object> estimate(Long userId, ProjectCopySelection selection) {
        requireSourceAdmin(userId, selection);
        selection.normalize();
        Map<String, Object> body = buildEstimate(selection);
        if (selection.getTargetTeamId() != null) {
            SysTeamPlan plan = sysTeamPlanService.resolvePlanForTeam(selection.getTargetTeamId());
            long max = plan.getMaxProjectBytes() != null
                    ? plan.getMaxProjectBytes() : SysTeamPlanService.DEFAULT_MAX_PROJECT_BYTES;
            long estimated = ((Number) body.get("estimatedBytes")).longValue();
            body.put("maxProjectBytes", max);
            body.put("overQuota", estimated > max);
            body.put("planName", plan.getPlanName());
        }
        return body;
    }

    /**
     * 空项目已插入之后调用：再次校验权限与空间，写任务并异步复制。
     */
    public String startCopy(Long targetProjectId, Long userId, ProjectCopySelection selection) {
        if (targetProjectId == null) {
            throw new ApiException("项目不能为空");
        }
        requireSourceAdmin(userId, selection);
        selection.normalize();
        if (!selection.hasWork()) {
            throw new ApiException("请至少勾选一项复制内容");
        }
        if (targetProjectId.equals(selection.getSourceProjectId())) {
            throw new ApiException("不能从项目自身复制");
        }
        SysProject target = sysProjectMapper.selectSysProjectByProjectId(targetProjectId, null);
        if (target == null || target.getTeamId() == null) {
            throw new ApiException(404, "项目不存在");
        }
        selection.setTargetTeamId(target.getTeamId());
        Map<String, Object> estimated = buildEstimate(selection);
        long bytes = ((Number) estimated.get("estimatedBytes")).longValue();
        selection.setEstimatedBytes(bytes);
        // 提交前卡项目空间上限；日常增长不在本期拦截
        sysTeamPlanService.assertCopyWithinProjectBytes(target.getTeamId(), bytes);

        String jobId = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        ProjectCopyJob job = new ProjectCopyJob();
        job.setJobId(jobId);
        job.setSourceProjectId(selection.getSourceProjectId());
        job.setTargetProjectId(targetProjectId);
        job.setState(ProjectCopyJob.PENDING);
        job.setPercent(0);
        job.setEstimatedBytes(bytes);
        job.setActualBytes(0L);
        job.setCreateBy(String.valueOf(userId));
        job.setCreateTime(now);
        job.setUpdateTime(now);
        job.setItemsJson("[]");
        try {
            job.setSelectionJson(JSON.writeValueAsString(selection));
        } catch (IOException e) {
            job.setSelectionJson("{}");
        }
        store.insert(job);
        executor.execute(() -> runJob(jobId, userId));
        return jobId;
    }

    public Map<String, Object> getJob(String jobId, Long userId) {
        ProjectCopyJob job = store.find(jobId);
        if (job == null) {
            throw new ApiException(404, "复制任务不存在");
        }
        boolean owner = job.getCreateBy() != null && job.getCreateBy().equals(String.valueOf(userId));
        boolean member = job.getTargetProjectId() != null
                && projectMembershipAuth.canAccessProject(userId, job.getTargetProjectId());
        if (!owner && !member) {
            throw new ApiException(403, "无权限查看该复制任务");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", job.getJobId());
        body.put("sourceProjectId", job.getSourceProjectId());
        body.put("targetProjectId", job.getTargetProjectId());
        body.put("state", job.getState());
        body.put("percent", job.getPercent());
        body.put("estimatedBytes", job.getEstimatedBytes());
        body.put("actualBytes", job.getActualBytes());
        body.put("errorMessage", job.getErrorMessage());
        body.put("createBy", job.getCreateBy());
        body.put("createTime", job.getCreateTime());
        body.put("updateTime", job.getUpdateTime());
        body.put("items", readItems(job.getItemsJson()));
        return body;
    }

    private void runJob(String jobId, Long userId) {
        ProjectCopyJob job = store.find(jobId);
        if (job == null) {
            return;
        }
        moduleIdCache.remove();
        ProjectCopySelection selection = readSelection(job.getSelectionJson());
        List<Map<String, Object>> items = new ArrayList<>();
        AtomicLong actual = new AtomicLong(0L);
        try {
            job.setState(ProjectCopyJob.RUNNING);
            job.setPercent(1);
            store.update(job);
            SysProject target = sysProjectMapper.selectSysProjectByProjectId(job.getTargetProjectId(), null);
            if (target == null) {
                throw new ApiException("目标项目不存在");
            }
            List<String> steps = plannedSteps(selection);
            int done = 0;
            for (String step : steps) {
                Map<String, Object> item = runStep(step, selection, target, userId, actual);
                items.add(item);
                done++;
                job.setItemsJson(writeJson(items));
                job.setActualBytes(actual.get());
                job.setPercent(Math.min(99, done * 100 / steps.size()));
                store.update(job);
            }
            job.setItemsJson(writeJson(items));
            job.setActualBytes(actual.get());
            job.setPercent(100);
            job.setState(aggregateState(items));
            job.setErrorMessage(summarize(items));
            store.update(job);
            store.updateUsedBytes(job.getTargetProjectId(), actual.get());
        } catch (RuntimeException e) {
            LOG.warn("项目复制失败 " + jobId, e);
            job.setState(items.isEmpty() ? ProjectCopyJob.FAILED : aggregateState(items));
            if (ProjectCopyJob.RUNNING.equals(job.getState()) || job.getState() == null) {
                job.setState(ProjectCopyJob.FAILED);
            }
            job.setErrorMessage(trim(e.getMessage(), 2000));
            job.setItemsJson(writeJson(items));
            job.setActualBytes(actual.get());
            try {
                store.update(job);
                if (actual.get() > 0) {
                    store.updateUsedBytes(job.getTargetProjectId(), actual.get());
                }
            } catch (RuntimeException updateError) {
                LOG.warn("回写复制任务失败 " + jobId, updateError);
            }
        } finally {
            moduleIdCache.remove();
        }
    }

    private Map<String, Object> runStep(String step, ProjectCopySelection selection, SysProject target,
                                        Long userId, AtomicLong actual) {
        try {
            return switch (step) {
                case "git" -> copyGit(selection, target, userId, actual);
                case "modules" -> copyModules(selection, target.getProjectId(), userId, actual);
                case "cases" -> copyCases(selection, target, userId, actual);
                case "documents" -> copyDocuments(selection, target.getProjectId(), actual);
                case "members" -> copyMembers(selection, target, userId);
                case "ai" -> copyAi(selection.getSourceProjectId(), target.getProjectId(), userId);
                case "mcp" -> copyMcp(selection.getSourceProjectId(), target.getProjectId(), userId);
                case "im" -> copyIm(selection, target.getProjectId());
                default -> item(step, ProjectCopyJob.FAILED, "未知分项");
            };
        } catch (RuntimeException e) {
            LOG.warn("复制分项失败 " + step, e);
            Map<String, Object> failed = item(step, ProjectCopyJob.FAILED, trim(e.getMessage(), 500));
            return failed;
        }
    }

    private Map<String, Object> copyGit(ProjectCopySelection selection, SysProject target, Long userId, AtomicLong actual) {
        SysGitRepository query = new SysGitRepository();
        query.setProjectId(selection.getSourceProjectId());
        List<SysGitRepository> repos = sysGitRepositoryMapper.selectSysGitRepositoryList(query);
        Map<String, SysGitRepository> bySlug = new LinkedHashMap<>();
        if (repos != null) {
            for (SysGitRepository repo : repos) {
                if (repo.getSlug() != null) {
                    bySlug.put(repo.getSlug().trim(), repo);
                }
            }
        }
        List<Map<String, Object>> failures = new ArrayList<>();
        int ok = 0;
        for (String slug : selection.getGitSlugs()) {
            SysGitRepository source = bySlug.get(slug);
            if (source == null) {
                failures.add(failure(slug, "源码库不存在"));
                continue;
            }
            Path copied = null;
            try {
                String disk = gitPathValidator.buildDefaultDiskPath(target.getTeamKey(), target.getProjectKey(), slug);
                disk = gitPathValidator.resolveStoredDiskPath(disk);
                copied = copyBare(source.getDiskPath(), disk);
                long bytes = directorySize(copied);
                String finalDisk = disk;
                store.run(() -> insertGitRow(source, target.getProjectId(), slug, finalDisk, userId));
                rememberGitKind(source.getRepositoryId(), slug, target.getProjectId());
                actual.addAndGet(bytes);
                ok++;
            } catch (RuntimeException e) {
                wipeQuietly(copied);
                failures.add(failure(slug, trim(e.getMessage(), 300)));
            }
        }
        return finishCounts("git", ok, failures, "仓库");
    }

    private void insertGitRow(SysGitRepository source, Long targetProjectId, String slug, String disk, Long userId) {
        SysGitRepository row = new SysGitRepository();
        row.setProjectId(targetProjectId);
        row.setName(source.getName() != null ? source.getName() : slug);
        row.setSlug(slug);
        row.setDiskPath(disk);
        row.setDefaultBranch(source.getDefaultBranch() != null ? source.getDefaultBranch() : "main");
        row.setVisibility(source.getVisibility() != null ? source.getVisibility() : "project");
        row.setRemark(source.getRemark());
        row.setCreateBy(String.valueOf(userId));
        row.setUpdateBy(String.valueOf(userId));
        Date now = new Date();
        row.setCreateTime(now);
        row.setUpdateTime(now);
        if (sysGitRepositoryMapper.insertSysGitRepository(row) != 1) {
            throw new ApiException("写入仓库元数据失败");
        }
    }

    /** kind 在元数据提交后再写。列不存在时忽略。不复制 PR / Webhook。 */
    private void rememberGitKind(Long sourceRepositoryId, String slug, Long targetProjectId) {
        try {
            String kind = projectCopyJobMapper.selectGitKind(sourceRepositoryId);
            if (kind == null || kind.isBlank()) {
                return;
            }
            SysGitRepository created = sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(targetProjectId, slug);
            if (created == null || created.getRepositoryId() == null) {
                return;
            }
            Long newId = created.getRepositoryId();
            store.run(() -> projectCopyJobMapper.updateGitKind(newId, kind));
        } catch (RuntimeException ignored) {
            // 无 kind 列时保持默认
        }
    }

    private Path copyBare(String sourceDisk, String targetDisk) {
        if (sourceDisk == null || sourceDisk.isBlank()) {
            throw new ApiException("源 bare 目录不存在");
        }
        String resolved = gitPathValidator.resolveStoredDiskPath(sourceDisk);
        Path source = Path.of(resolved);
        if (!Files.isDirectory(source)) {
            throw new ApiException("源 bare 目录不存在");
        }
        Path target = Path.of(targetDisk);
        if (Files.exists(target)) {
            throw new ApiException("目标仓库目录已存在");
        }
        gitPathValidator.ensureParentWritable(targetDisk);
        try {
            FileUtils.copyDirectory(source.toFile(), target.toFile());
        } catch (IOException e) {
            throw new ApiException("拷贝 bare 失败：" + e.getMessage());
        }
        return target;
    }

    private Map<String, Object> copyModules(ProjectCopySelection selection, Long targetProjectId, Long userId, AtomicLong actual) {
        Long sourceProjectId = selection.getSourceProjectId();
        SysModule query = new SysModule();
        query.setProjectId(sourceProjectId);
        List<SysModule> all = sysModuleMapper.selectSysModuleList(query);
        if (all == null || all.isEmpty()) {
            moduleIdCache.set(Map.of());
            return item("modules", ProjectCopyJob.SUCCESS, "源项目没有交付物");
        }
        LinkedHashSet<Long> want = resolveModuleIds(selection, all);
        List<SysModule> modules = all.stream().filter(m -> want.contains(m.getModuleId())).toList();
        if (modules.isEmpty()) {
            moduleIdCache.set(Map.of());
            return item("modules", ProjectCopyJob.SUCCESS, "未勾选交付物");
        }
        Map<Long, Long> idMap = new LinkedHashMap<>();
        moduleIdCache.set(idMap);
        List<SysModule> pending = new ArrayList<>(modules);
        List<Map<String, Object>> failures = new ArrayList<>();
        int guard = modules.size() + 2;
        while (!pending.isEmpty() && guard-- > 0) {
            int before = pending.size();
            List<SysModule> later = new ArrayList<>();
            for (SysModule source : pending) {
                Long pid = source.getModulePid();
                boolean root = pid == null || pid <= 0 || !containsId(modules, pid);
                if (!root && !idMap.containsKey(pid)) {
                    later.add(source);
                    continue;
                }
                try {
                    long[] bytes = new long[1];
                    String annex = rewriteUrls(source.getAnnexUrls(), bytes);
                    store.run(() -> {
                        SysModule row = new SysModule();
                        row.setProjectId(targetProjectId);
                        row.setModuleName(source.getModuleName());
                        row.setRemark(source.getRemark());
                        row.setAnnexUrls(annex);
                        row.setModulePid(root ? 0L : idMap.get(pid));
                        if (sysModuleMapper.insertSysModule(row) != 1 || row.getModuleId() == null) {
                            throw new ApiException("写入交付物失败");
                        }
                        idMap.put(source.getModuleId(), row.getModuleId());
                    });
                    actual.addAndGet(bytes[0]);
                } catch (RuntimeException e) {
                    failures.add(failure(String.valueOf(source.getModuleId()), trim(e.getMessage(), 300)));
                }
            }
            pending = later;
            if (pending.size() == before) {
                break;
            }
        }
        for (SysModule left : pending) {
            failures.add(failure(String.valueOf(left.getModuleId()), "父交付物未能复制"));
        }
        return finishCounts("modules", idMap.size(), failures, "交付物");
    }

    /** 同一复制线程内，交付物 id 映射供用例使用。 */
    private final ThreadLocal<Map<Long, Long>> moduleIdCache = new ThreadLocal<>();

    private Map<String, Object> copyCases(ProjectCopySelection selection, SysProject target, Long userId, AtomicLong actual) {
        Long sourceProjectId = selection.getSourceProjectId();
        SysCase query = new SysCase();
        query.setProjectId(sourceProjectId);
        List<SysCase> all = sysCaseMapper.selectSysCaseListAll(query);
        if (all == null || all.isEmpty()) {
            return item("cases", ProjectCopyJob.SUCCESS, "源项目没有用例");
        }
        List<SysCase> cases = filterCases(selection, all);
        if (cases.isEmpty()) {
            return item("cases", ProjectCopyJob.SUCCESS, "未勾选用例");
        }
        Map<Long, Long> modules = moduleIdCache.get();
        if (modules == null) {
            modules = Map.of();
        }
        Map<String, Long> typeByName = new LinkedHashMap<>();
        Long fallbackType = null;
        List<SysCaseType> targetTypes = sysCaseTypeMapper.selectByTeamId(target.getTeamId());
        if (targetTypes != null) {
            for (SysCaseType type : targetTypes) {
                if (type.getTypeName() != null && type.getTypeId() != null) {
                    typeByName.putIfAbsent(type.getTypeName(), type.getTypeId());
                    if (fallbackType == null && (type.getEnabled() == null || type.getEnabled() == 1)) {
                        fallbackType = type.getTypeId();
                    }
                }
            }
        }
        List<Map<String, Object>> failures = new ArrayList<>();
        int ok = 0;
        Long fallback = fallbackType;
        Map<Long, Long> moduleMap = modules;
        for (SysCase source : cases) {
            try {
                long[] fileBytes = new long[1];
                String img = rewriteUrls(source.getImgUrls(), fileBytes);
                String annex = rewriteUrls(source.getAnnexUrls(), fileBytes);
                store.run(() -> insertCase(source, target.getProjectId(), userId, moduleMap, typeByName, fallback, img, annex));
                actual.addAndGet(fileBytes[0]);
                ok++;
            } catch (RuntimeException e) {
                failures.add(failure(source.getCaseName(), trim(e.getMessage(), 300)));
            }
        }
        return finishCounts("cases", ok, failures, "用例");
    }

    private void insertCase(SysCase source, Long targetProjectId, Long userId, Map<Long, Long> moduleMap,
                            Map<String, Long> typeByName, Long fallbackType, String imgUrls, String annexUrls) {
        SysCase row = new SysCase();
        row.setProjectId(targetProjectId);
        row.setCaseName(source.getCaseName());
        row.setCaseExpect(source.getCaseExpect());
        row.setCaseStep(source.getCaseStep());
        row.setCaseLevel(source.getCaseLevel());
        row.setCasePreconditions(source.getCasePreconditions());
        row.setCaseNum(source.getCaseNum());
        row.setRemark(source.getRemark());
        row.setImgUrls(imgUrls);
        row.setAnnexUrls(annexUrls);
        row.setCaseData(source.getCaseData());
        row.setCreateBy(source.getCreateBy() != null ? source.getCreateBy() : String.valueOf(userId));
        row.setUpdateBy(String.valueOf(userId));
        Date now = new Date();
        row.setCreateTime(now);
        row.setUpdateTime(now);
        if (source.getModuleId() != null) {
            row.setModuleId(moduleMap.get(source.getModuleId()));
        }
        // ADR-023：类型按目标团队名称匹配，对不上用默认类型，再没有则留空。不把源团队字典写入目标团队。
        if (source.getCaseType() != null || (source.getCaseTypeName() != null && !source.getCaseTypeName().isBlank())) {
            String name = source.getCaseTypeName();
            if (name == null || name.isBlank()) {
                SysCaseType sourceType = sysCaseTypeMapper.selectById(source.getCaseType());
                name = sourceType != null ? sourceType.getTypeName() : null;
            }
            Long mapped = name != null ? typeByName.get(name) : null;
            row.setCaseType(mapped != null ? mapped : fallbackType);
        }
        if (sysCaseMapper.insertSysCase(row) != 1) {
            throw new ApiException("写入用例失败");
        }
    }

    private Map<String, Object> copyDocuments(ProjectCopySelection selection, Long targetProjectId, AtomicLong actual) {
        Long sourceProjectId = selection.getSourceProjectId();
        SysDocument query = new SysDocument();
        query.setProjectId(sourceProjectId);
        List<SysDocument> all = sysDocumentMapper.selectSysDocumentList(query, 0, 100_000);
        if (all == null || all.isEmpty()) {
            return item("documents", ProjectCopyJob.SUCCESS, "源项目没有文档");
        }
        LinkedHashSet<Long> want = resolveDocumentIds(selection, all);
        List<SysDocument> docs = all.stream().filter(d -> want.contains(d.getDocId())).toList();
        if (docs.isEmpty()) {
            return item("documents", ProjectCopyJob.SUCCESS, "未勾选文档");
        }
        Map<Long, Long> idMap = new LinkedHashMap<>();
        Map<String, String> copiedFiles = new LinkedHashMap<>();
        List<SysDocument> pending = new ArrayList<>(docs);
        List<Map<String, Object>> failures = new ArrayList<>();
        int guard = docs.size() + 2;
        while (!pending.isEmpty() && guard-- > 0) {
            int before = pending.size();
            List<SysDocument> later = new ArrayList<>();
            for (SysDocument source : pending) {
                Long pid = source.getDocPid();
                boolean root = pid == null || pid <= 0 || !containsDoc(docs, pid);
                if (!root && !idMap.containsKey(pid)) {
                    later.add(source);
                    continue;
                }
                try {
                    long[] bytes = new long[1];
                    String fileUrl = copyDocumentFile(source.getFileUrl(), copiedFiles, bytes);
                    store.run(() -> {
                        SysDocument row = new SysDocument();
                        row.setProjectId(targetProjectId);
                        row.setDocName(source.getDocName() != null && !source.getDocName().isBlank()
                                ? source.getDocName() : "未命名");
                        row.setDocType(source.getDocType());
                        row.setFileExtension(source.getFileExtension());
                        row.setFileVersion(source.getFileVersion());
                        row.setDocRemakr(source.getDocRemakr());
                        row.setFileUrl(fileUrl);
                        row.setCreateById(source.getCreateById());
                        row.setUpdateById(source.getUpdateById());
                        Date now = new Date();
                        row.setCreateTime(source.getCreateTime() != null ? source.getCreateTime() : now);
                        row.setUpdateTime(now);
                        row.setDocPid(root ? 0L : idMap.get(pid));
                        if (sysDocumentMapper.insertSysDocument(row) != 1 || row.getDocId() == null) {
                            throw new ApiException("写入文档失败");
                        }
                        idMap.put(source.getDocId(), row.getDocId());
                    });
                    actual.addAndGet(bytes[0]);
                } catch (RuntimeException e) {
                    failures.add(failure(source.getDocName(), trim(e.getMessage(), 300)));
                }
            }
            pending = later;
            if (pending.size() == before) {
                break;
            }
        }
        for (SysDocument left : pending) {
            failures.add(failure(left.getDocName(), "父文档未能复制"));
        }
        return finishCounts("documents", idMap.size(), failures, "文档");
    }

    private Map<String, Object> copyMembers(ProjectCopySelection selection, SysProject target, Long userId) {
        List<SysUser> members = filterMembers(selection,
                sysUserProjectService.selectSysUserListByProjectId(selection.getSourceProjectId()));
        if (members.isEmpty()) {
            return item("members", ProjectCopyJob.SUCCESS, "源项目没有可复制的成员");
        }
        List<Map<String, Object>> failures = new ArrayList<>();
        int ok = 0;
        for (SysUser member : members) {
            if (member.getUserId() == null) {
                continue;
            }
            if (member.getUserId().equals(userId)) {
                // 创建人已在新项目上，只追加除 project.create-by 以外的角色
                addCreatorExtraRoles(target.getProjectId(), userId, member, failures);
                ok++;
                continue;
            }
            // 已在「项目成员」表单加入的，去重跳过插入，保留创建时选定的角色
            if (isAlreadyProjectMember(target.getProjectId(), member.getUserId())) {
                ok++;
                continue;
            }
            try {
                ensureTeamMember(target.getTeamId(), member.getUserId(), userId);
                List<Long> roles = new ArrayList<>();
                if (member.getRoles() != null) {
                    for (SysRole role : member.getRoles()) {
                        if (role.getRoleId() != null && role.getRoleId() != ROLE_PROJECT_CREATOR) {
                            roles.add(role.getRoleId());
                        }
                    }
                }
                if (roles.isEmpty()) {
                    roles.add(ROLE_PROJECT_ADMIN);
                }
                BatchUserRoleVo batch = new BatchUserRoleVo();
                batch.setProjectId(target.getProjectId());
                batch.setMemberIds(new Long[]{member.getUserId()});
                batch.setRoleIds(roles.toArray(Long[]::new));
                store.run(() -> sysUserProjectService.batchInsertSysUserProject(batch));
                ok++;
            } catch (ApiException e) {
                failures.add(failure(String.valueOf(member.getUserId()), trim(e.getMessage(), 300)));
            } catch (RuntimeException e) {
                failures.add(failure(String.valueOf(member.getUserId()), trim(e.getMessage(), 300)));
            }
        }
        return finishCounts("members", ok, failures, "成员");
    }

    private boolean isAlreadyProjectMember(Long projectId, Long memberId) {
        SysUserProject query = new SysUserProject();
        query.setProjectId(projectId);
        query.setUserId(memberId);
        List<SysUserProject> rows = sysUserProjectMapper.selectSysUserProjectList(query);
        return rows != null && !rows.isEmpty();
    }

    /** 创建人保留 project.create-by，源项目上的其他角色追加，不替换创建人角色。 */
    private void addCreatorExtraRoles(Long projectId, Long userId, SysUser member, List<Map<String, Object>> failures) {
        if (member.getRoles() == null || member.getRoles().isEmpty()) {
            return;
        }
        SysUserProject query = new SysUserProject();
        query.setProjectId(projectId);
        query.setUserId(userId);
        List<SysUserProject> rows = sysUserProjectMapper.selectSysUserProjectList(query);
        if (rows == null || rows.isEmpty() || rows.get(0).getUserProjectId() == null) {
            return;
        }
        Long userProjectId = rows.get(0).getUserProjectId();
        for (SysRole role : member.getRoles()) {
            if (role.getRoleId() == null || role.getRoleId() == ROLE_PROJECT_CREATOR) {
                continue;
            }
            Long roleId = role.getRoleId();
            try {
                store.run(() -> {
                    SysUserProjectRole link = new SysUserProjectRole();
                    link.setUserProjectId(userProjectId);
                    link.setRoleId(roleId);
                    sysUserProjectRoleMapper.insertSysUserProjectRole(link);
                });
            } catch (RuntimeException e) {
                failures.add(failure(String.valueOf(userId), trim(e.getMessage(), 300)));
            }
        }
    }

    /** 不在目标团队则按普通成员入团；超员跳过，由调用方记入失败项。 */
    private void ensureTeamMember(Long teamId, Long memberId, Long operatorId) {
        SysUserTeam query = new SysUserTeam();
        query.setTeamId(teamId);
        query.setUserId(memberId);
        List<SysUserTeam> existing = sysUserTeamMapper.selectSysUserTeamList(query);
        if (existing != null && !existing.isEmpty()) {
            SysUserTeam row = existing.get(0);
            if (row.getTeamLock() != null && row.getTeamLock() == 1) {
                throw new ApiException("团队成员已锁定，已跳过");
            }
            return;
        }
        sysTeamPlanService.assertCanAddMembers(teamId, 1);
        store.run(() -> {
            SysUserTeam team = new SysUserTeam();
            team.setTeamId(teamId);
            team.setUserId(memberId);
            team.setCreateBy(String.valueOf(operatorId));
            team.setCreateTime(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            if (sysUserTeamMapper.insertSysUserTeam(team) != 1 || team.getUserTeamId() == null) {
                throw new ApiException("加入团队失败");
            }
            SysUserTeamRole role = new SysUserTeamRole();
            role.setUserTeamId(team.getUserTeamId());
            role.setRoleId(ROLE_TEAM_DEFAULT);
            if (sysUserTeamRoleMapper.insertSysUserTeamRole(role) != 1) {
                throw new ApiException("写入团队角色失败");
            }
        });
    }

    private Map<String, Object> copyAi(Long sourceProjectId, Long targetProjectId, Long userId) {
        List<Map<String, Object>> failures = new ArrayList<>();
        int ok = 0;
        AiAccount query = new AiAccount();
        query.setProjectId(sourceProjectId);
        List<AiAccount> accounts = aiAccountMapper.selectAiAccountList(query);
        if (accounts != null) {
            for (AiAccount account : accounts) {
                if (account.getAiUrl() == null || account.getAiUrl().isBlank()
                        || account.getModelName() == null || account.getModelName().isBlank()) {
                    failures.add(failure(account.getAccountName(), "账号地址或模型为空"));
                    continue;
                }
                String name = account.getAccountName() != null && !account.getAccountName().isBlank()
                        ? account.getAccountName() : account.getModelName();
                try {
                    store.run(() -> {
                        // 不复制 API Key：列非空，置空串，需使用前重新填写
                        if (projectCopyJobMapper.insertAiAccountBlankKey(
                                account.getAiUrl(), account.getModelName(), account.getMaxCompletionTokens(),
                                userId, targetProjectId, name) != 1) {
                            throw new ApiException("写入 AI 账号失败");
                        }
                    });
                    ok++;
                } catch (RuntimeException e) {
                    failures.add(failure(name, trim(e.getMessage(), 300)));
                }
            }
        }
        SysAiModuleConfig aiConfig = sysAiModuleConfigMapper.selectSysAiModuleConfigByProjectId(sourceProjectId);
        if (aiConfig != null && sysAiModuleConfigMapper.selectSysAiModuleConfigByProjectId(targetProjectId) == null) {
            try {
                store.run(() -> {
                    SysAiModuleConfig row = new SysAiModuleConfig();
                    row.setProjectId(targetProjectId);
                    row.setCreateById(aiConfig.getCreateById() != null ? aiConfig.getCreateById() : userId);
                    row.setUpdateById(userId);
                    Date now = new Date();
                    row.setCreateTime(now);
                    row.setUpdateTime(now);
                    sysAiModuleConfigMapper.insertSysAiModuleConfig(row);
                });
                ok++;
            } catch (RuntimeException e) {
                failures.add(failure("ai-module", trim(e.getMessage(), 300)));
            }
        }
        if (ok == 0 && failures.isEmpty()) {
            return item("ai", ProjectCopyJob.SUCCESS, "源项目没有 AI 配置");
        }
        return finishCounts("ai", ok, failures, "AI 配置");
    }

    private Map<String, Object> copyMcp(Long sourceProjectId, Long targetProjectId, Long userId) {
        List<Map<String, Object>> failures = new ArrayList<>();
        int ok = 0;
        try {
            SysProjectMcp mcp = sysProjectMcpMapper.selectByProjectId(sourceProjectId);
            if (mcp != null) {
                store.run(() -> sysProjectMcpService.save(
                        targetProjectId, mcp.isEnabled(), mcp.getModules(), String.valueOf(userId)));
                ok++;
            }
        } catch (RuntimeException e) {
            failures.add(failure("mcp", trim(e.getMessage(), 300)));
        }
        try {
            store.run(() -> projectCopyJobMapper.copyMcpServers(sourceProjectId, targetProjectId));
        } catch (RuntimeException e) {
            failures.add(failure("mcp-server", "外部 MCP 未复制（" + trim(e.getMessage(), 180) + "），凭证已要求置空"));
        }
        if (ok == 0 && failures.isEmpty()) {
            return item("mcp", ProjectCopyJob.SUCCESS, "源项目没有 MCP 配置");
        }
        return finishCounts("mcp", ok, failures, "MCP 配置");
    }

    private Map<String, Object> copyIm(ProjectCopySelection selection, Long targetProjectId) {
        LinkedHashSet<String> wantCodes = new LinkedHashSet<>();
        if (selection.getImSystemCodes() != null) {
            for (String code : selection.getImSystemCodes()) {
                if (code != null && !code.isBlank()) {
                    wantCodes.add(code.trim().toLowerCase());
                }
            }
        }
        if (wantCodes.isEmpty()) {
            return item("im", ProjectCopyJob.SUCCESS, "未勾选第三方应用");
        }
        IMProjectConfig query = new IMProjectConfig();
        query.setProjectId(selection.getSourceProjectId());
        List<IMProjectConfig> configs = imProjectConfigMapper.selectImProjectConfigList(query);
        if (configs == null || configs.isEmpty()) {
            return item("im", ProjectCopyJob.SUCCESS, "源项目没有 IM 配置");
        }
        List<Map<String, Object>> failures = new ArrayList<>();
        int ok = 0;
        for (IMProjectConfig source : configs) {
            String code = source.getSystemCode() == null ? "" : source.getSystemCode().trim().toLowerCase();
            if (!wantCodes.contains(code)) {
                continue;
            }
            try {
                String params = blankSecrets(source.getConfigParams());
                store.run(() -> {
                    IMProjectConfig row = new IMProjectConfig();
                    row.setProjectId(targetProjectId);
                    row.setSystemCode(source.getSystemCode());
                    row.setConfigParams(params);
                    if (imProjectConfigMapper.insertImProjectConfig(row) != 1) {
                        throw new ApiException("写入 IM 配置失败");
                    }
                });
                ok++;
            } catch (RuntimeException e) {
                failures.add(failure(source.getSystemCode(), trim(e.getMessage(), 300)));
            }
        }
        if (ok == 0 && failures.isEmpty()) {
            return item("im", ProjectCopyJob.SUCCESS, "源项目没有勾选的第三方应用配置");
        }
        return finishCounts("im", ok, failures, "IM 配置");
    }

    private Map<String, Object> buildEstimate(ProjectCopySelection selection) {
        selection.normalize();
        long gitBytes = 0L;
        List<Map<String, Object>> repositories = new ArrayList<>();
        SysGitRepository query = new SysGitRepository();
        query.setProjectId(selection.getSourceProjectId());
        List<SysGitRepository> repos = sysGitRepositoryMapper.selectSysGitRepositoryList(query);
        Map<String, SysGitRepository> bySlug = new LinkedHashMap<>();
        if (repos != null) {
            for (SysGitRepository repo : repos) {
                if (repo.getSlug() == null || repo.getSlug().isBlank()) {
                    continue;
                }
                String slug = repo.getSlug().trim();
                bySlug.put(slug, repo);
                long bytes = 0L;
                if (repo.getDiskPath() != null) {
                    try {
                        bytes = directorySize(Path.of(gitPathValidator.resolveStoredDiskPath(repo.getDiskPath())));
                    } catch (RuntimeException ignored) {
                        bytes = 0L;
                    }
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("slug", slug);
                row.put("name", repo.getName());
                row.put("bytes", bytes);
                repositories.add(row);
            }
        }
        LinkedHashSet<String> slugs = new LinkedHashSet<>();
        for (String slug : selection.getGitSlugs()) {
            if (slug == null || slug.isBlank()) {
                continue;
            }
            String trimmed = slug.trim();
            if (!bySlug.containsKey(trimmed)) {
                throw new ApiException("源码库不存在：" + trimmed);
            }
            slugs.add(trimmed);
        }
        selection.setGitSlugs(new ArrayList<>(slugs));
        for (String slug : slugs) {
            for (Map<String, Object> row : repositories) {
                if (slug.equals(row.get("slug"))) {
                    gitBytes += ((Number) row.get("bytes")).longValue();
                }
            }
        }

        long moduleBytes = 0L;
        long caseBytes = 0L;
        SysModule moduleQuery = new SysModule();
        moduleQuery.setProjectId(selection.getSourceProjectId());
        List<SysModule> allModules = sysModuleMapper.selectSysModuleList(moduleQuery);
        if (selection.wantsModules() || selection.wantsCases()) {
            LinkedHashSet<Long> wantModules = resolveModuleIds(selection, allModules != null ? allModules : List.of());
            if (allModules != null) {
                for (SysModule module : allModules) {
                    if (!wantModules.contains(module.getModuleId())) {
                        continue;
                    }
                    moduleBytes += MODULE_ROW_BYTES;
                    moduleBytes += urlBytes(module.getAnnexUrls());
                }
            }
        }
        if (selection.wantsCases()) {
            SysCase caseQuery = new SysCase();
            caseQuery.setProjectId(selection.getSourceProjectId());
            List<SysCase> cases = filterCases(selection, sysCaseMapper.selectSysCaseListAll(caseQuery));
            for (SysCase row : cases) {
                caseBytes += CASE_ROW_BYTES;
                caseBytes += urlBytes(row.getImgUrls());
                caseBytes += urlBytes(row.getAnnexUrls());
            }
        }
        long documentBytes = 0L;
        if (selection.wantsDocuments()) {
            SysDocument docQuery = new SysDocument();
            docQuery.setProjectId(selection.getSourceProjectId());
            List<SysDocument> allDocs = sysDocumentMapper.selectSysDocumentList(docQuery, 0, 100_000);
            LinkedHashSet<Long> wantDocs = resolveDocumentIds(selection, allDocs != null ? allDocs : List.of());
            if (allDocs != null) {
                for (SysDocument doc : allDocs) {
                    if (wantDocs.contains(doc.getDocId())) {
                        documentBytes += urlBytes(doc.getFileUrl());
                    }
                }
            }
        }
        long configBytes = 0L;
        if (selection.wantsMembers()) {
            configBytes += CONFIG_BLOCK_BYTES;
        }
        if (selection.isCopyAi()) {
            configBytes += CONFIG_BLOCK_BYTES;
        }
        if (selection.isCopyMcp()) {
            configBytes += CONFIG_BLOCK_BYTES;
        }
        if (selection.wantsIm()) {
            configBytes += CONFIG_BLOCK_BYTES * selection.getImSystemCodes().size();
        }
        long total = gitBytes + moduleBytes + caseBytes + documentBytes + configBytes;
        selection.setEstimatedBytes(total);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceProjectId", selection.getSourceProjectId());
        body.put("estimatedBytes", total);
        body.put("approximate", true);
        body.put("copyModules", selection.wantsModules());
        body.put("copyCases", selection.wantsCases());
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("git", gitBytes);
        breakdown.put("modules", moduleBytes);
        breakdown.put("cases", caseBytes);
        breakdown.put("documents", documentBytes);
        breakdown.put("config", configBytes);
        body.put("breakdown", breakdown);
        body.put("repositories", repositories);
        return body;
    }

    /**
     * 源项目可勾选项列表（需对该源有创建人/管理员权限）。
     * type: git | cases | modules | documents | members
     */
    public List<Map<String, Object>> listCopyItems(Long userId, Long sourceProjectId, String type, String search) {
        ProjectCopySelection probe = new ProjectCopySelection();
        probe.setSourceProjectId(sourceProjectId);
        requireSourceAdmin(userId, probe);
        String kind = type == null ? "" : type.trim().toLowerCase();
        String q = search == null ? "" : search.trim().toLowerCase();
        List<Map<String, Object>> items = new ArrayList<>();
        switch (kind) {
            case "git" -> {
                SysGitRepository query = new SysGitRepository();
                query.setProjectId(sourceProjectId);
                List<SysGitRepository> repos = sysGitRepositoryMapper.selectSysGitRepositoryList(query);
                if (repos != null) {
                    for (SysGitRepository repo : repos) {
                        if (repo.getSlug() == null || repo.getSlug().isBlank()) {
                            continue;
                        }
                        String name = repo.getName() != null ? repo.getName() : repo.getSlug();
                        if (!q.isEmpty() && !name.toLowerCase().contains(q) && !repo.getSlug().toLowerCase().contains(q)) {
                            continue;
                        }
                        long bytes = 0L;
                        if (repo.getDiskPath() != null) {
                            try {
                                bytes = directorySize(Path.of(gitPathValidator.resolveStoredDiskPath(repo.getDiskPath())));
                            } catch (RuntimeException ignored) {
                                bytes = 0L;
                            }
                        }
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", repo.getSlug().trim());
                        row.put("name", name);
                        row.put("bytes", bytes);
                        items.add(row);
                    }
                }
            }
            case "cases" -> {
                SysCase caseQuery = new SysCase();
                caseQuery.setProjectId(sourceProjectId);
                List<SysCase> cases = sysCaseMapper.selectSysCaseListAll(caseQuery);
                if (cases != null) {
                    for (SysCase row : cases) {
                        String name = row.getCaseName() != null ? row.getCaseName() : String.valueOf(row.getCaseId());
                        if (!q.isEmpty() && !name.toLowerCase().contains(q)) {
                            continue;
                        }
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", row.getCaseId());
                        item.put("name", name);
                        item.put("moduleId", row.getModuleId());
                        items.add(item);
                    }
                }
            }
            case "modules" -> {
                // 扁平返回完整树节点（id/name/parentId），搜索由前端 el-tree 过滤，避免断链
                SysModule moduleQuery = new SysModule();
                moduleQuery.setProjectId(sourceProjectId);
                List<SysModule> modules = sysModuleMapper.selectSysModuleList(moduleQuery);
                if (modules != null) {
                    for (SysModule m : modules) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", m.getModuleId());
                        item.put("name", m.getModuleName() != null ? m.getModuleName() : String.valueOf(m.getModuleId()));
                        Long pid = m.getModulePid();
                        item.put("parentId", (pid == null || pid <= 0) ? null : pid);
                        items.add(item);
                    }
                }
            }
            case "documents" -> {
                // 扁平返回完整树节点；docType==0 为文件夹，搜索由前端树过滤
                SysDocument docQuery = new SysDocument();
                docQuery.setProjectId(sourceProjectId);
                List<SysDocument> docs = sysDocumentMapper.selectSysDocumentList(docQuery, 0, 100_000);
                if (docs != null) {
                    for (SysDocument d : docs) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", d.getDocId());
                        item.put("name", d.getDocName() != null ? d.getDocName() : String.valueOf(d.getDocId()));
                        Long pid = d.getDocPid();
                        item.put("parentId", (pid == null || pid <= 0) ? null : pid);
                        long docType = d.getDocType() == null ? 1L : d.getDocType();
                        item.put("folder", docType == 0L);
                        items.add(item);
                    }
                }
            }
            case "members" -> {
                List<SysUser> members = sysUserProjectService.selectSysUserListByProjectId(sourceProjectId);
                if (members != null) {
                    for (SysUser member : members) {
                        if (member.getUserId() == null) {
                            continue;
                        }
                        String nick = member.getNickName() != null && !member.getNickName().isBlank()
                                ? member.getNickName() : "";
                        String userName = member.getUserName() != null ? member.getUserName() : "";
                        String phone = member.getPhoneNumber() != null ? member.getPhoneNumber() : "";
                        String email = member.getEmail() != null ? member.getEmail() : "";
                        String name = !nick.isEmpty() ? nick : (!userName.isEmpty() ? userName : String.valueOf(member.getUserId()));
                        if (!q.isEmpty()
                                && !name.toLowerCase().contains(q)
                                && !userName.toLowerCase().contains(q)
                                && !nick.toLowerCase().contains(q)
                                && !phone.toLowerCase().contains(q)
                                && !email.toLowerCase().contains(q)) {
                            continue;
                        }
                        List<Map<String, Object>> roles = new ArrayList<>();
                        if (member.getRoles() != null) {
                            for (SysRole role : member.getRoles()) {
                                if (role.getRoleId() == null) {
                                    continue;
                                }
                                Map<String, Object> roleRow = new LinkedHashMap<>();
                                roleRow.put("roleId", role.getRoleId());
                                roleRow.put("roleName", role.getRoleName() != null ? role.getRoleName() : "");
                                roles.add(roleRow);
                            }
                        }
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", member.getUserId());
                        item.put("userId", member.getUserId());
                        item.put("name", name);
                        item.put("nickName", name);
                        item.put("userName", userName);
                        item.put("phoneNumber", phone);
                        item.put("email", email);
                        item.put("avatar", member.getAvatar());
                        item.put("roles", roles);
                        items.add(item);
                    }
                }
            }
            default -> throw new ApiException(400, "未知的复制类型：" + type);
        }
        return items;
    }

    private static List<SysUser> filterMembers(ProjectCopySelection selection, List<SysUser> all) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        if (selection.isCopyAllMembers()) {
            return all;
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>(selection.getMemberIds() != null ? selection.getMemberIds() : List.of());
        if (ids.isEmpty()) {
            return List.of();
        }
        List<SysUser> out = new ArrayList<>();
        for (SysUser member : all) {
            if (member.getUserId() != null && ids.contains(member.getUserId())) {
                out.add(member);
            }
        }
        return out;
    }

    private static List<SysCase> filterCases(ProjectCopySelection selection, List<SysCase> all) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        if (selection.isCopyAllCases()) {
            return all;
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>(selection.getCaseIds() != null ? selection.getCaseIds() : List.of());
        List<SysCase> out = new ArrayList<>();
        for (SysCase row : all) {
            if (ids.contains(row.getCaseId())) {
                out.add(row);
            }
        }
        return out;
    }

    private LinkedHashSet<Long> resolveModuleIds(ProjectCopySelection selection, List<SysModule> all) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (selection.isCopyAllModules()) {
            for (SysModule m : all) {
                if (m.getModuleId() != null) {
                    ids.add(m.getModuleId());
                }
            }
            return ids;
        }
        if (selection.getModuleIds() != null) {
            ids.addAll(selection.getModuleIds());
        }
        if (selection.wantsCases()) {
            SysCase caseQuery = new SysCase();
            caseQuery.setProjectId(selection.getSourceProjectId());
            for (SysCase row : filterCases(selection, sysCaseMapper.selectSysCaseListAll(caseQuery))) {
                if (row.getModuleId() != null) {
                    ids.add(row.getModuleId());
                }
            }
        }
        return expandModuleAncestors(ids, all);
    }

    private static LinkedHashSet<Long> expandModuleAncestors(LinkedHashSet<Long> ids, List<SysModule> all) {
        Map<Long, SysModule> byId = new LinkedHashMap<>();
        for (SysModule m : all) {
            byId.put(m.getModuleId(), m);
        }
        LinkedHashSet<Long> out = new LinkedHashSet<>();
        for (Long id : ids) {
            Long cur = id;
            int guard = byId.size() + 2;
            while (cur != null && cur > 0 && guard-- > 0) {
                if (!out.add(cur)) {
                    break;
                }
                SysModule m = byId.get(cur);
                if (m == null) {
                    break;
                }
                Long pid = m.getModulePid();
                cur = (pid == null || pid <= 0) ? null : pid;
            }
        }
        return out;
    }

    private static LinkedHashSet<Long> resolveDocumentIds(ProjectCopySelection selection, List<SysDocument> all) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (selection.isCopyAllDocuments()) {
            for (SysDocument d : all) {
                if (d.getDocId() != null) {
                    ids.add(d.getDocId());
                }
            }
            return ids;
        }
        if (selection.getDocumentIds() != null) {
            ids.addAll(selection.getDocumentIds());
        }
        return expandDocumentAncestors(ids, all);
    }

    private static LinkedHashSet<Long> expandDocumentAncestors(LinkedHashSet<Long> ids, List<SysDocument> all) {
        Map<Long, SysDocument> byId = new LinkedHashMap<>();
        for (SysDocument d : all) {
            byId.put(d.getDocId(), d);
        }
        LinkedHashSet<Long> out = new LinkedHashSet<>();
        for (Long id : ids) {
            Long cur = id;
            int guard = byId.size() + 2;
            while (cur != null && cur > 0 && guard-- > 0) {
                if (!out.add(cur)) {
                    break;
                }
                SysDocument d = byId.get(cur);
                if (d == null) {
                    break;
                }
                Long pid = d.getDocPid();
                cur = (pid == null || pid <= 0) ? null : pid;
            }
        }
        return out;
    }

    private static String modulePath(SysModule module, Map<Long, SysModule> byId) {
        List<String> parts = new ArrayList<>();
        Long cur = module.getModuleId();
        int guard = byId.size() + 2;
        while (cur != null && cur > 0 && guard-- > 0) {
            SysModule m = byId.get(cur);
            if (m == null) {
                break;
            }
            parts.add(0, m.getModuleName() != null ? m.getModuleName() : String.valueOf(cur));
            Long pid = m.getModulePid();
            cur = (pid == null || pid <= 0) ? null : pid;
        }
        return String.join(" / ", parts);
    }

    private static String documentPath(SysDocument doc, Map<Long, SysDocument> byId) {
        List<String> parts = new ArrayList<>();
        Long cur = doc.getDocId();
        int guard = byId.size() + 2;
        while (cur != null && cur > 0 && guard-- > 0) {
            SysDocument d = byId.get(cur);
            if (d == null) {
                break;
            }
            parts.add(0, d.getDocName() != null ? d.getDocName() : String.valueOf(cur));
            Long pid = d.getDocPid();
            cur = (pid == null || pid <= 0) ? null : pid;
        }
        return String.join(" / ", parts);
    }

    private void requireSourceAdmin(Long userId, ProjectCopySelection selection) {
        if (selection == null || selection.getSourceProjectId() == null) {
            throw new ApiException("请选择源项目");
        }
        if (userId == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        if (projectCopyJobMapper.countCreatorOrAdmin(userId, selection.getSourceProjectId()) <= 0) {
            throw new ApiException(403, "只能复制你作为创建人或管理员的项目");
        }
    }

    private static List<String> plannedSteps(ProjectCopySelection selection) {
        List<String> steps = new ArrayList<>();
        if (selection.getGitSlugs() != null && !selection.getGitSlugs().isEmpty()) {
            steps.add("git");
        }
        if (selection.wantsModules()) {
            steps.add("modules");
        }
        if (selection.wantsCases()) {
            steps.add("cases");
        }
        if (selection.wantsDocuments()) {
            steps.add("documents");
        }
        if (selection.wantsMembers()) {
            steps.add("members");
        }
        if (selection.isCopyAi()) {
            steps.add("ai");
        }
        if (selection.isCopyMcp()) {
            steps.add("mcp");
        }
        if (selection.wantsIm()) {
            steps.add("im");
        }
        return steps;
    }

    private String rewriteUrls(String raw, long[] bytesOut) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        boolean json = raw.trim().startsWith("[");
        List<String> urls = splitUrls(raw);
        List<String> rewritten = new ArrayList<>();
        for (String url : urls) {
            String next = copyOneFile(url, bytesOut);
            rewritten.add(next != null ? next : url);
        }
        if (!json) {
            return String.join(",", rewritten);
        }
        try {
            return JSON.writeValueAsString(rewritten);
        } catch (IOException e) {
            return String.join(",", rewritten);
        }
    }

    private String copyDocumentFile(String fileUrl, Map<String, String> cache, long[] bytesOut) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return fileUrl;
        }
        if (cache.containsKey(fileUrl)) {
            return cache.get(fileUrl);
        }
        String copied = copyOneFile(fileUrl, bytesOut);
        String result = copied != null ? copied : fileUrl;
        cache.put(fileUrl, result);
        return result;
    }

    /** 复制到 upload 新路径。源文件缺失时返回 null，调用方保留原地址。 */
    private String copyOneFile(String fileUrl, long[] bytesOut) {
        Path source = resolveProfileFile(fileUrl);
        if (source == null || !Files.isRegularFile(source)) {
            return null;
        }
        try {
            String ext = "";
            String name = source.getFileName().toString();
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                ext = name.substring(dot);
            }
            String day = LocalDate.now().format(DAY);
            String stored = "copy_" + UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = profileConfig.resolveUploadPath("upload/" + day + "/" + stored);
            Files.createDirectories(target.getParent());
            Files.copy(source, target);
            if (bytesOut != null && bytesOut.length > 0) {
                bytesOut[0] += Files.size(target);
            }
            return "/profile/upload/" + day + "/" + stored;
        } catch (IOException e) {
            return null;
        }
    }

    private long urlBytes(String raw) {
        long total = 0L;
        for (String url : splitUrls(raw)) {
            Path file = resolveProfileFile(url);
            if (file != null && Files.isRegularFile(file)) {
                try {
                    total += Files.size(file);
                } catch (IOException ignored) {
                    // 单个附件读失败不让预估失败
                }
            }
        }
        return total;
    }

    private Path resolveProfileFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String path = fileName.startsWith("/") ? fileName : "/" + fileName;
        String relative = path.startsWith("/profile/") ? path.substring("/profile/".length()) : path.replaceFirst("^/", "");
        try {
            return profileConfig.resolveUploadPath(relative);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static List<String> splitUrls(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            try {
                JsonNode node = JSON.readTree(trimmed);
                if (node.isArray()) {
                    for (JsonNode item : node) {
                        if (item.isTextual() && !item.asText().isBlank()) {
                            out.add(item.asText().trim());
                        }
                    }
                    return out;
                }
            } catch (IOException ignored) {
                // 按逗号继续拆
            }
        }
        for (String part : trimmed.split(",")) {
            if (!part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    private String blankSecrets(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }
        try {
            JsonNode node = JSON.readTree(json);
            blankNode(node);
            return JSON.writeValueAsString(node);
        } catch (IOException e) {
            return "{}";
        }
    }

    private void blankNode(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            List<String> names = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                if (SECRET_KEY.matcher(name).matches()) {
                    objectNode.put(name, "");
                } else {
                    blankNode(objectNode.get(name));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                blankNode(child);
            }
        }
    }

    private static long directorySize(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            return 0L;
        }
        long total = 0L;
        try (var walk = Files.walk(root)) {
            var iterator = walk.filter(Files::isRegularFile).iterator();
            while (iterator.hasNext()) {
                try {
                    total += Files.size(iterator.next());
                } catch (IOException ignored) {
                    // 单个文件失败不影响整仓体积
                }
            }
        } catch (IOException e) {
            return total;
        }
        return total;
    }

    private static boolean containsId(List<SysModule> modules, Long id) {
        for (SysModule module : modules) {
            if (id.equals(module.getModuleId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsDoc(List<SysDocument> docs, Long id) {
        for (SysDocument doc : docs) {
            if (id.equals(doc.getDocId())) {
                return true;
            }
        }
        return false;
    }

    private static void wipeQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try {
            FileUtils.deleteDirectory(dir.toFile());
        } catch (IOException ignored) {
            // 清理失败留待人工处理，不掩盖原始错误
        }
    }

    private static Map<String, Object> finishCounts(String key, int ok, List<Map<String, Object>> failures, String label) {
        String state;
        if (failures.isEmpty()) {
            state = ProjectCopyJob.SUCCESS;
        } else if (ok > 0) {
            state = ProjectCopyJob.PARTIAL;
        } else {
            state = ProjectCopyJob.FAILED;
        }
        Map<String, Object> item = item(key, state, label + "成功 " + ok + "，失败 " + failures.size());
        if (!failures.isEmpty()) {
            item.put("failures", failures);
        }
        return item;
    }

    private static String aggregateState(List<Map<String, Object>> items) {
        boolean anyProgress = false;
        boolean anyBad = false;
        for (Map<String, Object> item : items) {
            String state = String.valueOf(item.get("state"));
            if (ProjectCopyJob.SUCCESS.equals(state)) {
                anyProgress = true;
            } else if (ProjectCopyJob.PARTIAL.equals(state)) {
                anyProgress = true;
                anyBad = true;
            } else {
                anyBad = true;
            }
        }
        if (!anyBad) {
            return ProjectCopyJob.SUCCESS;
        }
        if (anyProgress) {
            return ProjectCopyJob.PARTIAL;
        }
        return ProjectCopyJob.FAILED;
    }

    private static String summarize(List<Map<String, Object>> items) {
        List<String> bits = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String state = String.valueOf(item.get("state"));
            if (!ProjectCopyJob.SUCCESS.equals(state)) {
                bits.add(item.get("key") + ":" + item.get("message"));
            }
        }
        if (bits.isEmpty()) {
            return null;
        }
        return trim(String.join("；", bits), 2000);
    }

    private static Map<String, Object> item(String key, String state, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("state", state);
        item.put("message", message);
        return item;
    }

    private static Map<String, Object> failure(String ref, String reason) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ref", ref);
        row.put("reason", reason);
        return row;
    }

    private ProjectCopySelection readSelection(String json) {
        try {
            ProjectCopySelection selection = JSON.readValue(json, ProjectCopySelection.class);
            if (selection != null) {
                selection.normalize();
                return selection;
            }
        } catch (IOException ignored) {
            // 落到空勾选，任务会失败并留下原因
        }
        return new ProjectCopySelection();
    }

    private List<Map<String, Object>> readItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(json, JSON.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (IOException e) {
            return List.of();
        }
    }

    private static String writeJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (IOException e) {
            return "[]";
        }
    }

    private static String trim(String text, int max) {
        if (text == null || text.isBlank()) {
            return "复制失败";
        }
        String oneLine = text.replace('\n', ' ').trim();
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max);
    }
}
