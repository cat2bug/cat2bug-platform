package com.cat2bug.admin.mcp;

import com.cat2bug.system.mapper.SysMcpIdempotencyMapper;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Date;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 创建类工具的幂等键：同一用户、同一工具、同一键在 24 小时内只执行一次，重放返回首次结果的对象 ID。
 */
@ApplicationScoped
public class McpIdempotency {

    static final long KEEP_MILLIS = 24L * 3600_000L;
    private static final Pattern KEY = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");

    @Inject
    SysMcpIdempotencyMapper mapper;

    public record Outcome(String resultId, boolean replayed) {
    }

    public Outcome run(Long userId, String tool, String key, Supplier<String> action) {
        if (key == null || key.isBlank()) {
            return new Outcome(action.get(), false);
        }
        String k = key.trim();
        if (!KEY.matcher(k).matches()) {
            throw new ToolCallException("idempotency_key 只能包含字母、数字和 ._:-，长度 1～64");
        }
        Date after = new Date(System.currentTimeMillis() - KEEP_MILLIS);
        String existing = mapper.selectResultId(userId, tool, k, after);
        if (existing != null) {
            return new Outcome(existing, true);
        }
        mapper.deleteOne(userId, tool, k);
        String resultId = action.get();
        try {
            mapper.insert(userId, tool, k, resultId, new Date());
        } catch (RuntimeException ignored) {
            // 并发重放时主键冲突：对象已创建，以本次结果返回即可
        }
        return new Outcome(resultId, false);
    }

    @Scheduled(every = "1h", delayed = "5m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void purge() {
        try {
            mapper.deleteBefore(new Date(System.currentTimeMillis() - KEEP_MILLIS));
        } catch (RuntimeException ignored) {
            // 安装向导完成前运行期数据源不可用
        }
    }
}
