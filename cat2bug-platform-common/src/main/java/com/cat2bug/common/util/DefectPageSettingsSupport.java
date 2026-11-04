package com.cat2bug.common.util;

import com.cat2bug.common.domain.DefectPageSettings;
import com.cat2bug.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 缺陷页面设置默认值、解析与校验。 */
public final class DefectPageSettingsSupport {

    public static final String MSG_SHARE_DISABLED_KEY = "defect.page-settings.share-disabled";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> VIEW_ORDER = List.of(
            DefectPageSettings.VIEW_TABLE,
            DefectPageSettings.VIEW_EXCEL,
            DefectPageSettings.VIEW_GANTT,
            DefectPageSettings.VIEW_KANBAN);

    private DefectPageSettingsSupport() {
    }

    public static String shareDisabledMessage() {
        return MessageUtils.message(MSG_SHARE_DISABLED_KEY);
    }

    public static DefectPageSettings defaults() {
        DefectPageSettings s = new DefectPageSettings();
        Map<String, Boolean> views = new LinkedHashMap<>();
        for (String key : VIEW_ORDER) {
            views.put(key, true);
        }
        s.setViews(views);
        s.setLifeTimeColors(new DefectPageSettings.LifeTimeColors());
        s.setRejectCountColors(new DefectPageSettings.RejectCountColors());
        s.setAllowShare(true);
        return s;
    }

    public static DefectPageSettings parseOrDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaults();
        }
        try {
            DefectPageSettings parsed = MAPPER.readValue(raw, DefectPageSettings.class);
            return mergeWithDefaults(parsed);
        } catch (Exception e) {
            return defaults();
        }
    }

    public static String toJson(DefectPageSettings settings) {
        try {
            return MAPPER.writeValueAsString(normalize(settings));
        } catch (Exception e) {
            throw new ApiException(MessageUtils.message("defect.page-settings.serialize-failed"));
        }
    }

    public static DefectPageSettings normalizeAndValidate(DefectPageSettings input) {
        DefectPageSettings settings = normalize(input);
        validate(settings);
        return settings;
    }

    public static List<String> enabledViews(DefectPageSettings settings) {
        DefectPageSettings s = settings == null ? defaults() : mergeWithDefaults(settings);
        List<String> enabled = new ArrayList<>();
        for (String key : VIEW_ORDER) {
            if (Boolean.TRUE.equals(s.getViews().get(key))) {
                enabled.add(key);
            }
        }
        return enabled;
    }

    /** 存活时长（毫秒）→ 色阶色值。 */
    public static String lifeTimeColor(DefectPageSettings settings, long lifeMs) {
        DefectPageSettings.LifeTimeColors c = mergeWithDefaults(settings).getLifeTimeColors();
        double days = lifeMs / (1000.0 * 60 * 60 * 24);
        if (days < c.getGreenBelowDays()) {
            return c.getGreen();
        }
        if (days < c.getOrangeBelowDays()) {
            return c.getOrange();
        }
        return c.getRed();
    }

    public static String rejectCountColor(DefectPageSettings settings, int rejectCount) {
        DefectPageSettings.RejectCountColors c = mergeWithDefaults(settings).getRejectCountColors();
        if (rejectCount < c.getGreenBelow()) {
            return c.getGreen();
        }
        if (rejectCount < c.getOrangeBelow()) {
            return c.getOrange();
        }
        return c.getRed();
    }

    private static DefectPageSettings normalize(DefectPageSettings input) {
        return mergeWithDefaults(input == null ? defaults() : input);
    }

    private static DefectPageSettings mergeWithDefaults(DefectPageSettings input) {
        DefectPageSettings base = defaults();
        if (input == null) {
            return base;
        }
        Map<String, Boolean> views = new LinkedHashMap<>(base.getViews());
        if (input.getViews() != null) {
            for (String key : VIEW_ORDER) {
                if (input.getViews().containsKey(key) && input.getViews().get(key) != null) {
                    views.put(key, input.getViews().get(key));
                }
            }
        }
        base.setViews(views);
        if (input.getLifeTimeColors() != null) {
            DefectPageSettings.LifeTimeColors src = input.getLifeTimeColors();
            DefectPageSettings.LifeTimeColors dst = base.getLifeTimeColors();
            dst.setGreenBelowDays(src.getGreenBelowDays());
            dst.setOrangeBelowDays(src.getOrangeBelowDays());
            if (src.getGreen() != null && !src.getGreen().isBlank()) {
                dst.setGreen(src.getGreen());
            }
            if (src.getOrange() != null && !src.getOrange().isBlank()) {
                dst.setOrange(src.getOrange());
            }
            if (src.getRed() != null && !src.getRed().isBlank()) {
                dst.setRed(src.getRed());
            }
        }
        if (input.getRejectCountColors() != null) {
            DefectPageSettings.RejectCountColors src = input.getRejectCountColors();
            DefectPageSettings.RejectCountColors dst = base.getRejectCountColors();
            dst.setGreenBelow(src.getGreenBelow());
            dst.setOrangeBelow(src.getOrangeBelow());
            if (src.getGreen() != null && !src.getGreen().isBlank()) {
                dst.setGreen(src.getGreen());
            }
            if (src.getOrange() != null && !src.getOrange().isBlank()) {
                dst.setOrange(src.getOrange());
            }
            if (src.getRed() != null && !src.getRed().isBlank()) {
                dst.setRed(src.getRed());
            }
        }
        base.setAllowShare(input.isAllowShare());
        return base;
    }

    private static void validate(DefectPageSettings settings) {
        if (enabledViews(settings).isEmpty()) {
            throw new ApiException(MessageUtils.message("defect.page-settings.views-empty"));
        }
        DefectPageSettings.LifeTimeColors life = settings.getLifeTimeColors();
        if (life.getGreenBelowDays() < 0 || life.getOrangeBelowDays() < 0) {
            throw new ApiException(MessageUtils.message("defect.page-settings.life-threshold-negative"));
        }
        if (life.getGreenBelowDays() >= life.getOrangeBelowDays()) {
            throw new ApiException(MessageUtils.message("defect.page-settings.life-threshold-order"));
        }
        DefectPageSettings.RejectCountColors reject = settings.getRejectCountColors();
        if (reject.getGreenBelow() < 0 || reject.getOrangeBelow() < 0) {
            throw new ApiException(MessageUtils.message("defect.page-settings.reject-threshold-negative"));
        }
        if (reject.getGreenBelow() >= reject.getOrangeBelow()) {
            throw new ApiException(MessageUtils.message("defect.page-settings.reject-threshold-order"));
        }
    }
}
