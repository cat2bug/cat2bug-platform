package com.cat2bug.admin.service;

import java.util.ArrayList;
import java.util.List;

/** 一次出站勾选解析后的分包计划。 */
public class CloudPushPlan {

    private boolean legacyFull;
    private boolean overLimit;
    private long maxTotalBytes;
    private long estimatedBytes;
    private long requiredBytes;
    private List<String> gitSlugs = new ArrayList<>();
    private List<Long> documentIds = new ArrayList<>();
    private List<Part> parts = new ArrayList<>();

    public boolean isLegacyFull() {
        return legacyFull;
    }

    public void setLegacyFull(boolean legacyFull) {
        this.legacyFull = legacyFull;
    }

    public boolean isOverLimit() {
        return overLimit;
    }

    public void setOverLimit(boolean overLimit) {
        this.overLimit = overLimit;
    }

    public long getMaxTotalBytes() {
        return maxTotalBytes;
    }

    public void setMaxTotalBytes(long maxTotalBytes) {
        this.maxTotalBytes = maxTotalBytes;
    }

    public long getEstimatedBytes() {
        return estimatedBytes;
    }

    public void setEstimatedBytes(long estimatedBytes) {
        this.estimatedBytes = estimatedBytes;
    }

    public long getRequiredBytes() {
        return requiredBytes;
    }

    public void setRequiredBytes(long requiredBytes) {
        this.requiredBytes = requiredBytes;
    }

    public List<String> getGitSlugs() {
        return gitSlugs;
    }

    public void setGitSlugs(List<String> gitSlugs) {
        this.gitSlugs = gitSlugs;
    }

    public List<Long> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(List<Long> documentIds) {
        this.documentIds = documentIds;
    }

    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    public static class Part {
        private String partId;
        private String type;
        private String refKey;
        private int sortOrder;
        private long estimatedBytes;

        public String getPartId() {
            return partId;
        }

        public void setPartId(String partId) {
            this.partId = partId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getRefKey() {
            return refKey;
        }

        public void setRefKey(String refKey) {
            this.refKey = refKey;
        }

        public int getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
        }

        public long getEstimatedBytes() {
            return estimatedBytes;
        }

        public void setEstimatedBytes(long estimatedBytes) {
            this.estimatedBytes = estimatedBytes;
        }
    }
}
