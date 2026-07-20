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
    void buildPicker_filtersPresetTitlesByKeyword()
    {
        ReportTemplatePickerResponse all = reportPresetTemplateService.buildPicker(Collections.emptyList());
        int allPresetCount = all.getGroups().get(1).getItems().size() + all.getGroups().get(2).getItems().size();
        assertTrue(allPresetCount > 0);

        ReportTemplatePickerResponse filtered = reportPresetTemplateService.buildPicker(Collections.emptyList(), "完成");
        int filteredCount = filtered.getGroups().get(1).getItems().size() + filtered.getGroups().get(2).getItems().size();
        assertTrue(filteredCount > 0);
        assertTrue(filteredCount <= allPresetCount);
        filtered.getGroups().get(1).getItems().forEach(item ->
                assertTrue(item.getTemplateTitle().toLowerCase().contains("完成")));
        filtered.getGroups().get(2).getItems().forEach(item ->
                assertTrue(item.getTemplateTitle().toLowerCase().contains("完成")));

        ReportTemplatePickerResponse none = reportPresetTemplateService.buildPicker(Collections.emptyList(), "___no_such_template___");
        assertTrue(none.getGroups().get(1).getItems().isEmpty());
        assertTrue(none.getGroups().get(2).getItems().isEmpty());
    }
}
