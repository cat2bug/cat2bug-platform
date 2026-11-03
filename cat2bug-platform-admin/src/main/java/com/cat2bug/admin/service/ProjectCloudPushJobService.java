package com.cat2bug.admin.service;

import com.cat2bug.common.domain.SysCloudPushJob;
import com.cat2bug.common.domain.SysCloudPushPart;
import com.cat2bug.common.domain.vo.PullProject;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 出站同步任务：内存执行 + 数据库进度。刷新轮询同一 jobId；进程重启后补打包丢失的临时文件再上传。
 */
@ApplicationScoped
public class ProjectCloudPushJobService {

    private static final Logger LOG = Logger.getLogger(ProjectCloudPushJobService.class);

    public static final String STATE_RUNNING = "RUNNING";
    public static final String STATE_SUCCESS = "SUCCESS";
    public static final String STATE_FAILED = "FAILED";

    private static final int MAX_LOGS = 400;
    private static final long JOB_TTL_MS = TimeUnit.HOURS.toMillis(2);
    private static final TypeReference<List<Map<String, Object>>> LOG_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() {
    };

    private final ConcurrentHashMap<String, PushJob> jobs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> runningByProject = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Object> projectLocks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "project-cloud-push");
        thread.setDaemon(true);
        return thread;
    });

    @Inject
    ProjectCloudPushService projectCloudPushService;

    @Inject
    CloudPushMultipartClient multipartClient;

    @Inject
    CloudPushJobStore store;

    @ConfigProperty(name = "cat2bug.temp", defaultValue = "./uploadTemp")
    String tempPath;

    void onStart(@Observes StartupEvent event) {
        try {
            for (SysCloudPushJob row : store.findRunning()) {
                PushJob job = hydrate(row);
                ensureRunning(job);
            }
        } catch (RuntimeException e) {
            LOG.warn("恢复出站同步任务失败", e);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public Map<String, Object> start(Long projectId, PullProject request) {
        if (projectId == null) {
            throw new ApiException("项目ID不能为空");
        }
        if (request == null || StringUtils.isBlank(request.getPullKey())) {
            throw new ApiException("同步密钥不能为空");
        }
        String pullKey = request.getPullKey().trim();
        if (pullKey.length() > 1024) {
            throw new ApiException("同步密钥过长");
        }
        synchronized (lock(projectId)) {
            PushJob running = findRunning(projectId);
            if (running != null) {
                Map<String, Object> body = snapshot(running);
                body.put("alreadyRunning", true);
                return body;
            }
            CloudPushPlan plan = projectCloudPushService.prepare(projectId, request.getGitSlugs(), request.getDocumentIds());
            if (plan.isOverLimit()) {
                throw new ApiException(400, projectCloudPushService.overLimitMessage(
                        plan.getEstimatedBytes(), plan.getMaxTotalBytes()));
            }
            String jobId = UUID.randomUUID().toString().replace("-", "");
            PushJob job = new PushJob(jobId, projectId);
            job.pullKey = pullKey;
            job.maxTotalBytes = plan.getMaxTotalBytes();
            job.legacyFull = plan.isLegacyFull();
            job.gitSlugs = new ArrayList<>(plan.getGitSlugs());
            job.documentIds = new ArrayList<>(plan.getDocumentIds());
            job.sessionId = UUID.randomUUID().toString().replace("-", "");
            job.phase = "准备";
            int sortSeen = 0;
            for (CloudPushPlan.Part planned : plan.getParts()) {
                PartRuntime part = new PartRuntime();
                part.partId = planned.getPartId();
                part.type = planned.getType();
                part.refKey = planned.getRefKey();
                part.sortOrder = planned.getSortOrder();
                part.expectedBytes = planned.getEstimatedBytes();
                part.state = CloudPushSupport.PART_PENDING;
                job.parts.add(part);
                sortSeen++;
            }
            if (sortSeen == 0) {
                throw new ApiException("没有可同步的内容");
            }
            job.log("INFO", "开始同步项目 " + projectId + "，分包 " + job.parts.size() + " 个");
            jobs.put(jobId, job);
            runningByProject.put(projectId, jobId);
            job.busy = true;
            persist(job, true);
            executor.execute(() -> execute(job, null));
            return snapshot(job);
        }
    }

    public Map<String, Object> status(String jobId) {
        if (StringUtils.isBlank(jobId)) {
            throw new ApiException("同步任务不存在或已过期");
        }
        PushJob job = jobs.get(jobId);
        if (job == null) {
            SysCloudPushJob row = store.findJob(jobId);
            if (row == null) {
                throw new ApiException("同步任务不存在或已过期");
            }
            job = hydrate(row);
        }
        ensureRunning(job);
        return snapshot(job);
    }

    public Map<String, Object> current(Long projectId) {
        if (projectId == null) {
            throw new ApiException("项目ID不能为空");
        }
        PushJob job = findOpen(projectId);
        if (job == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("projectId", projectId);
            empty.put("jobId", null);
            empty.put("state", null);
            return empty;
        }
        ensureRunning(job);
        return snapshot(job);
    }

    public Map<String, Object> retryFailedById(String jobId) {
        if (StringUtils.isBlank(jobId)) {
            throw new ApiException("同步任务不存在或已过期");
        }
        PushJob cached = jobs.get(jobId);
        if (cached != null) {
            return retryFailed(cached.projectId, jobId);
        }
        SysCloudPushJob row = store.findJob(jobId);
        if (row == null || row.getProjectId() == null) {
            throw new ApiException("同步任务不存在或已过期");
        }
        return retryFailed(row.getProjectId(), jobId);
    }

    public Map<String, Object> retryFailed(Long projectId, String jobId) {
        if (projectId == null || StringUtils.isBlank(jobId)) {
            throw new ApiException("同步任务不存在或已过期");
        }
        synchronized (lock(projectId)) {
            PushJob job = jobs.get(jobId);
            if (job == null) {
                SysCloudPushJob row = store.findJob(jobId);
                if (row == null) {
                    throw new ApiException("同步任务不存在或已过期");
                }
                job = hydrate(row);
            }
            if (!projectId.equals(job.projectId)) {
                throw new ApiException("同步任务与项目不匹配");
            }
            if (job.busy || STATE_RUNNING.equals(job.state)) {
                throw new ApiException("同步仍在进行，请稍后再试");
            }
            if (sizeBlocked(job)) {
                throw new ApiException(400, "同步包超过上限，请减少勾选后重新发起同步");
            }
            Set<String> failedIds = new LinkedHashSet<>();
            synchronized (job) {
                for (PartRuntime part : job.parts) {
                    if (CloudPushSupport.PART_FAILED.equals(part.state)) {
                        failedIds.add(part.partId);
                        part.state = CloudPushSupport.PART_PENDING;
                        part.attempts = 0;
                        part.error = null;
                    }
                }
                if (failedIds.isEmpty()) {
                    throw new ApiException("没有失败的分包需要重试");
                }
                job.state = STATE_RUNNING;
                job.phase = "重试失败分包";
                job.error = null;
                job.message = null;
                job.busy = true;
                job.log("INFO", "重试失败分包 " + failedIds.size() + " 个");
            }
            runningByProject.put(projectId, job.jobId);
            persist(job, false);
            final PushJob retryJob = job;
            executor.execute(() -> execute(retryJob, failedIds));
            return snapshot(retryJob);
        }
    }

    private void ensureRunning(PushJob job) {
        if (job == null || job.busy || !STATE_RUNNING.equals(job.state)) {
            return;
        }
        synchronized (lock(job.projectId)) {
            if (job.busy || !STATE_RUNNING.equals(job.state)) {
                return;
            }
            job.busy = true;
            runningByProject.put(job.projectId, job.jobId);
            job.log("INFO", "继续未完成的同步任务");
            executor.execute(() -> execute(job, null));
        }
    }

    private void execute(PushJob job, Set<String> onlyPartIds) {
        try {
            List<PartRuntime> todo = new ArrayList<>();
            synchronized (job) {
                for (PartRuntime part : job.parts) {
                    if (onlyPartIds != null) {
                        if (onlyPartIds.contains(part.partId)) {
                            todo.add(part);
                        }
                    } else if (CloudPushSupport.shouldUpload(part.state, false)) {
                        todo.add(part);
                    }
                }
            }
            if (todo.isEmpty()) {
                finish(job);
                return;
            }
            job.phase(8, "打包分包", "准备 " + todo.size() + " 个分包");
            if (!ensurePacked(job, todo)) {
                return;
            }
            Map<String, Object> manifest = buildManifest(job);
            Map<String, Object> include = includeOf(job);
            try {
                CloudPushMultipartClient.SessionOpen opened = multipartClient.openSession(
                        job.pullKey, job.sessionId, manifest, include);
                synchronized (job) {
                    job.sessionId = opened.sessionId;
                    job.legacyReceiveUrl = opened.legacyReceiveUrl;
                }
                job.log("INFO", opened.legacyReceiveUrl
                        ? "云站无会话接口，改为单包上传（入站只显示一条）"
                        : ("云站会话 " + opened.sessionId));
                persist(job, false);
            } catch (IOException e) {
                String message = e.getMessage() == null ? "创建云站会话失败" : e.getMessage();
                synchronized (job) {
                    for (PartRuntime part : job.parts) {
                        if (!CloudPushSupport.PART_OK.equals(part.state)) {
                            part.state = CloudPushSupport.PART_FAILED;
                            if (part.error == null) {
                                part.error = CloudPushSupport.truncate(message, 1800);
                            }
                        }
                    }
                }
                job.completeFailed(message);
                return;
            }
            if (job.legacyReceiveUrl) {
                uploadMergedLegacy(job, todo);
                finish(job);
                return;
            }
            String manifestJson = CloudPushSupport.JSON.writeValueAsString(buildManifest(job));
            int partCount = job.parts.size();
            for (PartRuntime part : todo) {
                if (CloudPushSupport.PART_FAILED.equals(part.state) || CloudPushSupport.PART_OK.equals(part.state)) {
                    continue;
                }
                if (!hasFile(part)) {
                    markPartFailed(job, part, "分包文件不存在，无法上传");
                    continue;
                }
                uploadWithRetry(job, part, partCount, manifestJson);
            }
            finish(job);
        } catch (Exception e) {
            job.completeFailed(formatException(e));
        } finally {
            job.busy = false;
            if (!STATE_RUNNING.equals(job.state)) {
                runningByProject.remove(job.projectId, job.jobId);
            }
            persist(job, false);
            purgeExpired();
        }
    }

    private boolean ensurePacked(PushJob job, List<PartRuntime> todo) {
        List<PartRuntime> missing = new ArrayList<>();
        for (PartRuntime part : todo) {
            if (hasFile(part)) {
                fillHash(part);
            }
            if (!hasFile(part)) {
                missing.add(part);
            }
        }
        List<PartRuntime> ordered = new ArrayList<>();
        for (PartRuntime part : missing) {
            if (!CloudPushSupport.TYPE_CORE.equals(part.type)) {
                ordered.add(part);
            }
        }
        for (PartRuntime part : missing) {
            if (CloudPushSupport.TYPE_CORE.equals(part.type)) {
                ordered.add(part);
            }
        }
        int index = 0;
        for (PartRuntime part : ordered) {
            index++;
            int percent = 8 + (int) (32L * index / Math.max(1, ordered.size()));
            job.phase(percent, "打包分包", "正在打包 " + part.partId);
            try {
                packOne(job, part);
            } catch (SizeLimitException e) {
                markPartFailed(job, part, e.getMessage());
                job.completeFailed(e.getMessage());
                return false;
            } catch (Exception e) {
                markPartFailed(job, part, formatException(e));
            }
        }
        long packed = 0;
        synchronized (job) {
            for (PartRuntime part : job.parts) {
                if (part.actualBytes > 0) {
                    packed += part.actualBytes;
                }
            }
            job.packedBytes = packed;
        }
        persist(job, false);
        return true;
    }

    private void packOne(PushJob job, PartRuntime part) throws IOException {
        synchronized (job) {
            part.state = CloudPushSupport.PART_PACKING;
            part.error = null;
        }
        persist(job, false);
        CloudPushPlan plan = planOf(job);
        CloudPushPlan.Part spec = new CloudPushPlan.Part();
        spec.setPartId(part.partId);
        spec.setType(part.type);
        spec.setRefKey(part.refKey);
        spec.setSortOrder(part.sortOrder);
        spec.setEstimatedBytes(part.expectedBytes);
        List<Map<String, Object>> embedded = CloudPushSupport.TYPE_CORE.equals(part.type)
                ? partRows(job, true)
                : null;
        Path dir = Path.of(tempPath, "cloud-push", job.jobId);
        Path file = projectCloudPushService.packPartFile(job.projectId, job.pullKey, spec, dir, plan, embedded);
        long size = Files.size(file);
        long others = sumOtherBytes(job, part);
        if (CloudPushSupport.wouldExceed(others, size, job.maxTotalBytes)) {
            Files.deleteIfExists(file);
            throw new SizeLimitException("打包后合计 " + CloudPushSupport.formatSize(others + size)
                    + " 将" + CloudPushSupport.SIZE_LIMIT_MARK + " "
                    + CloudPushSupport.formatSize(job.maxTotalBytes) + "，已停止后续打包");
        }
        synchronized (job) {
            part.tempPath = file.toString();
            part.actualBytes = size;
            part.sha256 = CloudPushSupport.sha256(file);
            part.state = CloudPushSupport.PART_PENDING;
            job.packedBytes = others + size;
        }
        job.log("INFO", part.partId + " 打包完成 " + CloudPushSupport.formatSize(size));
        persist(job, false);
    }

    /** 旧云站：把已打好的 part 合成一个 zip，一次上传，避免入站列表出现多条。 */
    private void uploadMergedLegacy(PushJob job, List<PartRuntime> todo) {
        List<PartRuntime> ordered = new ArrayList<>();
        for (PartRuntime part : todo) {
            if (CloudPushSupport.TYPE_CORE.equals(part.type)) {
                ordered.add(part);
            }
        }
        for (PartRuntime part : todo) {
            if (!CloudPushSupport.TYPE_CORE.equals(part.type)) {
                ordered.add(part);
            }
        }
        for (PartRuntime part : ordered) {
            if (!hasFile(part)) {
                markPartFailed(job, part, "分包文件不存在，无法合并上传");
                job.completeFailed("分包文件不存在，无法合并上传");
                return;
            }
        }
        Path dir = Path.of(tempPath, "cloud-push", job.jobId);
        Path combined = dir.resolve("legacy-merged.zip");
        job.phase(50, "上传到云站", "合并 " + ordered.size() + " 个分包为单包");
        try {
            Files.createDirectories(dir);
            mergePartZips(ordered, combined);
            long size = Files.size(combined);
            job.phase(70, "上传到云站", "上传合并包 " + CloudPushSupport.formatSize(size));
            persist(job, false);
            multipartClient.uploadLegacyZip(job.pullKey, combined);
            synchronized (job) {
                for (PartRuntime part : ordered) {
                    part.state = CloudPushSupport.PART_OK;
                    part.error = null;
                }
            }
            job.log("INFO", "单包上传成功 " + CloudPushSupport.formatSize(size));
            persist(job, false);
        } catch (Exception e) {
            String message = formatException(e);
            synchronized (job) {
                for (PartRuntime part : ordered) {
                    if (!CloudPushSupport.PART_OK.equals(part.state)) {
                        part.state = CloudPushSupport.PART_FAILED;
                        part.error = CloudPushSupport.truncate(message, 1800);
                    }
                }
            }
            job.completeFailed(message);
        } finally {
            try {
                Files.deleteIfExists(combined);
            } catch (IOException ignored) {
                // 临时合并包清理失败不影响结果
            }
        }
    }

    private static void mergePartZips(List<PartRuntime> parts, Path target) throws IOException {
        Files.deleteIfExists(target);
        Set<String> seen = new HashSet<>();
        try (OutputStream raw = Files.newOutputStream(target);
             ZipOutputStream out = new ZipOutputStream(raw)) {
            byte[] buffer = new byte[8192];
            for (PartRuntime part : parts) {
                try (InputStream in = Files.newInputStream(Path.of(part.tempPath));
                     ZipInputStream zip = new ZipInputStream(in)) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        String name = entry.getName();
                        if (entry.isDirectory() || name == null || name.isBlank() || !seen.add(name)) {
                            zip.closeEntry();
                            continue;
                        }
                        out.putNextEntry(new ZipEntry(name));
                        int n;
                        while ((n = zip.read(buffer)) > 0) {
                            out.write(buffer, 0, n);
                        }
                        out.closeEntry();
                        zip.closeEntry();
                    }
                }
            }
        }
    }

    private void uploadWithRetry(PushJob job, PartRuntime part, int partCount, String manifestJson) {
        int start = Math.max(1, part.attempts + 1);
        if (start > CloudPushSupport.MAX_UPLOAD_ATTEMPTS) {
            markPartFailed(job, part, "上传重试次数已用完");
            return;
        }
        for (int attempt = start; attempt <= CloudPushSupport.MAX_UPLOAD_ATTEMPTS; attempt++) {
            synchronized (job) {
                part.state = CloudPushSupport.PART_UPLOADING;
                part.attempts = attempt;
            }
            int okCount = count(job, CloudPushSupport.PART_OK);
            int percent = 40 + (int) (55L * okCount / Math.max(1, partCount));
            job.phase(percent, "上传到云站", "上传 " + part.partId + "（第 " + attempt + " 次）");
            persist(job, false);
            try {
                String adopted = multipartClient.uploadPart(
                        job.pullKey,
                        job.sessionId,
                        part.partId,
                        part.type,
                        part.sha256,
                        part.sortOrder + 1,
                        partCount,
                        manifestJson,
                        Path.of(part.tempPath));
                synchronized (job) {
                    if (StringUtils.isNotBlank(adopted)) {
                        job.sessionId = adopted;
                    }
                    part.state = CloudPushSupport.PART_OK;
                    part.error = null;
                }
                job.log("INFO", part.partId + " 上传成功");
                persist(job, false);
                return;
            } catch (Exception e) {
                String message = formatException(e);
                synchronized (job) {
                    part.attempts = attempt;
                    part.error = CloudPushSupport.truncate(message, 1800);
                }
                if (attempt >= CloudPushSupport.MAX_UPLOAD_ATTEMPTS) {
                    markPartFailed(job, part, message);
                    return;
                }
                long wait = CloudPushSupport.backoffMillis(attempt);
                job.log("WARN", part.partId + " 上传失败，" + (wait / 1000) + " 秒后重试：" + message);
                persist(job, false);
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    markPartFailed(job, part, "上传被中断");
                    return;
                }
            }
        }
    }

    private void finish(PushJob job) {
        boolean allOk;
        synchronized (job) {
            allOk = !job.parts.isEmpty() && job.parts.stream()
                    .allMatch(part -> CloudPushSupport.PART_OK.equals(part.state));
        }
        if (allOk) {
            deleteTemp(job);
            job.completeSuccess("已推送到云站收件箱");
        } else if (!STATE_FAILED.equals(job.state)) {
            job.completeFailed("部分内容上传失败，可重试失败项");
        }
    }

    private void deleteTemp(PushJob job) {
        synchronized (job) {
            for (PartRuntime part : job.parts) {
                if (StringUtils.isNotBlank(part.tempPath)) {
                    try {
                        Files.deleteIfExists(Path.of(part.tempPath));
                    } catch (IOException e) {
                        LOG.warnf("删除临时分包失败: %s", part.tempPath);
                    }
                    part.tempPath = null;
                }
            }
        }
        try {
            Files.deleteIfExists(Path.of(tempPath, "cloud-push", job.jobId));
        } catch (IOException ignored) {
            // 目录非空时保留
        }
    }

    private void markPartFailed(PushJob job, PartRuntime part, String message) {
        synchronized (job) {
            part.state = CloudPushSupport.PART_FAILED;
            part.error = CloudPushSupport.truncate(message, 1800);
        }
        job.log("ERROR", part.partId + " 失败：" + message);
        persist(job, false);
    }

    private static boolean hasFile(PartRuntime part) {
        if (StringUtils.isBlank(part.tempPath)) {
            return false;
        }
        Path path = Path.of(part.tempPath);
        try {
            return Files.isRegularFile(path) && Files.size(path) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static void fillHash(PartRuntime part) {
        try {
            Path path = Path.of(part.tempPath);
            if (part.actualBytes <= 0) {
                part.actualBytes = Files.size(path);
            }
            if (StringUtils.isBlank(part.sha256)) {
                part.sha256 = CloudPushSupport.sha256(path);
            }
        } catch (IOException e) {
            part.tempPath = null;
        }
    }

    private static long sumOtherBytes(PushJob job, PartRuntime current) {
        long total = 0;
        synchronized (job) {
            for (PartRuntime part : job.parts) {
                if (part == current || part.actualBytes <= 0) {
                    continue;
                }
                total += part.actualBytes;
            }
        }
        return total;
    }

    private static int count(PushJob job, String state) {
        int count = 0;
        synchronized (job) {
            for (PartRuntime part : job.parts) {
                if (state.equals(part.state)) {
                    count++;
                }
            }
        }
        return count;
    }

    private Map<String, Object> buildManifest(PushJob job) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("packageSchema", "1.2");
        manifest.put("include", includeOf(job));
        manifest.put("parts", partRows(job, false));
        return manifest;
    }

    private List<Map<String, Object>> partRows(PushJob job, boolean blankCoreHash) {
        List<Map<String, Object>> rows = new ArrayList<>();
        synchronized (job) {
            for (PartRuntime part : job.parts) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("partId", part.partId);
                row.put("type", part.type);
                long bytes = part.actualBytes > 0 ? part.actualBytes : part.expectedBytes;
                row.put("expectedBytes", bytes);
                String hash = part.sha256;
                if (blankCoreHash && CloudPushSupport.TYPE_CORE.equals(part.type)) {
                    hash = null;
                }
                row.put("sha256", hash);
                if (CloudPushSupport.TYPE_DOC.equals(part.type) && StringUtils.isNotBlank(part.refKey)) {
                    row.put("docId", Long.valueOf(part.refKey));
                }
                if (CloudPushSupport.TYPE_GIT.equals(part.type)) {
                    row.put("slug", part.refKey);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private static Map<String, Object> includeOf(PushJob job) {
        Map<String, Object> include = new LinkedHashMap<>();
        include.put("required", List.of("defects", "cases", "plans", "deliverables"));
        include.put("silent", List.of("members", "teamConfig", "workCalendar", "mcp"));
        include.put("gitSlugs", job.gitSlugs);
        include.put("documentIds", job.documentIds);
        include.put("legacyFull", job.legacyFull);
        return include;
    }

    private static CloudPushPlan planOf(PushJob job) {
        CloudPushPlan plan = new CloudPushPlan();
        plan.setLegacyFull(job.legacyFull);
        plan.setGitSlugs(job.gitSlugs);
        plan.setDocumentIds(job.documentIds);
        plan.setMaxTotalBytes(job.maxTotalBytes);
        return plan;
    }

    private PushJob findRunning(Long projectId) {
        String jobId = runningByProject.get(projectId);
        if (jobId != null) {
            PushJob job = jobs.get(jobId);
            if (job != null && STATE_RUNNING.equals(job.state)) {
                if (!job.busy) {
                    job.busy = true;
                    executor.execute(() -> execute(job, null));
                }
                return job;
            }
        }
        SysCloudPushJob row = store.findOpenByProject(projectId);
        if (row != null && STATE_RUNNING.equals(row.getState())) {
            PushJob job = hydrate(row);
            if (STATE_RUNNING.equals(job.state)) {
                if (!job.busy) {
                    job.busy = true;
                    runningByProject.put(projectId, job.jobId);
                    executor.execute(() -> execute(job, null));
                }
                return job;
            }
        }
        return null;
    }

    private PushJob findOpen(Long projectId) {
        String jobId = runningByProject.get(projectId);
        if (jobId != null) {
            PushJob running = jobs.get(jobId);
            if (running != null && STATE_RUNNING.equals(running.state)) {
                return running;
            }
        }
        PushJob failed = null;
        for (PushJob job : jobs.values()) {
            if (!projectId.equals(job.projectId) || !STATE_FAILED.equals(job.state)) {
                continue;
            }
            if (failed == null || job.updatedAtMs > failed.updatedAtMs) {
                failed = job;
            }
        }
        SysCloudPushJob row = store.findOpenByProject(projectId);
        if (row == null) {
            return failed;
        }
        if (STATE_RUNNING.equals(row.getState()) || failed == null) {
            return hydrate(row);
        }
        return failed;
    }

    private PushJob hydrate(SysCloudPushJob row) {
        synchronized (jobs) {
            PushJob existing = jobs.get(row.getJobId());
            if (existing != null) {
                return existing;
            }
            PushJob job = new PushJob(row.getJobId(), row.getProjectId());
            job.pullKey = row.getPullKey();
            job.state = row.getState() == null ? STATE_FAILED : row.getState();
            job.percent = row.getPercent() == null ? 0 : row.getPercent();
            job.phase = row.getPhase();
            job.error = row.getErrorMessage();
            job.message = row.getMessage();
            job.sessionId = row.getSessionId();
            job.packedBytes = row.getPackedBytes() == null ? 0 : row.getPackedBytes();
            job.maxTotalBytes = row.getMaxTotalBytes() == null ? projectCloudPushService.maxTotalBytes() : row.getMaxTotalBytes();
            job.legacyFull = row.getLegacyFull() != null && row.getLegacyFull() == 1;
            job.persisted = true;
            job.updatedAtMs = row.getUpdateTime() == null ? System.currentTimeMillis() : row.getUpdateTime().getTime();
            readSelection(job, row.getSelectionJson());
            readLogs(job, row.getLogsJson());
            for (SysCloudPushPart rowPart : store.findParts(row.getJobId())) {
                PartRuntime part = new PartRuntime();
                part.partId = rowPart.getPartId();
                part.type = rowPart.getPartType();
                part.refKey = rowPart.getRefKey();
                part.state = rowPart.getState();
                part.expectedBytes = rowPart.getExpectedBytes() == null ? 0 : rowPart.getExpectedBytes();
                part.actualBytes = rowPart.getActualBytes() == null ? 0 : rowPart.getActualBytes();
                part.sha256 = rowPart.getSha256();
                part.tempPath = rowPart.getTempPath();
                part.attempts = rowPart.getAttemptCount() == null ? 0 : rowPart.getAttemptCount();
                part.error = rowPart.getErrorMessage();
                part.sortOrder = rowPart.getSortOrder() == null ? 0 : rowPart.getSortOrder();
                job.parts.add(part);
            }
            jobs.put(job.jobId, job);
            if (STATE_RUNNING.equals(job.state)) {
                runningByProject.put(job.projectId, job.jobId);
            }
            return job;
        }
    }

    private void readSelection(PushJob job, String json) {
        job.gitSlugs = new ArrayList<>();
        job.documentIds = new ArrayList<>();
        if (StringUtils.isBlank(json)) {
            return;
        }
        try {
            Map<String, Object> selection = CloudPushSupport.JSON.readValue(json, MAP_TYPE);
            Object legacy = selection.get("legacyFull");
            job.legacyFull = legacy instanceof Boolean flag && flag;
            if (selection.get("gitSlugs") != null) {
                job.gitSlugs = CloudPushSupport.JSON.convertValue(selection.get("gitSlugs"), STRING_LIST);
            }
            if (selection.get("documentIds") != null) {
                job.documentIds = CloudPushSupport.JSON.convertValue(selection.get("documentIds"), LONG_LIST);
            }
        } catch (Exception e) {
            LOG.warn("读取同步勾选失败", e);
        }
    }

    private void readLogs(PushJob job, String json) {
        if (StringUtils.isBlank(json)) {
            return;
        }
        try {
            List<Map<String, Object>> logs = CloudPushSupport.JSON.readValue(json, LOG_TYPE);
            if (logs != null) {
                job.logs.addAll(logs);
            }
        } catch (Exception e) {
            LOG.warn("读取同步日志失败", e);
        }
    }

    private void persist(PushJob job, boolean insert) {
        SysCloudPushJob row;
        List<SysCloudPushPart> parts;
        synchronized (job) {
            job.updatedAtMs = System.currentTimeMillis();
            row = toJobRow(job);
            parts = toPartRows(job);
            if (insert) {
                job.persisted = true;
            }
        }
        try {
            if (insert) {
                store.insertJobAndParts(row, parts);
            } else {
                store.updateJob(row);
                for (SysCloudPushPart part : parts) {
                    store.updatePart(part);
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("保存出站同步任务失败", e);
            if (insert) {
                job.persisted = false;
            }
        }
    }

    private static SysCloudPushJob toJobRow(PushJob job) {
        SysCloudPushJob row = new SysCloudPushJob();
        row.setJobId(job.jobId);
        row.setProjectId(job.projectId);
        row.setPullKey(job.pullKey);
        row.setState(job.state);
        row.setPercent(job.percent);
        row.setPhase(job.phase);
        row.setErrorMessage(job.error);
        row.setMessage(job.message);
        row.setSessionId(job.sessionId);
        row.setPackedBytes(job.packedBytes);
        row.setMaxTotalBytes(job.maxTotalBytes);
        row.setLegacyFull(job.legacyFull ? 1 : 0);
        Date now = new Date();
        row.setCreateTime(now);
        row.setUpdateTime(now);
        Map<String, Object> selection = new LinkedHashMap<>();
        selection.put("legacyFull", job.legacyFull);
        selection.put("gitSlugs", job.gitSlugs);
        selection.put("documentIds", job.documentIds);
        try {
            row.setSelectionJson(CloudPushSupport.JSON.writeValueAsString(selection));
            row.setLogsJson(CloudPushSupport.JSON.writeValueAsString(job.logs));
        } catch (Exception e) {
            row.setSelectionJson("{}");
            row.setLogsJson("[]");
        }
        return row;
    }

    private static List<SysCloudPushPart> toPartRows(PushJob job) {
        List<SysCloudPushPart> rows = new ArrayList<>();
        Date now = new Date();
        for (PartRuntime part : job.parts) {
            SysCloudPushPart row = new SysCloudPushPart();
            row.setJobId(job.jobId);
            row.setPartId(part.partId);
            row.setPartType(part.type);
            row.setRefKey(part.refKey);
            row.setState(part.state);
            row.setExpectedBytes(part.expectedBytes);
            row.setActualBytes(part.actualBytes);
            row.setSha256(part.sha256);
            row.setTempPath(part.tempPath);
            row.setAttemptCount(part.attempts);
            row.setErrorMessage(part.error);
            row.setSortOrder(part.sortOrder);
            row.setUpdateTime(now);
            rows.add(row);
        }
        return rows;
    }

    private static boolean sizeBlocked(PushJob job) {
        String error = job.error == null ? "" : job.error;
        String message = job.message == null ? "" : job.message;
        return error.contains(CloudPushSupport.SIZE_LIMIT_MARK) || message.contains(CloudPushSupport.SIZE_LIMIT_MARK);
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        jobs.entrySet().removeIf(entry -> {
            PushJob job = entry.getValue();
            return !STATE_RUNNING.equals(job.state) && !job.busy && now - job.updatedAtMs > JOB_TTL_MS;
        });
    }

    private Object lock(Long projectId) {
        return projectLocks.computeIfAbsent(projectId, id -> new Object());
    }

    private static Map<String, Object> snapshot(PushJob job) {
        synchronized (job) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("jobId", job.jobId);
            body.put("projectId", job.projectId);
            body.put("state", job.state);
            body.put("percent", job.percent);
            body.put("phase", job.phase);
            body.put("error", job.error);
            body.put("message", job.message);
            body.put("sessionId", job.sessionId);
            body.put("maxTotalBytes", job.maxTotalBytes);
            body.put("packedBytes", job.packedBytes);
            Map<String, Object> selection = new LinkedHashMap<>();
            selection.put("legacyFull", job.legacyFull);
            selection.put("gitSlugs", job.gitSlugs);
            selection.put("documentIds", job.documentIds);
            body.put("selection", selection);
            List<Map<String, Object>> parts = new ArrayList<>();
            for (PartRuntime part : job.parts) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("partId", part.partId);
                row.put("type", part.type);
                row.put("refKey", part.refKey);
                row.put("state", part.state);
                row.put("bytes", part.actualBytes > 0 ? part.actualBytes : part.expectedBytes);
                row.put("expectedBytes", part.expectedBytes);
                row.put("actualBytes", part.actualBytes);
                row.put("sha256", part.sha256);
                row.put("attempts", part.attempts);
                row.put("error", part.error);
                row.put("sortOrder", part.sortOrder);
                parts.add(row);
            }
            body.put("parts", parts);
            body.put("logs", List.copyOf(job.logs));
            return body;
        }
    }

    static String formatException(Throwable error) {
        if (error == null) {
            return "未知错误";
        }
        StringBuilder sb = new StringBuilder();
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 6) {
            String msg = current.getMessage();
            if (msg != null) {
                msg = msg.trim();
            }
            String piece = (msg != null && !msg.isEmpty()) ? msg : current.getClass().getSimpleName();
            if (sb.length() == 0) {
                sb.append(piece);
            } else if (!sb.toString().contains(piece)) {
                sb.append(" ← ").append(piece);
            }
            current = current.getCause();
            depth++;
        }
        return sb.length() == 0 ? error.getClass().getSimpleName() : sb.toString();
    }

    private static final class SizeLimitException extends IOException {
        SizeLimitException(String message) {
            super(message);
        }
    }

    static final class PartRuntime {
        String partId;
        String type;
        String refKey;
        String state;
        long expectedBytes;
        long actualBytes;
        String sha256;
        String tempPath;
        int attempts;
        String error;
        int sortOrder;
    }

    static final class PushJob {
        final String jobId;
        final Long projectId;
        String pullKey;
        volatile String state = STATE_RUNNING;
        volatile int percent;
        volatile String phase = "准备";
        volatile String error;
        volatile String message;
        volatile String sessionId;
        volatile long packedBytes;
        volatile long maxTotalBytes;
        volatile boolean legacyFull;
        volatile boolean legacyReceiveUrl;
        volatile boolean busy;
        volatile boolean persisted;
        volatile long updatedAtMs = System.currentTimeMillis();
        List<String> gitSlugs = new ArrayList<>();
        List<Long> documentIds = new ArrayList<>();
        final List<PartRuntime> parts = new ArrayList<>();
        final List<Map<String, Object>> logs = new ArrayList<>();

        PushJob(String jobId, Long projectId) {
            this.jobId = jobId;
            this.projectId = projectId;
        }

        void phase(int percent, String phase, String detail) {
            synchronized (this) {
                this.percent = Math.max(0, Math.min(100, percent));
                if (phase != null && !phase.isBlank()) {
                    this.phase = phase;
                }
                this.updatedAtMs = System.currentTimeMillis();
            }
            if (detail != null && !detail.isBlank()) {
                log("INFO", detail);
            }
        }

        void log(String level, String message) {
            if (message == null || message.isBlank()) {
                return;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("time", Instant.now().toString());
            row.put("level", level == null ? "INFO" : level);
            row.put("message", message);
            synchronized (this) {
                logs.add(row);
                while (logs.size() > MAX_LOGS) {
                    logs.remove(0);
                }
                this.updatedAtMs = System.currentTimeMillis();
            }
        }

        void completeSuccess(String message) {
            synchronized (this) {
                this.percent = 100;
                this.phase = "完成";
                this.message = message;
                this.error = null;
                this.state = STATE_SUCCESS;
                this.updatedAtMs = System.currentTimeMillis();
            }
            log("INFO", message);
        }

        void completeFailed(String error) {
            synchronized (this) {
                this.state = STATE_FAILED;
                this.error = error;
                this.message = error;
                this.phase = "失败";
                this.updatedAtMs = System.currentTimeMillis();
            }
            log("ERROR", error);
        }
    }
}
