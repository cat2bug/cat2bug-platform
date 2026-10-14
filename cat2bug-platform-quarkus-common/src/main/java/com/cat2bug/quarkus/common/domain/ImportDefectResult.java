package com.cat2bug.quarkus.common.domain;

import java.util.Collections;
import java.util.List;

public final class ImportDefectResult {

    private final String errorMessage;
    private final List<Long> insertedDefectIds;

    private ImportDefectResult(String errorMessage, List<Long> insertedDefectIds) {
        this.errorMessage = errorMessage != null ? errorMessage : "";
        this.insertedDefectIds = insertedDefectIds != null ? List.copyOf(insertedDefectIds) : List.of();
    }

    public static ImportDefectResult success(List<Long> insertedDefectIds) {
        return new ImportDefectResult("", insertedDefectIds);
    }

    public static ImportDefectResult failure(String errorMessage) {
        return new ImportDefectResult(errorMessage, Collections.emptyList());
    }

    public boolean isSuccess() {
        return errorMessage.isEmpty();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<Long> getInsertedDefectIds() {
        return insertedDefectIds;
    }
}
