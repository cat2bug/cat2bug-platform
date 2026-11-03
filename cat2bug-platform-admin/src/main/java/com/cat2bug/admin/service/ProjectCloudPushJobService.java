package com.cat2bug.admin.service;

import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 出站同步任务：异步执行并提供进度 / 日志供前端轮询。
 */
@ApplicationScoped
public class ProjectCloudPushJobService {

    public static final String STATE_RUNNING = "RUNNING";
    public static final String STATE_SUCCESS = "SUCCESS";
    public static final String STATE_FAILED = "FAILED";

    private static final int MAX_LOGS = 400;
    private static final long JOB_TTL_MS = TimeUnit.HOURS.toMillis(2);

    private final ConcurrentHashMap<String, PushJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "project-cloud-push");
        t.setDaemon(true);
        return t;
    });

    @Inject
    ProjectCloudPushService projectCloudPushService;

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public Map<String, Object> start(Long projectId, String pullKey) {
        if (projectId == null) {
            throw new ApiException("项目ID不能为空");
        }
        if (StringUtils.isBlank(pullKey)) {
            throw new ApiException("同步密钥不能为空");
        }
        String jobId = UUID.randomUUID().toString().replace("-", "");
        PushJob job = new PushJob(jobId, projectId);
        jobs.put(jobId, job);
        executor.execute(() -> runJob(job, pullKey.trim()));
        return snapshot(job);
    }

    public Map<String, Object> status(String jobId) {
        PushJob job = jobs.get(jobId);
        if (job == null) {
            throw new ApiException("同步任务不存在或已过期");
        }
        return snapshot(job);
    }

    private void runJob(PushJob job, String pullKey) {
        try {
            job.log("INFO", "开始同步项目 " + job.projectId);
            boolean ok = projectCloudPushService.pushToCloud(job.projectId, pullKey, job);
            if (ok) {
                job.completeSuccess("已推送到云站收件箱");
            } else {
                job.completeFailed("云站未接受同步包（请检查同步密钥与云站地址）");
            }
        } catch (Exception e) {
            job.completeFailed(formatException(e));
        } finally {
            purgeExpired();
        }
    }

    /** 拼出可读失败原因（含 cause），避免只显示空的 “IOException”。 */
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
            String piece = (msg != null && !msg.isEmpty())
                    ? msg
                    : current.getClass().getSimpleName();
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

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        jobs.entrySet().removeIf(e -> {
            PushJob j = e.getValue();
            return !STATE_RUNNING.equals(j.state) && now - j.updatedAtMs > JOB_TTL_MS;
        });
    }

    private static Map<String, Object> snapshot(PushJob job) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", job.jobId);
        body.put("projectId", job.projectId);
        body.put("state", job.state);
        body.put("percent", job.percent);
        body.put("phase", job.phase);
        body.put("error", job.error);
        body.put("message", job.message);
        synchronized (job.logs) {
            body.put("logs", List.copyOf(job.logs));
        }
        return body;
    }

    /** 推送过程回调，供 {@link ProjectCloudPushService} 写入进度。 */
    public static final class PushJob implements ProjectCloudPushService.PushProgress {
        private final String jobId;
        private final Long projectId;
        private volatile String state = STATE_RUNNING;
        private volatile int percent;
        private volatile String phase = "准备";
        private volatile String error;
        private volatile String message;
        private volatile long updatedAtMs = System.currentTimeMillis();
        private final List<Map<String, Object>> logs = new ArrayList<>();

        PushJob(String jobId, Long projectId) {
            this.jobId = jobId;
            this.projectId = projectId;
        }

        @Override
        public void phase(int percent, String phase, String detail) {
            this.percent = Math.max(0, Math.min(100, percent));
            if (phase != null && !phase.isBlank()) {
                this.phase = phase;
            }
            this.updatedAtMs = System.currentTimeMillis();
            if (detail != null && !detail.isBlank()) {
                log("INFO", detail);
            } else if (phase != null && !phase.isBlank()) {
                log("INFO", phase);
            }
        }

        @Override
        public void log(String level, String message) {
            if (message == null || message.isBlank()) {
                return;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("time", Instant.now().toString());
            row.put("level", level == null ? "INFO" : level);
            row.put("message", message);
            synchronized (logs) {
                logs.add(row);
                while (logs.size() > MAX_LOGS) {
                    logs.remove(0);
                }
            }
            this.updatedAtMs = System.currentTimeMillis();
        }

        void completeSuccess(String message) {
            this.percent = 100;
            this.phase = "完成";
            this.message = message;
            this.state = STATE_SUCCESS;
            this.updatedAtMs = System.currentTimeMillis();
            log("INFO", message);
        }

        void completeFailed(String error) {
            this.state = STATE_FAILED;
            this.error = error;
            this.message = error;
            this.phase = "失败";
            this.updatedAtMs = System.currentTimeMillis();
            log("ERROR", error);
        }
    }
}
