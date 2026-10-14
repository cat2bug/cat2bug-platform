package com.cat2bug.quarkus.system.git.release;

/**
 * 发行版附件上限（design.md：单文件大小 + 每 release 附件数，后续可配置化）。
 */
public final class GitReleaseLimits
{
    /** 单个附件最大 100 MiB */
    public static final long MAX_ASSET_FILE_BYTES = 100L * 1024 * 1024;

    /** 每个发行版最多附件数 */
    public static final int MAX_ASSETS_PER_RELEASE = 20;

    private GitReleaseLimits()
    {
    }
}
