package com.cat2bug.web.domain.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Classpath manifest for preset report templates under {@code 报告/模版/manifest.json}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportPresetManifest
{
    private List<ReportPresetManifestGroup> groups = new ArrayList<>();

    public List<ReportPresetManifestGroup> getGroups()
    {
        return groups;
    }

    public void setGroups(List<ReportPresetManifestGroup> groups)
    {
        this.groups = groups != null ? groups : new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReportPresetManifestGroup
    {
        private String code;
        private String title;
        private List<ReportPresetManifestItem> items = new ArrayList<>();

        public String getCode()
        {
            return code;
        }

        public void setCode(String code)
        {
            this.code = code;
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        public List<ReportPresetManifestItem> getItems()
        {
            return items;
        }

        public void setItems(List<ReportPresetManifestItem> items)
        {
            this.items = items != null ? items : new ArrayList<>();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReportPresetManifestItem
    {
        private String key;
        private String title;
        private String contentPath;
        private String previewPath;

        public String getKey()
        {
            return key;
        }

        public void setKey(String key)
        {
            this.key = key;
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        public String getContentPath()
        {
            return contentPath;
        }

        public void setContentPath(String contentPath)
        {
            this.contentPath = contentPath;
        }

        public String getPreviewPath()
        {
            return previewPath;
        }

        public void setPreviewPath(String previewPath)
        {
            this.previewPath = previewPath;
        }
    }
}
