package com.cat2bug.admin.service.report;

import com.cat2bug.admin.domain.report.ReportPresetManifest;
import com.cat2bug.admin.domain.report.ReportTemplatePickerGroup;
import com.cat2bug.admin.domain.report.ReportTemplatePickerItem;
import com.cat2bug.admin.domain.report.ReportTemplatePickerResponse;
import com.cat2bug.common.domain.SysReportTemplate;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.storage.FileUploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads preset report templates from classpath {@code 报告/模版/}.
 */
@ApplicationScoped
public class ReportPresetTemplateService {

    public static final String GROUP_CUSTOM = "custom";
    public static final String GROUP_ISO_29119_3 = "iso-29119-3";
    public static final String GROUP_ISO_9001 = "iso-9001";

    public static final String CUSTOM_GROUP_TITLE = "自定义模版";
    public static final String ISO_29119_3_GROUP_TITLE = "ISO/IEC/IEEE 29119-3:2021";
    public static final String ISO_9001_GROUP_TITLE = "ISO 9001";

    private static final String MANIFEST_PATH = "报告/模版/manifest.json";
    private static final String RESOURCE_ROOT = "报告/模版/";
    private static final String PRESET_API_PREFIX = "/system/report/template/preset/";
    /** 预览资源版本：重渲主题后 bump，强制客户端绕过旧缓存 */
    private static final String PREVIEW_URL_VERSION = "?v=light-1";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    FileUploadService fileUploadService;

    public ReportTemplatePickerResponse buildPicker(List<SysReportTemplate> customTemplates) {
        return buildPicker(customTemplates, null);
    }

    /**
     * @param titleKeyword optional case-insensitive substring filter for preset titles
     *                     (custom templates are expected to be filtered by the caller/DB)
     */
    public ReportTemplatePickerResponse buildPicker(List<SysReportTemplate> customTemplates, String titleKeyword) {
        ReportPresetManifest manifest = loadManifest().orElse(null);
        Map<String, ReportPresetManifest.ReportPresetManifestGroup> manifestByCode = indexManifestGroups(manifest);
        String normalizedKeyword = normalizeTitleKeyword(titleKeyword);

        ReportTemplatePickerResponse response = new ReportTemplatePickerResponse();
        List<ReportTemplatePickerGroup> groups = new ArrayList<>();

        ReportTemplatePickerGroup customGroup = new ReportTemplatePickerGroup();
        customGroup.setCode(GROUP_CUSTOM);
        customGroup.setTitle(CUSTOM_GROUP_TITLE);
        List<ReportTemplatePickerItem> customItems = new ArrayList<>();
        if (customTemplates != null) {
            for (SysReportTemplate template : customTemplates) {
                customItems.add(ReportTemplatePickerItem.fromCustom(template));
            }
        }
        customGroup.setItems(customItems);
        groups.add(customGroup);

        groups.add(buildPresetPickerGroup(
                GROUP_ISO_29119_3, ISO_29119_3_GROUP_TITLE, manifestByCode.get(GROUP_ISO_29119_3), normalizedKeyword));
        groups.add(buildPresetPickerGroup(
                GROUP_ISO_9001, ISO_9001_GROUP_TITLE, manifestByCode.get(GROUP_ISO_9001), normalizedKeyword));

        response.setGroups(groups);
        return response;
    }

    public String readMarkdownContent(String groupCode, String folderOrKey, String key) {
        String templateKey = resolveTemplateKey(groupCode, folderOrKey, key);
        ReportPresetManifest.ReportPresetManifestItem item = findManifestItem(groupCode, templateKey)
                .orElseThrow(() -> new ApiException(404, "预制模版不存在"));
        return readUtf8Resource(item.getContentPath());
    }

    public byte[] readPreviewPng(String groupCode, String folderOrKey, String key) {
        String templateKey = resolveTemplateKey(groupCode, folderOrKey, key);
        ReportPresetManifest.ReportPresetManifestItem item = findManifestItem(groupCode, templateKey)
                .orElseThrow(() -> new ApiException(404, "预制模版不存在"));
        if (item.getPreviewPath() == null || item.getPreviewPath().isBlank()) {
            throw new ApiException(404, "预制模版预览不存在");
        }
        return readBinaryResource(item.getPreviewPath());
    }

    public SysReportTemplate buildCustomCopy(Long projectId, String groupCode, String folderOrKey, String key) {
        String templateKey = resolveTemplateKey(groupCode, folderOrKey, key);
        ReportPresetManifest.ReportPresetManifestItem item = findManifestItem(groupCode, templateKey)
                .orElseThrow(() -> new ApiException(404, "预制模版不存在"));
        String content = readUtf8Resource(item.getContentPath());

        SysReportTemplate copy = new SysReportTemplate();
        copy.setProjectId(projectId);
        copy.setTemplateTitle(item.getTitle() + " 副本");
        copy.setTemplateContent(content);
        copy.setMajorVersion(1L);
        copy.setMinorVersion(0L);
        copy.setTemplateIconUrl(persistPresetPreviewIcon(item));
        return copy;
    }

    /**
     * Copy classpath preset PNG into upload dir so custom picker cards keep a preview.
     */
    private String persistPresetPreviewIcon(ReportPresetManifest.ReportPresetManifestItem item) {
        if (item == null || item.getPreviewPath() == null || item.getPreviewPath().isBlank()) {
            return null;
        }
        try {
            byte[] png = readBinaryResource(item.getPreviewPath());
            if (png == null || png.length == 0) {
                return null;
            }
            try (ByteArrayInputStream input = new ByteArrayInputStream(png)) {
                FileUploadService.UploadResult result =
                        fileUploadService.upload(input, png.length, "preset-preview.png", null);
                return result.getUrl();
            }
        } catch (Exception ex) {
            // Preview is best-effort; content copy must still succeed.
            return null;
        }
    }

    /**
     * Resolve preset markdown when creating a report (presetKey may contain {@code manuals/...} for ISO 9001).
     */
    public String readMarkdownByPresetKey(String groupCode, String presetKey) {
        if (groupCode == null || groupCode.isBlank() || presetKey == null || presetKey.isBlank()) {
            throw new ApiException(400, "预制模版参数不完整");
        }
        if (presetKey.contains("/")) {
            int slash = presetKey.indexOf('/');
            return readMarkdownContent(groupCode, presetKey.substring(0, slash), presetKey.substring(slash + 1));
        }
        return readMarkdownContent(groupCode, presetKey, null);
    }

    public Optional<ReportPresetManifest> loadManifest() {
        InputStream raw = openClasspathStream(MANIFEST_PATH);
        if (raw == null) {
            return Optional.empty();
        }
        try (InputStream in = raw) {
            ReportPresetManifest manifest = objectMapper.readValue(in, ReportPresetManifest.class);
            return Optional.ofNullable(manifest);
        } catch (IOException ex) {
            throw new ApiException("读取预制模版清单失败: " + ex.getMessage());
        }
    }

    public String resolveTemplateKey(String groupCode, String folderOrKey, String key) {
        validatePresetGroup(groupCode);
        if (key != null && !key.isBlank()) {
            return folderOrKey + "/" + key;
        }
        return folderOrKey;
    }

    private ReportTemplatePickerGroup buildPresetPickerGroup(
            String defaultCode,
            String defaultTitle,
            ReportPresetManifest.ReportPresetManifestGroup manifestGroup,
            String normalizedKeyword) {
        ReportTemplatePickerGroup group = new ReportTemplatePickerGroup();
        group.setCode(defaultCode);
        group.setTitle(manifestGroup != null && manifestGroup.getTitle() != null
                ? manifestGroup.getTitle()
                : defaultTitle);
        List<ReportTemplatePickerItem> items = new ArrayList<>();
        if (manifestGroup != null && manifestGroup.getItems() != null) {
            for (ReportPresetManifest.ReportPresetManifestItem manifestItem : manifestGroup.getItems()) {
                if (manifestItem.getKey() == null || manifestItem.getKey().isBlank()) {
                    continue;
                }
                if (!titleMatchesKeyword(manifestItem.getTitle(), normalizedKeyword)) {
                    continue;
                }
                items.add(ReportTemplatePickerItem.fromPreset(
                        defaultCode,
                        manifestItem.getKey(),
                        manifestItem.getTitle(),
                        buildPreviewUrl(defaultCode, manifestItem.getKey())));
            }
        }
        group.setItems(items);
        return group;
    }

    private static String normalizeTitleKeyword(String titleKeyword) {
        if (titleKeyword == null) {
            return null;
        }
        String trimmed = titleKeyword.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    private static boolean titleMatchesKeyword(String title, String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return true;
        }
        if (title == null) {
            return false;
        }
        return title.toLowerCase().contains(normalizedKeyword);
    }

    private Map<String, ReportPresetManifest.ReportPresetManifestGroup> indexManifestGroups(ReportPresetManifest manifest) {
        Map<String, ReportPresetManifest.ReportPresetManifestGroup> map = new LinkedHashMap<>();
        if (manifest != null && manifest.getGroups() != null) {
            for (ReportPresetManifest.ReportPresetManifestGroup group : manifest.getGroups()) {
                if (group.getCode() != null) {
                    map.put(group.getCode(), group);
                }
            }
        }
        return map;
    }

    private Optional<ReportPresetManifest.ReportPresetManifestItem> findManifestItem(String groupCode, String templateKey) {
        validatePresetGroup(groupCode);
        Optional<ReportPresetManifest> manifest = loadManifest();
        if (manifest.isEmpty() || manifest.get().getGroups() == null) {
            return Optional.empty();
        }
        for (ReportPresetManifest.ReportPresetManifestGroup group : manifest.get().getGroups()) {
            if (!groupCode.equals(group.getCode()) || group.getItems() == null) {
                continue;
            }
            for (ReportPresetManifest.ReportPresetManifestItem item : group.getItems()) {
                if (templateKey.equals(item.getKey())) {
                    return Optional.of(item);
                }
            }
        }
        return Optional.empty();
    }

    private void validatePresetGroup(String groupCode) {
        if (!GROUP_ISO_29119_3.equals(groupCode) && !GROUP_ISO_9001.equals(groupCode)) {
            throw new ApiException(400, "不支持的预制模版分组: " + groupCode);
        }
    }

    private String readUtf8Resource(String relativePath) {
        String fullPath = RESOURCE_ROOT + relativePath;
        try (InputStream in = openClasspathStream(fullPath)) {
            if (in == null) {
                throw new ApiException(404, "预制模版文件不存在");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ApiException("读取预制模版失败: " + ex.getMessage());
        }
    }

    private byte[] readBinaryResource(String relativePath) {
        String fullPath = RESOURCE_ROOT + relativePath;
        try (InputStream in = openClasspathStream(fullPath)) {
            if (in == null) {
                throw new ApiException(404, "预制模版预览不存在");
            }
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new ApiException("读取预制模版预览失败: " + ex.getMessage());
        }
    }

    private InputStream openClasspathStream(String classpathPath) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream in = loader != null ? loader.getResourceAsStream(classpathPath) : null;
        if (in == null) {
            in = getClass().getClassLoader().getResourceAsStream(classpathPath);
        }
        return in;
    }

    private String buildPreviewUrl(String groupCode, String templateKey) {
        if (GROUP_ISO_9001.equals(groupCode) && templateKey.contains("/")) {
            int slash = templateKey.indexOf('/');
            return PRESET_API_PREFIX + groupCode + "/"
                    + templateKey.substring(0, slash) + "/"
                    + templateKey.substring(slash + 1) + "/preview" + PREVIEW_URL_VERSION;
        }
        return PRESET_API_PREFIX + groupCode + "/" + templateKey + "/preview" + PREVIEW_URL_VERSION;
    }
}
