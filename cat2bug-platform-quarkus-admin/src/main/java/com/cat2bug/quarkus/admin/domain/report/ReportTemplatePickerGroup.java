package com.cat2bug.quarkus.admin.domain.report;

import java.util.ArrayList;
import java.util.List;

public class ReportTemplatePickerGroup {

    private String code;
    private String title;
    private List<ReportTemplatePickerItem> items = new ArrayList<>();
    /** Custom group pagination total (optional). */
    private Long total;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ReportTemplatePickerItem> getItems() {
        return items;
    }

    public void setItems(List<ReportTemplatePickerItem> items) {
        this.items = items;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
