package com.cat2bug.system.util;

import com.cat2bug.common.util.DefectPageSettingsSupport;

import com.cat2bug.common.domain.DefectPageSettings;
import com.cat2bug.common.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefectPageSettingsSupportTest {

    @Test
    void defaultsEnableAllViewsAndAllowShare() {
        DefectPageSettings s = DefectPageSettingsSupport.defaults();
        assertEquals(4, DefectPageSettingsSupport.enabledViews(s).size());
        assertTrue(s.isAllowShare());
    }

    @Test
    void rejectInvertedLifeThresholds() {
        DefectPageSettings s = DefectPageSettingsSupport.defaults();
        s.getLifeTimeColors().setGreenBelowDays(10);
        s.getLifeTimeColors().setOrangeBelowDays(3);
        assertThrows(ApiException.class, () -> DefectPageSettingsSupport.normalizeAndValidate(s));
    }

    @Test
    void rejectAllViewsDisabled() {
        DefectPageSettings s = DefectPageSettingsSupport.defaults();
        for (String key : s.getViews().keySet()) {
            s.getViews().put(key, false);
        }
        assertThrows(ApiException.class, () -> DefectPageSettingsSupport.normalizeAndValidate(s));
    }

    @Test
    void lifeTimeColorBands() {
        DefectPageSettings s = DefectPageSettingsSupport.defaults();
        long twoDays = TimeUnit.DAYS.toMillis(2);
        long fiveDays = TimeUnit.DAYS.toMillis(5);
        long tenDays = TimeUnit.DAYS.toMillis(10);
        assertEquals("#67C23A", DefectPageSettingsSupport.lifeTimeColor(s, twoDays));
        assertEquals("#E6A23C", DefectPageSettingsSupport.lifeTimeColor(s, fiveDays));
        assertEquals("#F56C6C", DefectPageSettingsSupport.lifeTimeColor(s, tenDays));
    }

    @Test
    void rejectCountColorBands() {
        DefectPageSettings s = DefectPageSettingsSupport.defaults();
        assertEquals("#67C23A", DefectPageSettingsSupport.rejectCountColor(s, 2));
        assertEquals("#E6A23C", DefectPageSettingsSupport.rejectCountColor(s, 5));
        assertEquals("#F56C6C", DefectPageSettingsSupport.rejectCountColor(s, 9));
    }

    @Test
    void parseBlankUsesDefaults() {
        DefectPageSettings s = DefectPageSettingsSupport.parseOrDefault(null);
        assertTrue(s.getViews().get(DefectPageSettings.VIEW_TABLE));
        assertFalse(DefectPageSettingsSupport.enabledViews(s).isEmpty());
    }

    @Test
    void roundTripJsonKeepsCustomThreshold() {
        DefectPageSettings s = DefectPageSettingsSupport.defaults();
        s.getLifeTimeColors().setGreenBelowDays(1);
        s.getLifeTimeColors().setOrangeBelowDays(2);
        s.setAllowShare(false);
        String json = DefectPageSettingsSupport.toJson(s);
        DefectPageSettings again = DefectPageSettingsSupport.parseOrDefault(json);
        assertEquals(1, again.getLifeTimeColors().getGreenBelowDays(), 0.001);
        assertFalse(again.isAllowShare());
        assertEquals(Map.of(
                DefectPageSettings.VIEW_TABLE, true,
                DefectPageSettings.VIEW_EXCEL, true,
                DefectPageSettings.VIEW_GANTT, true,
                DefectPageSettings.VIEW_KANBAN, true
        ), again.getViews());
    }
}
