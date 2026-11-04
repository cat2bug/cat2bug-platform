package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 团队功能模块页面设置（存 {@code sys_team.module_page_config} JSON）。
 * 空配置与缺省等价：全部启用、显示名为空、文档上传上限为空（用系统上限）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"case_plan", "defect", "deliverable", "document", "code"})
public class ModulePageSettings {

    public static final String CASE_PLAN = "case_plan";
    public static final String DEFECT = "defect";
    public static final String DELIVERABLE = "deliverable";
    public static final String DOCUMENT = "document";
    public static final String CODE = "code";

    @JsonProperty("case_plan")
    private ModuleSwitch casePlan = new ModuleSwitch();
    private ModuleSwitch defect = new ModuleSwitch();
    private ModuleSwitch deliverable = new ModuleSwitch();
    private ModuleSwitch document = new ModuleSwitch();
    private ModuleSwitch code = new ModuleSwitch();

    public ModuleSwitch getCasePlan() {
        return casePlan;
    }

    public void setCasePlan(ModuleSwitch casePlan) {
        this.casePlan = casePlan;
    }

    public ModuleSwitch getDefect() {
        return defect;
    }

    public void setDefect(ModuleSwitch defect) {
        this.defect = defect;
    }

    public ModuleSwitch getDeliverable() {
        return deliverable;
    }

    public void setDeliverable(ModuleSwitch deliverable) {
        this.deliverable = deliverable;
    }

    public ModuleSwitch getDocument() {
        return document;
    }

    public void setDocument(ModuleSwitch document) {
        this.document = document;
    }

    public ModuleSwitch getCode() {
        return code;
    }

    public void setCode(ModuleSwitch code) {
        this.code = code;
    }

    @JsonIgnore
    public boolean isEnabled(String gate) {
        ModuleSwitch entry = switch (gate == null ? "" : gate) {
            case CASE_PLAN -> casePlan;
            case DEFECT -> defect;
            case DELIVERABLE -> deliverable;
            case DOCUMENT -> document;
            case CODE -> code;
            default -> null;
        };
        return entry == null || entry.isOn();
    }

    /** 消息根键：{@code module/case/plan/doc/defect/defect.manage/code}。无别名时返回 {@code null}。 */
    @JsonIgnore
    public String alias(String rootKey) {
        if (rootKey == null) {
            return null;
        }
        return switch (rootKey) {
            case "module" -> text(deliverable == null ? null : deliverable.getLabel());
            case "case" -> text(casePlan == null ? null : casePlan.getLabelCase());
            case "plan" -> text(casePlan == null ? null : casePlan.getLabelPlan());
            case "doc" -> text(document == null ? null : document.getLabel());
            case "defect", "defect.manage" -> text(defect == null ? null : defect.getLabel());
            case "code" -> text(code == null ? null : code.getLabel());
            default -> null;
        };
    }

    /** MCP 模块键对应的可见别名；无别名时返回 {@code null}。 */
    @JsonIgnore
    public String aliasForMcp(String mcpKey) {
        if (mcpKey == null) {
            return null;
        }
        return switch (mcpKey) {
            case "case" -> alias("case");
            case "plan" -> alias("plan");
            case "deliverable" -> alias("module");
            case "document" -> alias("doc");
            case "defect" -> alias("defect");
            case "code" -> alias("code");
            default -> null;
        };
    }

    private static String text(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ModuleSwitch {
        private Boolean enabled;
        private String label;
        private String labelCase;
        private String labelPlan;
        private Integer maxUploadMb;

        @JsonIgnore
        public boolean isOn() {
            return enabled == null || enabled;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getLabelCase() {
            return labelCase;
        }

        public void setLabelCase(String labelCase) {
            this.labelCase = labelCase;
        }

        public String getLabelPlan() {
            return labelPlan;
        }

        public void setLabelPlan(String labelPlan) {
            this.labelPlan = labelPlan;
        }

        public Integer getMaxUploadMb() {
            return maxUploadMb;
        }

        public void setMaxUploadMb(Integer maxUploadMb) {
            this.maxUploadMb = maxUploadMb;
        }
    }
}
