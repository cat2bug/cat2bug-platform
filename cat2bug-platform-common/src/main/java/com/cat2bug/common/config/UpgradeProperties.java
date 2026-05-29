package com.cat2bug.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Legacy 升级向导配置（{@code cat2bug.upgrade.*}）。
 */
@Component
@ConfigurationProperties(prefix = "cat2bug.upgrade")
public class UpgradeProperties
{
    /** 为 true 时跳过升级向导（也可通过环境变量 CAT2BUG_UPGRADE_SKIP=true） */
    private boolean skip = false;

    /** 升级状态侧车文件路径（install 文件不存在时使用） */
    private String statePath;

    /** pending | running | failed | restart_required | completed（内存缓存，以磁盘为准） */
    private String state = InstallConfigSupport.UPGRADE_STATE_COMPLETED;

    private String lastError = "";

    private String lastStep = "";

    private int attemptCount;

    private String targetVersion = "";

    private String completedVersion = "";

    public boolean isSkip()
    {
        return skip;
    }

    public void setSkip(boolean skip)
    {
        this.skip = skip;
    }

    public String getStatePath()
    {
        return statePath;
    }

    public void setStatePath(String statePath)
    {
        this.statePath = statePath;
    }

    public boolean isSkipFromEnv()
    {
        return "true".equalsIgnoreCase(System.getenv("CAT2BUG_UPGRADE_SKIP"));
    }

    public boolean isUpgradeSkipped()
    {
        return skip || isSkipFromEnv();
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public String getLastError()
    {
        return lastError;
    }

    public void setLastError(String lastError)
    {
        this.lastError = lastError;
    }

    public String getLastStep()
    {
        return lastStep;
    }

    public void setLastStep(String lastStep)
    {
        this.lastStep = lastStep;
    }

    public int getAttemptCount()
    {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount)
    {
        this.attemptCount = attemptCount;
    }

    public String getTargetVersion()
    {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion)
    {
        this.targetVersion = targetVersion;
    }

    public String getCompletedVersion()
    {
        return completedVersion;
    }

    public void setCompletedVersion(String completedVersion)
    {
        this.completedVersion = completedVersion;
    }
}
