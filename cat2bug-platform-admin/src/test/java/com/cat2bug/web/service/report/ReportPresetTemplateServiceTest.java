package com.cat2bug.web.service.report;

import com.cat2bug.web.domain.report.ReportPresetManifest;
import com.cat2bug.web.domain.report.ReportTemplatePickerResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportPresetTemplateServiceTest
{
    private final ReportPresetTemplateService reportPresetTemplateService = new ReportPresetTemplateService();

    @Test
    void loadManifest_readsClasspathJson()
    {
        Optional<ReportPresetManifest> manifest = reportPresetTemplateService.loadManifest();
        assertTrue(manifest.isPresent());
        assertFalse(manifest.get().getGroups().isEmpty());
    }

    @Test
    void buildPicker_includesThreeGroupsWithPresetMetadata()
    {
        ReportTemplatePickerResponse picker = reportPresetTemplateService.buildPicker(Collections.emptyList());
        assertEquals(3, picker.getGroups().size());
        assertEquals(ReportPresetTemplateService.GROUP_CUSTOM, picker.getGroups().get(0).getCode());
        assertEquals(ReportPresetTemplateService.GROUP_ISO_29119_3, picker.getGroups().get(1).getCode());
        assertFalse(picker.getGroups().get(1).getItems().isEmpty());
        assertEquals("preset", picker.getGroups().get(1).getItems().get(0).getSource());
        assertEquals(Boolean.FALSE, picker.getGroups().get(1).getItems().get(0).getEditable());
        assertNotNull(picker.getGroups().get(1).getItems().get(0).getTemplateIconUrl());
        assertTrue(picker.getGroups().get(1).getItems().get(0).getTemplateIconUrl().contains("/preview"));
    }

    @Test
    void readMarkdownContent_loadsStubFile()
    {
        String content = reportPresetTemplateService.readMarkdownContent(
                ReportPresetTemplateService.GROUP_ISO_29119_3,
                "test-completion-report",
                null);
        assertTrue(content.contains("测试完成报告"));
    }
}
