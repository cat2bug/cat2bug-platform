package com.cat2bug.system.util;

import com.cat2bug.common.domain.ModulePageSettings;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.security.TeamModuleGates;
import com.cat2bug.common.util.ModulePageSettingsSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModulePageSettingsSupportTest {

    @Test
    void emptyConfigEnablesEveryModule() {
        ModulePageSettings settings = ModulePageSettingsSupport.parseOrDefault(null);
        assertTrue(settings.isEnabled(ModulePageSettings.CASE_PLAN));
        assertTrue(settings.isEnabled(ModulePageSettings.DEFECT));
        assertTrue(settings.isEnabled(ModulePageSettings.DELIVERABLE));
        assertTrue(settings.isEnabled(ModulePageSettings.DOCUMENT));
        assertTrue(settings.isEnabled(ModulePageSettings.CODE));
        assertNull(settings.alias("module"));
        assertNull(settings.getDocument().getMaxUploadMb());
    }

    @Test
    void partialJsonKeepsOtherModulesEnabled() {
        ModulePageSettings settings = ModulePageSettingsSupport.parseOrDefault(
                "{\"defect\":{\"enabled\":false,\"label\":\"问题\"},\"case_plan\":{\"enabled\":true,\"labelCase\":\"场景\",\"labelPlan\":\"迭代\"}}");
        assertFalse(settings.isEnabled(ModulePageSettings.DEFECT));
        assertEquals("问题", settings.alias("defect"));
        assertTrue(settings.isEnabled(ModulePageSettings.CASE_PLAN));
        assertEquals("场景", settings.alias("case"));
        assertEquals("迭代", settings.alias("plan"));
        assertTrue(settings.isEnabled(ModulePageSettings.CODE));
    }

    @Test
    void rejectsUploadAboveSystemCeiling() {
        ModulePageSettings settings = ModulePageSettingsSupport.defaults();
        settings.getDocument().setMaxUploadMb(600);
        assertThrows(ApiException.class, () -> ModulePageSettingsSupport.normalizeAndValidate(settings, 500));
    }

    @Test
    void blankUploadUsesSystemCeiling() {
        ModulePageSettings settings = ModulePageSettingsSupport.normalizeAndValidate(
                ModulePageSettingsSupport.defaults(), 500);
        assertEquals(500, ModulePageSettingsSupport.effectiveMaxMb(settings, 500));
        settings.getDocument().setMaxUploadMb(100);
        ModulePageSettings saved = ModulePageSettingsSupport.normalizeAndValidate(settings, 500);
        assertEquals(100, ModulePageSettingsSupport.effectiveMaxMb(saved, 500));
    }

    @Test
    void aliasReplacesVisibleDeliverableName() {
        ModulePageSettings settings = ModulePageSettingsSupport.defaults();
        settings.getDeliverable().setLabel("模块");
        settings = ModulePageSettingsSupport.normalize(settings);
        assertEquals("模块(必填)", ModulePageSettingsSupport.applyVisible("交付物(必填)", settings));
        assertEquals("模块", settings.aliasForMcp("deliverable"));
    }

    @Test
    void permissionPrefixesMapOntoTeamGates() {
        assertEquals(TeamModuleGates.CASE_PLAN, TeamModuleGates.gateOfPermission("system:case:list"));
        assertEquals(TeamModuleGates.CASE_PLAN, TeamModuleGates.gateOfPermission("system:plan:edit"));
        assertEquals(TeamModuleGates.CASE_PLAN, TeamModuleGates.gateOfPermission("system:PlanItem:list"));
        assertEquals(TeamModuleGates.CASE_PLAN, TeamModuleGates.gateOfPermission("system:case:review"));
        assertEquals(TeamModuleGates.CASE_PLAN, TeamModuleGates.gateOfPermission("system:case-review:list"));
        assertEquals(TeamModuleGates.DEFECT, TeamModuleGates.gateOfPermission("system:defect:list"));
        assertEquals(TeamModuleGates.DEFECT, TeamModuleGates.gateOfPermission("system:comment:list"));
        assertEquals(TeamModuleGates.DELIVERABLE, TeamModuleGates.gateOfPermission("system:module:add"));
        assertEquals(TeamModuleGates.DOCUMENT, TeamModuleGates.gateOfPermission("system:document:list"));
        assertEquals(TeamModuleGates.CODE, TeamModuleGates.gateOfPermission("system:git:list"));
        assertNull(TeamModuleGates.gateOfPermission("system:team:edit"));
        assertNull(TeamModuleGates.gateOfPermission("system:project:case-type:list"));
    }

    @Test
    void casePlanOffDropsCaseAndPlanFromMcpKeys() {
        assertEquals(TeamModuleGates.CASE_PLAN, TeamModuleGates.gateOfMcpKey("case"));
        assertEquals(TeamModuleGates.CASE_PLAN, TeamModuleGates.gateOfMcpKey("plan"));
        assertNull(TeamModuleGates.gateOfMcpKey("dashboard"));
        assertEquals(TeamModuleGates.CASE_PLAN, TeamModuleGates.gateOfPath("system/PlanItem/list"));
        assertEquals(TeamModuleGates.CASE_PLAN, TeamModuleGates.gateOfPath("api/case"));
        assertTrue(TeamModuleGates.exempt("system/team/1/module-page-settings"));
        assertTrue(TeamModuleGates.exempt("system/project/8/case-types"));
        ModulePageSettings closed = ModulePageSettingsSupport.defaults();
        closed.getCasePlan().setEnabled(false);
        String denied = TeamModuleGates.closedGate(false, new String[]{"system:case:list", "system:plan:list"},
                closed::isEnabled);
        assertEquals(TeamModuleGates.CASE_PLAN, denied);
        String stillOpen = TeamModuleGates.closedGate(true,
                new String[]{"system:case:list", "system:project:case-type:list"}, closed::isEnabled);
        assertNull(stillOpen);
        List<String> kept = List.of("defect", "case", "plan", "code").stream()
                .filter(key -> {
                    String gate = TeamModuleGates.gateOfMcpKey(key);
                    return gate == null || closed.isEnabled(gate);
                })
                .toList();
        assertEquals(List.of("defect", "code"), kept);
    }
}
