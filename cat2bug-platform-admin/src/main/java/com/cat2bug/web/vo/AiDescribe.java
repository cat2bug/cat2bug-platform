package com.cat2bug.web.vo;

import lombok.Data;

/**
 * 缺陷 AI 补全请求体。
 * <p>
 * {@link #serviceType} 缺省为 {@code ollama}；{@link #modelId} 在 Ollama 下为模型名，
 * 在 OpenAI 下为账号主键（字符串）。未传 {@link #modelId} 时由服务端解析为
 * 当前环境第一个已下载的 Ollama 模型，或该项目下第一个 OpenAI 账号。
 * </p>
 *
 * @Author: yuzhantao
 * @CreateTime: 2024-06-25 23:14
 * @Version: 1.0.0
 */
@Data
public class AiDescribe {
    private String describe;
    private Long projectId;
    /** ollama / openai，缺省 ollama */
    private String serviceType;
    /** Ollama：模型名；OpenAI：账号 ID（字符串） */
    private String modelId;
}
