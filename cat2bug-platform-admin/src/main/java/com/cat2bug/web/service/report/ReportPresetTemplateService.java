package com.cat2bug.web.service.report;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysReportTemplate;
import com.cat2bug.web.domain.report.ReportPresetManifest;
import com.cat2bug.web.domain.report.ReportTemplatePickerGroup;
import com.cat2bug.web.domain.report.ReportTemplatePickerItem;
import com.cat2bug.web.domain.report.ReportTemplatePickerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

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
@Service
public class ReportPresetTemplateService
{
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReportTemplatePickerResponse buildPicker(List<SysReportTemplate> customTemplates)
    {
        ReportPresetManifest manifest = loadManifest().orElse(null);
        Map<String, ReportPresetManifest.ReportPresetManifestGroup> manifestByCode = indexManifestGroups(manifest);

        ReportTemplatePickerResponse response = new ReportTemplatePickerResponse();
        List<ReportTemplatePickerGroup> groups = new ArrayList<>();

        ReportTemplatePickerGroup customGroup = new ReportTemplatePickerGroup();
        customGroup.setCode(GROUP_CUSTOM);
        customGroup.setTitle(CUSTOM_GROUP_TITLE);
        List<ReportTemplatePickerItem> customItems = new ArrayList<>();
        if (customTemplates != null)
        {
            for (SysReportTemplate template : customTemplates)
            {
                customItems.add(ReportTemplatePickerItem.fromCustom(template));
            }
        }
        customGroup.setItems(customItems);
        groups.add(customGroup);

        groups.add(buildPresetPickerGroup(GROUP_ISO_29119_3, ISO_29119_3_GROUP_TITLE, manifestByCode.get(GROUP_ISO_29119_3)));
        groups.add(buildPresetPickerGroup(GROUP_ISO_9001, ISO_9001_GROUP_TITLE, manifestByCode.get(GROUP_ISO_9001)));

        response.setGroups(groups);
        return response;
    }

    public String readMarkdownContent(String groupCode, String folderOrKey, String key)
    {
        String templateKey = resolveTemplateKey(groupCode, folderOrKey, key);
        ReportPresetManifest.ReportPresetManifestItem item = findManifestItem(groupCode, templateKey)
                .orElseThrow(() -> new ServiceException("预制模版不存在", HttpStatus.NOT_FOUND.value()));
        return readUtf8Resource(item.getContentPath());
    }

    public byte[] readPreviewPng(String groupCode, String folderOrKey, String key)
    {
        String templateKey = resolveTemplateKey(groupCode, folderOrKey, key);
        ReportPresetManifest.ReportPresetManifestItem item = findManifestItem(groupCode, templateKey)
                .orElseThrow(() -> new ServiceException("预制模版不存在", HttpStatus.NOT_FOUND.value()));
        if (item.getPreviewPath() == null || item.getPreviewPath().isBlank())
        {
            throw new ServiceException("预制模版预览不存在", HttpStatus.NOT_FOUND.value());
        }
        return readBinaryResource(item.getPreviewPath());
    }

    public SysReportTemplate buildCustomCopy(Long projectId, String groupCode, String folderOrKey, String key)
    {
        String templateKey = resolveTemplateKey(groupCode, folderOrKey, key);
        ReportPresetManifest.ReportPresetManifestItem item = findManifestItem(groupCode, templateKey)
                .orElseThrow(() -> new ServiceException("预制模版不存在", HttpStatus.NOT_FOUND.value()));
        String content = readUtf8Resource(item.getContentPath());

        SysReportTemplate copy = new SysReportTemplate();
        copy.setProjectId(projectId);
        copy.setTemplateTitle(item.getTitle() + " 副本");
        copy.setTemplateContent(content);
        copy.setMajorVersion(1L);
        copy.setMinorVersion(0L);
        return copy;
    }

    /**
     * Resolve preset markdown when creating a report (presetKey may contain {@code manuals/...} for ISO 9001).
     */
    public String readMarkdownByPresetKey(String groupCode, String presetKey)
    {
        if (groupCode == null || groupCode.isBlank() || presetKey == null || presetKey.isBlank())
        {
            throw new ServiceException("预制模版参数不完整", HttpStatus.BAD_REQUEST.value());
        }
        if (presetKey.contains("/"))
        {
            int slash = presetKey.indexOf('/');
            return readMarkdownContent(groupCode, presetKey.substring(0, slash), presetKey.substring(slash + 1));
        }
        return readMarkdownContent(groupCode, presetKey, null);
    }

    public Optional<ReportPresetManifest> loadManifest()
    {
        ClassPathResource resource = new ClassPathResource(MANIFEST_PATH);
        if (!resource.exists())
        {
            return Optional.empty();
        }
        try (InputStream in = resource.getInputStream())
        {
            ReportPresetManifest manifest = objectMapper.readValue(in, ReportPresetManifest.class);
            return Optional.ofNullable(manifest);
        }
        catch (IOException ex)
        {
            throw new ServiceException("读取预制模版清单失败: " + ex.getMessage());
        }
    }

    public String resolveTemplateKey(String groupCode, String folderOrKey, String key)
    {
        validatePresetGroup(groupCode);
        if (key != null && !key.isBlank())
        {
            return folderOrKey + "/" + key;
        }
        return folderOrKey;
    }

    private ReportTemplatePickerGroup buildPresetPickerGroup(
            String defaultCode,
            String defaultTitle,
            ReportPresetManifest.ReportPresetManifestGroup manifestGroup)
    {
        ReportTemplatePickerGroup group = new ReportTemplatePickerGroup();
        group.setCode(defaultCode);
        group.setTitle(manifestGroup != null && manifestGroup.getTitle() != null
                ? manifestGroup.getTitle()
                : defaultTitle);
        List<ReportTemplatePickerItem> items = new ArrayList<>();
        if (manifestGroup != null && manifestGroup.getItems() != null)
        {
            for (ReportPresetManifest.ReportPresetManifestItem manifestItem : manifestGroup.getItems())
            {
                if (manifestItem.getKey() == null || manifestItem.getKey().isBlank())
                {
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

    private Map<String, ReportPresetManifest.ReportPresetManifestGroup> indexManifestGroups(ReportPresetManifest manifest)
    {
        Map<String, ReportPresetManifest.ReportPresetManifestGroup> map = new LinkedHashMap<>();
        if (manifest != null && manifest.getGroups() != null)
        {
            for (ReportPresetManifest.ReportPresetManifestGroup group : manifest.getGroups())
            {
                if (group.getCode() != null)
                {
                    map.put(group.getCode(), group);
                }
            }
        }
        return map;
    }

    private Optional<ReportPresetManifest.ReportPresetManifestItem> findManifestItem(String groupCode, String templateKey)
    {
        validatePresetGroup(groupCode);
        Optional<ReportPresetManifest> manifest = loadManifest();
        if (manifest.isEmpty() || manifest.get().getGroups() == null)
        {
            return Optional.empty();
        }
        for (ReportPresetManifest.ReportPresetManifestGroup group : manifest.get().getGroups())
        {
            if (!groupCode.equals(group.getCode()) || group.getItems() == null)
            {
                continue;
            }
            for (ReportPresetManifest.ReportPresetManifestItem item : group.getItems())
            {
                if (templateKey.equals(item.getKey()))
                {
                    return Optional.of(item);
                }
            }
        }
        return Optional.empty();
    }

    private void validatePresetGroup(String groupCode)
    {
        if (!GROUP_ISO_29119_3.equals(groupCode) && !GROUP_ISO_9001.equals(groupCode))
        {
            throw new ServiceException("不支持的预制模版分组: " + groupCode, HttpStatus.BAD_REQUEST.value());
        }
    }

    private String readUtf8Resource(String relativePath)
    {
        try
        {
            ClassPathResource resource = new ClassPathResource(RESOURCE_ROOT + relativePath);
            if (!resource.exists())
            {
                throw new ServiceException("预制模版文件不存在", HttpStatus.NOT_FOUND.value());
            }
            try (InputStream in = resource.getInputStream())
            {
                return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            }
        }
        catch (IOException ex)
        {
            throw new ServiceException("读取预制模版失败: " + ex.getMessage());
        }
    }

    private byte[] readBinaryResource(String relativePath)
    {
        try
        {
            ClassPathResource resource = new ClassPathResource(RESOURCE_ROOT + relativePath);
            if (!resource.exists())
            {
                throw new ServiceException("预制模版预览不存在", HttpStatus.NOT_FOUND.value());
            }
            try (InputStream in = resource.getInputStream())
            {
                return StreamUtils.copyToByteArray(in);
            }
        }
        catch (IOException ex)
        {
            throw new ServiceException("读取预制模版预览失败: " + ex.getMessage());
        }
    }

    private String buildPreviewUrl(String groupCode, String templateKey)
    {
        if (GROUP_ISO_9001.equals(groupCode) && templateKey.contains("/"))
        {
            int slash = templateKey.indexOf('/');
            return PRESET_API_PREFIX + groupCode + "/"
                    + templateKey.substring(0, slash) + "/"
                    + templateKey.substring(slash + 1) + "/preview" + PREVIEW_URL_VERSION;
        }
        return PRESET_API_PREFIX + groupCode + "/" + templateKey + "/preview" + PREVIEW_URL_VERSION;
    }
}
