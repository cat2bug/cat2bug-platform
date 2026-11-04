package com.cat2bug.common.util;

import com.cat2bug.common.domain.ModulePageSettings;
import com.cat2bug.common.exception.ApiException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 模块页面设置默认值、解析、归一与校验。 */
public final class ModulePageSettingsSupport {

    public static final int LABEL_MAX = 64;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ModulePageSettingsSupport() {
    }

    public static ModulePageSettings defaults() {
        ModulePageSettings settings = new ModulePageSettings();
        settings.setCasePlan(emptySwitch(true));
        settings.setDefect(emptySwitch(false));
        settings.setDeliverable(emptySwitch(false));
        settings.setDocument(emptySwitch(false));
        settings.setCode(emptySwitch(false));
        return settings;
    }

    public static ModulePageSettings parseOrDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaults();
        }
        try {
            ModulePageSettings parsed = MAPPER.readValue(raw, ModulePageSettings.class);
            return normalize(parsed);
        } catch (Exception e) {
            return defaults();
        }
    }

    public static String toJson(ModulePageSettings settings) {
        try {
            return MAPPER.writeValueAsString(normalize(settings));
        } catch (Exception e) {
            throw new ApiException(MessageUtils.message("module.page-settings.serialize-failed"));
        }
    }

    public static ModulePageSettings normalizeAndValidate(ModulePageSettings input, int systemMaxMb) {
        ModulePageSettings settings = normalize(input);
        validate(settings, systemMaxMb);
        return settings;
    }

    /** 团队文档上限；空或越界时用系统上限。 */
    public static int effectiveMaxMb(ModulePageSettings settings, int systemMaxMb) {
        int ceiling = systemMaxMb >= 1 ? systemMaxMb : UploadLimits.DEFAULT_MB;
        ModulePageSettings normalized = settings == null ? defaults() : normalize(settings);
        Integer team = normalized.getDocument() == null ? null : normalized.getDocument().getMaxUploadMb();
        if (team == null || team < 1 || team > ceiling) {
            return ceiling;
        }
        return team;
    }

    public static ModulePageSettings normalize(ModulePageSettings input) {
        ModulePageSettings base = defaults();
        if (input == null) {
            return base;
        }
        base.setCasePlan(normalizeCasePlan(input.getCasePlan()));
        base.setDefect(normalizeLabeled(input.getDefect(), false));
        base.setDeliverable(normalizeLabeled(input.getDeliverable(), false));
        base.setDocument(normalizeLabeled(input.getDocument(), true));
        base.setCode(normalizeLabeled(input.getCode(), false));
        return base;
    }

    /**
     * 用别名替换可见文案中的默认模块名（长词优先，替换结果不再参与后续替换）。
     */
    public static String applyVisible(String text, ModulePageSettings settings) {
        if (text == null || text.isEmpty() || settings == null) {
            return text;
        }
        String out = text;
        java.util.List<String> slots = new java.util.ArrayList<>();
        for (Phrase phrase : PHRASES) {
            String alias = settings.alias(phrase.root);
            if (alias == null || alias.equals(phrase.text) || !out.contains(phrase.text)) {
                continue;
            }
            String token = "\u0001L" + slots.size() + "\u0001";
            out = out.replace(phrase.text, token);
            slots.add(alias);
        }
        for (int i = 0; i < slots.size(); i++) {
            out = out.replace("\u0001L" + i + "\u0001", slots.get(i));
        }
        return out;
    }

    private static void validate(ModulePageSettings settings, int systemMaxMb) {
        int ceiling = systemMaxMb >= 1 ? systemMaxMb : UploadLimits.DEFAULT_MB;
        checkLabel(settings.getCasePlan().getLabelCase());
        checkLabel(settings.getCasePlan().getLabelPlan());
        checkLabel(settings.getDefect().getLabel());
        checkLabel(settings.getDeliverable().getLabel());
        checkLabel(settings.getDocument().getLabel());
        checkLabel(settings.getCode().getLabel());
        Integer max = settings.getDocument().getMaxUploadMb();
        if (max != null && (max < 1 || max > ceiling)) {
            throw new ApiException(MessageUtils.message("module.page-settings.max-upload-invalid", ceiling));
        }
    }

    private static void checkLabel(String label) {
        if (label != null && label.length() > LABEL_MAX) {
            throw new ApiException(MessageUtils.message("module.page-settings.label-too-long", LABEL_MAX));
        }
    }

    private static ModulePageSettings.ModuleSwitch emptySwitch(boolean casePlan) {
        ModulePageSettings.ModuleSwitch entry = new ModulePageSettings.ModuleSwitch();
        entry.setEnabled(true);
        if (casePlan) {
            entry.setLabelCase(null);
            entry.setLabelPlan(null);
        }
        return entry;
    }

    private static ModulePageSettings.ModuleSwitch normalizeCasePlan(ModulePageSettings.ModuleSwitch src) {
        ModulePageSettings.ModuleSwitch dst = emptySwitch(true);
        if (src == null) {
            return dst;
        }
        dst.setEnabled(src.getEnabled() == null || src.getEnabled());
        dst.setLabelCase(blankToNull(src.getLabelCase()));
        dst.setLabelPlan(blankToNull(src.getLabelPlan()));
        return dst;
    }

    private static ModulePageSettings.ModuleSwitch normalizeLabeled(ModulePageSettings.ModuleSwitch src,
                                                                    boolean document) {
        ModulePageSettings.ModuleSwitch dst = emptySwitch(false);
        if (src == null) {
            return dst;
        }
        dst.setEnabled(src.getEnabled() == null || src.getEnabled());
        dst.setLabel(blankToNull(src.getLabel()));
        if (document) {
            dst.setMaxUploadMb(src.getMaxUploadMb());
        }
        return dst;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 默认可见名，长的在前，避免「测试计划」被「计划」截断。 */
    private static final Phrase[] PHRASES = {
            new Phrase("case", "测试用例"),
            new Phrase("plan", "测试计划"),
            new Phrase("case", "Test Case"),
            new Phrase("plan", "Test Plan"),
            new Phrase("module", "交付物"),
            new Phrase("module", "Deliverable"),
            new Phrase("doc", "文档"),
            new Phrase("doc", "Document"),
            new Phrase("defect", "缺陷"),
            new Phrase("defect", "Defect"),
            new Phrase("code", "源码"),
            new Phrase("code", "Source"),
            new Phrase("case", "用例"),
            new Phrase("plan", "计划"),
            new Phrase("code", "代码"),
            new Phrase("case", "Case"),
            new Phrase("plan", "Plan")
    };

    private record Phrase(String root, String text) {
    }
}
