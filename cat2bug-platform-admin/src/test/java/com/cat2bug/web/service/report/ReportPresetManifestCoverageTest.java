package com.cat2bug.web.service.report;

import com.cat2bug.web.domain.report.ReportPresetManifest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures packaged preset manifest covers skills corpus (29119-3 + 9001 manuals/records).
 */
class ReportPresetManifestCoverageTest
{
    @Test
    void manifestCoversThirtyBusinessTemplates() throws Exception
    {
        Path manifestPath = Paths.get("src/main/resources/报告/模版/manifest.json");
        if (!Files.exists(manifestPath))
        {
            manifestPath = Paths.get("../cat2bug-platform-admin/src/main/resources/报告/模版/manifest.json");
        }
        assertTrue(Files.exists(manifestPath), "manifest.json missing — run ./scripts/sync-report-preset-templates.sh");

        String json = Files.readString(manifestPath, StandardCharsets.UTF_8);
        ReportPresetManifest manifest = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, ReportPresetManifest.class);

        assertEquals(2, manifest.getGroups().size());
        int total = 0;
        for (ReportPresetManifest.ReportPresetManifestGroup group : manifest.getGroups())
        {
            assertFalse(group.getItems() == null || group.getItems().isEmpty());
            total += group.getItems().size();
        }
        assertEquals(30, total, "expected 9 (29119-3) + 21 (9001 manuals/records)");
    }
}
