package com.cat2bug.quarkus.admin.domain.report;

import java.util.ArrayList;
import java.util.List;

/**
 * Grouped payload for {@code GET /system/report/template/picker}.
 */
public class ReportTemplatePickerResponse {

    private List<ReportTemplatePickerGroup> groups = new ArrayList<>();

    public List<ReportTemplatePickerGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<ReportTemplatePickerGroup> groups) {
        this.groups = groups;
    }
}
