package com.cat2bug.ai.vo;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-11 12:19
 * @Version: 1.0.0
 */
@Data
public class ResponseBody {
    /**
     * 生成响应所花费的时间
     */
    private long totalDuration;
    /**
     * 加载模型的时间（以纳秒为单位）
     */
    private long loadDuration;
    /**
     * 提示符号中的令牌数量
     */
    private int promptEvalCount;
    /**
     * 花在纳秒内评估提示的时间
     */
    private long promptEvalDuration;
    /**
     * 响应中的令牌数量
     */
    private int evalCount;
    /**
     * 生成响应的时间（以纳秒为单位）
     */
    private long evalDuration;
    /**
     * 此响应中使用的对话的编码，可以在下一个请求中发送，以保持对话记忆
     */
    private String context;
    /**
     * 如果响应是流式传输的，则为空，如果没有流式传输，这将包含完整的响应
     */
    private long response;
}
