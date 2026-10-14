package com.cat2bug.quarkus.common.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MetaVo {

    private String title;
    private String titleI18nKey;
    private String icon;
    private boolean noCache;
    private String link;

    public MetaVo() {
    }

    public MetaVo(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }

    public MetaVo(String title, String icon, boolean noCache) {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
    }

    public MetaVo(String title, String icon, String link) {
        this.title = title;
        this.icon = icon;
        if (link != null && (link.startsWith("http://") || link.startsWith("https://"))) {
            this.link = link;
        }
    }

    public MetaVo(String title, String titleI18nKey, String icon, boolean noCache, String link) {
        this.title = title;
        this.titleI18nKey = titleI18nKey;
        this.icon = icon;
        this.noCache = noCache;
        if (link != null && (link.startsWith("http://") || link.startsWith("https://"))) {
            this.link = link;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleI18nKey() {
        return titleI18nKey;
    }

    public void setTitleI18nKey(String titleI18nKey) {
        this.titleI18nKey = titleI18nKey;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isNoCache() {
        return noCache;
    }

    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
