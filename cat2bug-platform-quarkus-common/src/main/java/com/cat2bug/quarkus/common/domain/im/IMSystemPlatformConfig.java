package com.cat2bug.quarkus.common.domain.im;

public class IMSystemPlatformConfig extends IMBasePlatformConfig {

    private boolean backgroundMusic;
    private String backgroundMusicUrl;
    private boolean panel;

    public IMSystemPlatformConfig() {
    }

    public IMSystemPlatformConfig(boolean configSwitch, boolean backgroundMusic, String backgroundMusicUrl, boolean panel) {
        super(configSwitch);
        this.backgroundMusic = backgroundMusic;
        this.backgroundMusicUrl = backgroundMusicUrl;
        this.panel = panel;
    }

    public boolean isBackgroundMusic() {
        return backgroundMusic;
    }

    public void setBackgroundMusic(boolean backgroundMusic) {
        this.backgroundMusic = backgroundMusic;
    }

    public String getBackgroundMusicUrl() {
        return backgroundMusicUrl;
    }

    public void setBackgroundMusicUrl(String backgroundMusicUrl) {
        this.backgroundMusicUrl = backgroundMusicUrl;
    }

    public boolean isPanel() {
        return panel;
    }

    public void setPanel(boolean panel) {
        this.panel = panel;
    }
}
