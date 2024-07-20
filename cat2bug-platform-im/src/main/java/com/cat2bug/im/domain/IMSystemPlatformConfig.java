package com.cat2bug.im.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:22
 * @Version: 1.0.0
 * 系统内部通知配置
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IMSystemPlatformConfig extends IMBasePlatformConfig {
    /**
     * 背景音乐文件名
     */
    private boolean backgroundMusic;

    /**
     * 背景音乐路径
     */
    private String backgroundMusicUrl;

    /**
     * 是否显示提示面板
     */
    private boolean panel;

    public IMSystemPlatformConfig(boolean configSwitch, boolean backgroundMusic, String backgroundMusicUrl, boolean panel) {
        super(configSwitch);
        this.backgroundMusic = backgroundMusic;
        this.backgroundMusicUrl = backgroundMusicUrl;
        this.panel = panel;
    }
}
