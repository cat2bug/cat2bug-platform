package com.cat2bug.framework.storage;

/**
 * 上传字节上限。框架提供调用点，由系统模块按 {@code sys_config} 与团队文档上限实现。
 */
public interface UploadByteLimit {

    /** 系统全局上限（字节）。 */
    long systemMaxBytes();

    /**
     * 能解析到项目时的有效上限（字节）。{@code projectId} 为空时等同系统上限。
     */
    long effectiveMaxBytes(Long projectId);
}
