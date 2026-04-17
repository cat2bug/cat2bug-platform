package com.cat2bug.common.core.domain.model;

import java.io.Serializable;

/**
 * 机器人配置
 *
 * @author Cat2Bug Platform
 */
public class RobotConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 智能体平台 */
    private String platform;

    /** 平台URL */
    private String platformUrl;

    /** 密钥（加密存储） */
    private String secretKey;

    /** 模型 */
    private String model;

    /** 机器人介绍 */
    private String introduction;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPlatformUrl() {
        return platformUrl;
    }

    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    @Override
    public String toString() {
        return "RobotConfig{" +
                "platform='" + platform + '\'' +
                ", platformUrl='" + platformUrl + '\'' +
                ", model='" + model + '\'' +
                ", introduction='" + introduction + '\'' +
                '}';
    }
}
